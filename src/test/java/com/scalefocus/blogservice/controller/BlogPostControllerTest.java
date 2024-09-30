package com.scalefocus.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalefocus.blogservice.config.SecurityConfig;
import com.scalefocus.blogservice.dto.*;
import com.scalefocus.blogservice.exceptions.BlogPostNotFoundException;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import com.scalefocus.blogservice.service.implementation.BlogPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
//@WebMvcTest(BlogPostController.class)
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class BlogPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogPostServiceImpl blogPostService;

    @MockBean
    private AuthClient authClient;


    @Autowired
    private ObjectMapper objectMapper;

    private BlogPostDTO blogPostDTO;
    private BlogPostDTO blogPostDTO2;


    @BeforeEach
    public void setUp() {
        ResponseTagDTO responseTagDTO1 = new ResponseTagDTO(1L, "Tag1", LocalDateTime.now());

        blogPostDTO = new BlogPostDTO(1L, "Test Title 1", "Test Content 1", List.of(responseTagDTO1));
        blogPostDTO2 = new BlogPostDTO(2L, "Test Title 2", "Test Content 2", List.of(responseTagDTO1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testLogin_Successful() throws Exception {
        LoginUserDto loginUserDto = new LoginUserDto("testuser", "password123");
        LoginResponse loginResponse = new LoginResponse("mocked-jwt-token", 3600, "message");

        when(authClient.authenticate("testuser", "password123")).thenReturn(loginResponse);

        mockMvc.perform(post("/api/blogposts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));

        verify(authClient, times(1)).authenticate("testuser", "password123");
    }

    @Test
    void testLogin_Failed() throws Exception {
        LoginUserDto loginUserDto = new LoginUserDto("invalidUser", "wrongpassword");

        when(authClient.authenticate("invalidUser", "wrongpassword")).thenThrow(new RuntimeException("Invalid username or password"));

        mockMvc.perform(post("/api/blogposts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUserDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }


    @Test
    void testGetBlogPostById() throws Exception {
        when(blogPostService.getBlogPostById(1L)).thenReturn(blogPostDTO);

        mockMvc.perform(get("/api/blogposts/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title 1"))
                .andExpect(jsonPath("$.text").value("Test Content 1"));
    }

    @Test
    void testGetBlogPostById_NotFound() throws Exception {
        when(blogPostService.getBlogPostById(1L)).thenThrow(new BlogPostNotFoundException(1L));

        mockMvc.perform(get("/api/blogposts/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Blogpost with id 1 not found"));
    }


    @Test
    void testGetFilteredBlogPosts_WithTagOnly() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO));

        when(blogPostService.getFilteredBlogPosts("New Tag", null, null, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("tag", "New Tag")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()));
    }

    @Test
    void testGetFilteredBlogPosts_WithParityOnly() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO, blogPostDTO2));

        when(blogPostService.getFilteredBlogPosts(null, "even", null, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("parity", "even")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()))
                .andExpect(jsonPath("$.content[1].title").value(blogPostDTO2.getTitle()));
    }

    @Test
    void testGetFilteredBlogPosts_WithSummaryLimitOnly() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO, blogPostDTO2));

        when(blogPostService.getFilteredBlogPosts(null, null, 30, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("summaryLimit", "30")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()))
                .andExpect(jsonPath("$.content[1].title").value(blogPostDTO2.getTitle()));
    }

    @Test
    void testGetFilteredBlogPosts_WithParityAndSummaryLimit() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO));

        when(blogPostService.getFilteredBlogPosts(null, "even", 30, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("parity", "even")
                        .param("summaryLimit", "30")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()));
    }

    @Test
    void testGetFilteredBlogPosts_WithTagAndSummaryLimit() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO));

        when(blogPostService.getFilteredBlogPosts("New Tag", null, 30, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("tag", "New Tag")
                        .param("summaryLimit", "30")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()));
    }

    @Test
    void testGetFilteredBlogPosts_NoFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostDTO> blogPostPage = new PageImpl<>(List.of(blogPostDTO, blogPostDTO2));

        when(blogPostService.getFilteredBlogPosts(null, null, null, pageable))
                .thenReturn(blogPostPage);

        mockMvc.perform(get("/api/blogposts")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value(blogPostDTO.getTitle()))
                .andExpect(jsonPath("$.content[1].title").value(blogPostDTO2.getTitle()));
    }

    @Test
    void testAddBlogPostValidationFailure() throws Exception {
        BlogPostDTO invalidBlogPostDTO = new BlogPostDTO(6L, "", "Yes", Collections.emptyList());
        String token = "Bearer mocked-jwt-token";

        when(authClient.validateToken("mocked-jwt-token")).thenReturn(true);

        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        when(authClient.getUserDetails("mocked-jwt-token")).thenReturn(mockUserDetails);

        mockMvc.perform(post("/api/blogposts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBlogPostDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testAddBlogPost_Successful() throws Exception {
        BlogPostDTO newBlogPostDTO = new BlogPostDTO(4L, "New Title", "New Content", Collections.emptyList());
        newBlogPostDTO.setAuthor("testuser");

        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        when(blogPostService.addBlogPost(any(CreateBlogPostDTO.class), eq("testuser"))).thenReturn(newBlogPostDTO);


        mockMvc.perform(post("/api/blogposts")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBlogPostDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(newBlogPostDTO.getTitle()))
                .andExpect(jsonPath("$.author").value("testuser"));
    }

    @Test
    @WithMockUser
    void testAddBlogPost_Unauthorized() throws Exception {
        BlogPostDTO newBlogPostDTO = new BlogPostDTO(4L, "New Title", "New Content", Collections.emptyList());
        String token = "Bearer mocked-jwt-token";

        when(authClient.validateToken("mocked-jwt-token")).thenReturn(false);

        mockMvc.perform(post("/api/blogposts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBlogPostDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateBlogPost() throws Exception {
        BlogPostDTO existingBlogPostDTO = new BlogPostDTO(1L, "Old Title", "Old Content", Collections.emptyList());
        existingBlogPostDTO.setAuthor("testuser");

        BlogPostDTO updatedBlogPostDTO = new BlogPostDTO(1L, "Updated Title", "Updated Content", Collections.emptyList());
        updatedBlogPostDTO.setAuthor("testuser");

        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        Mockito.when(blogPostService.getBlogPostById(1L)).thenReturn(existingBlogPostDTO);

        Mockito.when(authClient.canEditOrDeleteBlogPost(token, "testuser")).thenReturn(true);

        Mockito.when(blogPostService.updateBlogPost(eq(1L), any(BlogPostDTO.class))).thenReturn(updatedBlogPostDTO);

        mockMvc.perform(patch("/api/blogposts/1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBlogPostDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedBlogPostDTO.getTitle()))
                .andExpect(jsonPath("$.text").value(updatedBlogPostDTO.getText()));

        Mockito.verify(blogPostService).updateBlogPost(eq(1L), any(BlogPostDTO.class));
    }

    @Test
    @WithMockUser
    void testAddTagToBlogPost() throws Exception {
        BlogPostDTO updatedBlogPostDTO = new BlogPostDTO(1L, "Test Title 1", "Test Content 1", Collections.singletonList(new ResponseTagDTO(1L, "New Tag", LocalDateTime.now())));
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        when(blogPostService.addTagToBlogPost(1L, "New Tag")).thenReturn(updatedBlogPostDTO);

        mockMvc.perform(post("/api/blogposts/1/tags")
                        .param("tag", "New Tag")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0].tagName").value("New Tag"))
                .andExpect(jsonPath("$.title").value(updatedBlogPostDTO.getTitle()))
                .andExpect(jsonPath("$.text").value(updatedBlogPostDTO.getText()));
    }

    @Test
    @WithMockUser
    void testRemoveTagFromBlogPost() throws Exception {
        BlogPostDTO updatedBlogPostDTO = new BlogPostDTO(1L, "Test title", "Test content", Collections.emptyList());
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        when(blogPostService.removeTagFromBlogPost(1L, "Tag1")).thenReturn(updatedBlogPostDTO);

        mockMvc.perform(delete("/api/blogposts/1/tags")
                        .param("tag", "Tag1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.text").value("Test content"))
                .andExpect(jsonPath("$.tags").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteBlogPost() throws Exception {
        BlogPostDTO existingBlogPostDTO = new BlogPostDTO(1L, "Test Title", "Test Content", Collections.emptyList());
        existingBlogPostDTO.setAuthor("testuser");

        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        Mockito.when(blogPostService.getBlogPostById(1L)).thenReturn(existingBlogPostDTO);

        Mockito.when(authClient.canEditOrDeleteBlogPost(token, "testuser")).thenReturn(true);

        doNothing().when(blogPostService).deleteBlogPost(1L);

        mockMvc.perform(delete("/api/blogposts/1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Mockito.verify(blogPostService).deleteBlogPost(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteBlogPostError() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        Mockito.when(blogPostService.getBlogPostById(1L)).thenThrow(new BlogPostNotFoundException(1L));

        mockMvc.perform(delete("/api/blogposts/1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Blogpost with id 1 not found"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteBlogPost_Unauthorized() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        BlogPostDTO existingBlogPostDTO = new BlogPostDTO(1L, "Test Title", "Test Content", Collections.emptyList());
        existingBlogPostDTO.setAuthor("anotherUser");

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));
        Mockito.when(blogPostService.getBlogPostById(1L)).thenReturn(existingBlogPostDTO);
        Mockito.when(authClient.canEditOrDeleteBlogPost(token, "anotherUser")).thenReturn(false);

        mockMvc.perform(delete("/api/blogposts/1")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("User does not have permission to delete this blog post."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetAllUsers_Unauthorized() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("testuser", List.of("ROLE_USER")));

        mockMvc.perform(get("/api/blogposts/admin/users")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()) // Expect 403 Forbidden status
                .andExpect(jsonPath("$.errorMessage").value("Access Denied"));
    }

    @Test
    @WithMockUser(username ="admin", roles = {"ADMIN"})
    void testGetAllUsers_Authorized() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        List<UserDetailsDto> users = List.of(
                new UserDetailsDto("user1", List.of("ROLE_USER")),
                new UserDetailsDto("user2", List.of("ROLE_USER"))
        );

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        Mockito.when(authClient.getUserDetails(token)).thenReturn(new UserDetailsDto("admin", List.of("ROLE_ADMIN")));
        Mockito.when(authClient.getAllUsers(token)).thenReturn(users);

        mockMvc.perform(get("/api/blogposts/admin/users")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));

        Mockito.verify(authClient).getAllUsers(token);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_Authorized() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;
        String usernameToDelete = "user1";

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        UserDetailsDto adminDetails = new UserDetailsDto("admin", List.of("ROLE_ADMIN"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(adminDetails);

        doNothing().when(authClient).deleteUser(usernameToDelete, token);

        mockMvc.perform(delete("/api/blogposts/admin/users")
                        .header("Authorization", authHeader)
                        .param("username", usernameToDelete)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Mockito.verify(authClient, times(1)).deleteUser(usernameToDelete, token);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteUser_Unauthorized() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;
        String usernameToDelete = "user1";

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken(token)).thenReturn(true);

        UserDetailsDto userDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(userDetails);

        Mockito.doThrow(new AccessDeniedException("Access Denied")).when(authClient).deleteUser(usernameToDelete, token);

        mockMvc.perform(delete("/api/blogposts/admin/users")
                        .header("Authorization", authHeader)
                        .param("username", usernameToDelete)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Access Denied"));

        Mockito.verify(authClient, never()).deleteUser(usernameToDelete, token);
    }
}
