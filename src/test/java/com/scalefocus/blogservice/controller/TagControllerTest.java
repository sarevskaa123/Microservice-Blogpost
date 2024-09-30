package com.scalefocus.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalefocus.blogservice.config.SecurityConfig;
import com.scalefocus.blogservice.dto.ResponseTagDTO;
import com.scalefocus.blogservice.dto.UserDetailsDto;
import com.scalefocus.blogservice.exceptions.TagNotFoundException;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import com.scalefocus.blogservice.service.implementation.TagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TagController.class)
@Import(SecurityConfig.class)
class TagControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TagServiceImpl tagService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthClient authClient;

    private ResponseTagDTO responseTagDTO1;
    private ResponseTagDTO responseTagDTO2;
    private ResponseTagDTO responseTagDTO3;

    @BeforeEach
    public void setUp() {
        responseTagDTO1 = new ResponseTagDTO(1L, "Tag1", LocalDateTime.now());
        responseTagDTO2 = new ResponseTagDTO(2L, "Tag2", LocalDateTime.now());
        responseTagDTO3 = new ResponseTagDTO(3L, "Tag3", LocalDateTime.now());
    }

    @Test
    void testGetAllTags() throws Exception {
        Mockito.when(tagService.getAllTags()).thenReturn(Arrays.asList(responseTagDTO1, responseTagDTO2, responseTagDTO3));

        mockMvc.perform(get("/api/tags").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tagName").value(responseTagDTO1.tagName()))
                .andExpect(jsonPath("$[1].tagName").value(responseTagDTO2.tagName()))
                .andExpect(jsonPath("$[2].tagName").value(responseTagDTO3.tagName()));

    }

    @Test
    @WithMockUser
    void testCreateTag_Successful() throws Exception {
        ResponseTagDTO responseTagDTO = new ResponseTagDTO(1L, "New", LocalDateTime.now());
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken("mocked-jwt-token")).thenReturn(true);

        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);

        Mockito.when(tagService.createTag(anyString())).thenReturn(responseTagDTO);

        mockMvc.perform(post("/api/tags/create-tag")
                        .header("Authorization", authHeader)
                        .param("tagName", "New").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagName").value("New"));
    }

    @Test
    @WithMockUser
    void testCreateTag_Unauthorized() throws Exception {
        String token = "Bearer invalid-token";
        Mockito.when(authClient.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/tags/create-tag")
                        .header("Authorization", token)
                        .param("tagName", "New")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testCreateTag_EmptyTagName() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);

        mockMvc.perform(post("/api/tags/create-tag")
                        .header("Authorization", authHeader)
                        .param("tagName", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage", containsString("Tag name is mandatory")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void testCreateTag_TooLongTagName() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);


        mockMvc.perform(post("/api/tags/create-tag")
                        .header("Authorization", authHeader)
                        .param("tagName", "This tag name exceeds the maximum defined limit for a tag name and should throw an exception.")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("createTag.tagName: Tag name must be at most 50 characters long"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void testCreateTag_BlankTagName() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.validateToken(token)).thenReturn(true);
        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);

        mockMvc.perform(post("/api/tags/create-tag")
                        .header("Authorization", authHeader)
                        .param("tagName", " ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage", containsString("Tag name is mandatory")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void testDeleteTag_Successful() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken("mocked-jwt-token")).thenReturn(true);

        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);

        doNothing().when(tagService).deleteTag("Tag1");

        mockMvc.perform(delete("/api/tags")
                        .header("Authorization", authHeader)
                        .param("tagName", "Tag1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void testDeleteTag_Unauthorized() throws Exception {
        String token = "Bearer invalid-token";
        Mockito.when(authClient.validateToken("invalid-token")).thenReturn(false);


        mockMvc.perform(delete("/api/tags")
                        .header("Authorization", token)
                        .param("tagName", "Tag1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testDeleteTagError() throws Exception {
        String token = "mocked-jwt-token";
        String authHeader = "Bearer " + token;

        Mockito.when(authClient.extractToken(authHeader)).thenReturn(token);
        Mockito.when(authClient.validateToken("mocked-jwt-token")).thenReturn(true);

        UserDetailsDto mockUserDetails = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        Mockito.when(authClient.getUserDetails(token)).thenReturn(mockUserDetails);
        doThrow(new TagNotFoundException("Tag1")).when(tagService).deleteTag("Tag1");

        mockMvc.perform(delete("/api/tags")
                        .header("Authorization", authHeader)
                        .param("tagName", "Tag1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Tag with name Tag1 not found"));
    }

}
