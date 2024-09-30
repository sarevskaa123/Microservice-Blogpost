package com.scalefocus.blogservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalefocus.blogservice.config.ApplicationConfiguration;
import com.scalefocus.blogservice.dto.LoginResponse;
import com.scalefocus.blogservice.dto.UserDetailsDto;
import com.scalefocus.blogservice.exceptions.UserDeletionFailedException;
import com.scalefocus.blogservice.exceptions.UserDetailsRetrievalException;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@RestClientTest(AuthClient.class)
@Import(ApplicationConfiguration.class)
class AuthClientTest {
    @Autowired
    private AuthClient authClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockRestServiceServer mockServer;
    private final String authServiceUrl = "http://localhost:8081/auth/login";
    private final String authServiceValidationUrl = "http://localhost:8081/auth/validate";

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testAuthenticate_Successful() throws Exception {
        LoginResponse loginResponse = new LoginResponse("mocked-jwt-token", 3600, "message");
        mockServer.expect(requestTo(authServiceUrl)).andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andRespond(withSuccess(objectMapper.writeValueAsString(loginResponse), MediaType.APPLICATION_JSON));

        LoginResponse response = authClient.authenticate("testuser", "password123");

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
    }

    @Test
    void testAuthenticate_Failure() {
        mockServer.expect(requestTo("http://localhost:8081/auth/login")).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        Exception exception = assertThrows(RuntimeException.class, () -> authClient.authenticate("wronguser", "wrongpassword"));

        assertEquals("Failed to authenticate", exception.getMessage());

        mockServer.verify();
    }

    @Test
    void testValidateToken_Successful() {
        String token = "dummy-token";
        mockServer.expect(requestTo(authServiceUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"" + token + "\",\"expiresIn\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(authServiceValidationUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess());

        LoginResponse loginResponse = authClient.authenticate("testuser", "password123");
        boolean isValid = authClient.validateToken(loginResponse.getToken());

        assertTrue(isValid);

        mockServer.verify();
    }

    @Test
    void testValidateToken_Failure() {
        String token = "dummy-token";
        mockServer.expect(requestTo(authServiceUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"" + token + "\",\"expiresIn\":3600}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(authServiceValidationUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        LoginResponse loginResponse = authClient.authenticate("testuser", "password123");
        boolean isValid = authClient.validateToken(loginResponse.getToken());

        assertFalse(isValid);

        mockServer.verify();
    }

    @Test
    void testExtractToken_ValidHeader() {
        String authHeader = "Bearer validToken";
        String token = authClient.extractToken(authHeader);
        assertEquals("validToken", token);
    }

    @Test
    void testExtractToken_InvalidHeader() {
        String authHeader = "InvalidTokenHeader";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> authClient.extractToken(authHeader));
        assertEquals("Invalid Authorization header format", exception.getMessage());
    }

    @Test
    void testGetUserDetails_Success() throws Exception {
        String token = "dummy-token";
        UserDetailsDto userDetailsDto = new UserDetailsDto("testuser", List.of("ROLE_USER"));
        String userDetailsServiceUrl = "http://localhost:8081/auth/user-details";

        mockServer.expect(requestTo(userDetailsServiceUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess(objectMapper.writeValueAsString(userDetailsDto), MediaType.APPLICATION_JSON));

        UserDetailsDto result = authClient.getUserDetails(token);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertTrue(result.getRoles().contains("ROLE_USER"));
        mockServer.verify();
    }

    @Test
    void testGetUserDetails_Unauthorized() {
        String token = "dummy-token";
        String userDetailsServiceUrl = "http://localhost:8081/auth/user-details";

        mockServer.expect(requestTo(userDetailsServiceUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        UserDetailsRetrievalException exception = assertThrows(UserDetailsRetrievalException.class, () -> authClient.getUserDetails(token));
        assertEquals("Unauthorized access", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        String token = "dummy-token";
        UserDetailsDto[] users = { new UserDetailsDto("user1", List.of("ROLE_USER")), new UserDetailsDto("user2", List.of("ROLE_USER")) };
        String userDetailsServiceUrl = "http://localhost:8081/auth/users";

        mockServer.expect(requestTo(userDetailsServiceUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess(objectMapper.writeValueAsString(users), MediaType.APPLICATION_JSON));

        List<UserDetailsDto> result = authClient.getAllUsers(token);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals("user2", result.get(1).getUsername());
        mockServer.verify();
    }

    @Test
    void testGetAllUsers_Unauthorized() {
        String token = "dummy-token";
        String userDetailsServiceUrl = "http://localhost:8081/auth/users";

        mockServer.expect(requestTo(userDetailsServiceUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        UserDetailsRetrievalException exception = assertThrows(UserDetailsRetrievalException.class, () -> authClient.getAllUsers(token));
        assertEquals("Unauthorized access", exception.getMessage());
        mockServer.verify();
    }

    @Test
    void testDeleteUser_Success() {
        String token = "dummy-token";
        String username = "userToDelete";
        String deleteUserUrl = "http://localhost:8081/auth/users?username=" + username;

        mockServer.expect(requestTo(deleteUserUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> authClient.deleteUser(username, token));
        mockServer.verify();
    }

    @Test
    void testDeleteUser_Failure() {
        String token = "dummy-token";
        String username = "userToDelete";
        String deleteUserUrl = "http://localhost:8081/auth/users?username=" + username;

        mockServer.expect(requestTo(deleteUserUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        UserDeletionFailedException exception = assertThrows(UserDeletionFailedException.class, () -> authClient.deleteUser(username, token));
        assertEquals("Failed to delete user", exception.getMessage());
        mockServer.verify();
    }
}
