package com.scalefocus.blogservice.api;

import com.scalefocus.blogservice.dto.BlogPostDTO;
import com.scalefocus.blogservice.dto.CreateBlogPostDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BlogPosts", description = "Operations related to blog posts")
@Validated
public interface BlogPostApi {

    @Operation(summary = "Get all blog posts with optional filters")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of blog posts")
    @GetMapping
    Page<BlogPostDTO> getBlogPosts(@RequestParam(required = false) @Min(value = 5, message = "summaryLimit must be at least 5") Integer summaryLimit,
                                   @RequestParam(required = false) String tag,
                                   @RequestParam(required = false) String parity,
                                   Pageable pageable);

    @Operation(summary = "Get a blog post by ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the blog post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogPostDTO.class)))
    @ApiResponse(responseCode = "404", description = "Blog post not found")
    @GetMapping("/{id}")
    ResponseEntity<BlogPostDTO> getBlogPostById(@PathVariable Long id);

    @Operation(summary = "Create a new blog post")
    @ApiResponse(responseCode = "200", description = "Successfully created the blog post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogPostDTO.class)))
    @PostMapping
    ResponseEntity<BlogPostDTO> addBlogPost(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody CreateBlogPostDTO blogPostDTO);

    @Operation(summary = "Update an existing blog post")
    @ApiResponse(responseCode = "200", description = "Successfully updated the blog post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogPostDTO.class)))
    @PatchMapping("/{id}")
    ResponseEntity<BlogPostDTO> updateBlogPost(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody @Valid BlogPostDTO updatedBlogPostDTO);

    @Operation(summary = "Add a tag to a blog post")
    @ApiResponse(responseCode = "200", description = "Successfully added the tag to the blog post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogPostDTO.class)))
    @PostMapping("/{id}/tags")
    ResponseEntity<BlogPostDTO> addBlogPostTag(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam String tag);

    @Operation(summary = "Remove a tag from a blog post")
    @ApiResponse(responseCode = "200", description = "Successfully removed the tag from the blog post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BlogPostDTO.class)))
    @DeleteMapping("/{id}/tags")
    ResponseEntity<BlogPostDTO> deleteBlogPostTag(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestParam String tag);

    @Operation(summary = "Delete a blog post")
    @ApiResponse(responseCode = "204", description = "Successfully deleted the blog post")
    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteBlogPost(@RequestHeader("Authorization") String authHeader, @PathVariable Long id);
}
