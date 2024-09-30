package com.scalefocus.blogservice.controller;

import com.scalefocus.blogservice.api.BlogPostApi;
import com.scalefocus.blogservice.dto.*;
import com.scalefocus.blogservice.exceptions.InvalidTokenException;
import com.scalefocus.blogservice.exceptions.response.ErrorResponse;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import com.scalefocus.blogservice.service.implementation.BlogPostServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/blogposts")
public class BlogPostController implements BlogPostApi {

    private static final Logger logger = LoggerFactory.getLogger(BlogPostController.class);
    private final BlogPostServiceImpl blogPostService;
    private final AuthClient authClient;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto loginUserDto) {
        try {
            LoginResponse loginResponse = authClient.authenticate(loginUserDto.getUsername(), loginUserDto.getPassword());
            if (loginResponse == null || loginResponse.getToken() == null) {
                throw new InvalidTokenException("Failed to retrieve a valid token.");
            }
            logger.info("User authenticated successfully.");
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            logger.warn("Authentication failed for user: {}", loginUserDto.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid username or password"));
        }
    }

    @GetMapping
    public Page<BlogPostDTO> getBlogPosts(@RequestParam(required = false) @Min(value = 5, message = "summaryLimit must be at least 5") Integer summaryLimit,
                                          @RequestParam(required = false) String tag,
                                          @RequestParam(required = false) String parity,
                                          Pageable pageable) {

        logger.info("Fetching blog posts with filters - Tag: {}, Parity: {}, SummaryLimit: {}", tag, parity, summaryLimit);
        return blogPostService.getFilteredBlogPosts(tag, parity, summaryLimit, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostDTO> getBlogPostById(@PathVariable Long id) {
        logger.info("Fetching blog post with ID: {}", id);
        BlogPostDTO blogPostDTO = blogPostService.getBlogPostById(id);
        return ResponseEntity.ok(blogPostDTO);
    }

    @PostMapping
    public ResponseEntity<BlogPostDTO> addBlogPost(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody CreateBlogPostDTO blogPostDTO) {
        logger.info("User authenticated, adding new blog post: {}", blogPostDTO);

        String token = authHeader.substring(7);
        UserDetailsDto userDetails = authClient.getUserDetails(token);
        String username = userDetails.getUsername();

        BlogPostDTO createdBlogPostDTO = blogPostService.addBlogPost(blogPostDTO, username);
        logger.info("Blog post created with ID: {}", createdBlogPostDTO.getBlogId());
        return ResponseEntity.ok(createdBlogPostDTO);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BlogPostDTO> updateBlogPost(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody @Valid BlogPostDTO updatedBlogPostDTO) {

        String token = authClient.extractToken(authHeader);
        BlogPostDTO existingBlogPostDTO = blogPostService.getBlogPostById(id);
        String author = existingBlogPostDTO.getAuthor();

        if(!authClient.canEditOrDeleteBlogPost(token,author)) {
            logger.warn("User does not have permission to update this blog post.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        logger.info("User authenticated, updating blog post with ID: {}", id);
        BlogPostDTO result = blogPostService.updateBlogPost(id, updatedBlogPostDTO);
        logger.info("Blog post updated: {}", result);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<BlogPostDTO> addBlogPostTag(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam String tag) {
        logger.info("User authenticated, adding tag '{}' to blog post with ID: {}", tag, id);
        BlogPostDTO updatedBlogPostDTO = blogPostService.addTagToBlogPost(id, tag);
        logger.info("Tag added, updated blog post: {}", updatedBlogPostDTO);
        return ResponseEntity.ok(updatedBlogPostDTO);
    }

    @DeleteMapping("/{id}/tags")
    public ResponseEntity<BlogPostDTO> deleteBlogPostTag(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam String tag) {
        logger.info("Removing tag '{}' from blog post with ID: {}", tag, id);
        BlogPostDTO updatedBlogPostDTO = blogPostService.removeTagFromBlogPost(id, tag);
        logger.info("Tag removed, updated blog post: {}", updatedBlogPostDTO);
        return ResponseEntity.ok(updatedBlogPostDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlogPost(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        String token = authClient.extractToken(authHeader);
        BlogPostDTO existingBlogPostDTO = blogPostService.getBlogPostById(id);
        String author = existingBlogPostDTO.getAuthor();

        if(!authClient.canEditOrDeleteBlogPost(token,author)) {
            logger.warn("User does not have permission to delete this blog post.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("User does not have permission to delete this blog post.", LocalDateTime.now()));
        }

        logger.info("Deleting blog post with ID: {}", id);
        blogPostService.deleteBlogPost(id);
        logger.info("Blog post with ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserDetailsDto>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        List<UserDetailsDto> users = authClient.getAllUsers(token);
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/admin/users")
    public ResponseEntity<Void> deleteUser(@RequestHeader("Authorization") String authHeader, @RequestParam String username) {
        logger.info("Admin requesting to delete user with username: {}", username);
        String token = authHeader.substring(7);
        authClient.deleteUser(username, token);
        logger.info("Admin successfully deleted user with username: {}", username);
        return ResponseEntity.noContent().build();
    }

}
