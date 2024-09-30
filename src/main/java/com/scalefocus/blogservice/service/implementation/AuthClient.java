package com.scalefocus.blogservice.service.implementation;

import com.scalefocus.blogservice.config.ApplicationProperties;
import com.scalefocus.blogservice.dto.LoginResponse;
import com.scalefocus.blogservice.dto.LoginUserDto;
import com.scalefocus.blogservice.dto.UserDetailsDto;
import com.scalefocus.blogservice.exceptions.AuthenticationFailedException;
import com.scalefocus.blogservice.exceptions.UserDeletionFailedException;
import com.scalefocus.blogservice.exceptions.UserDetailsRetrievalException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthClient {
    private final RestTemplate restTemplate;

    private final ApplicationProperties applicationProperties;

    private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);

    public LoginResponse authenticate(String username, String password) {
        LoginUserDto loginUserDto = new LoginUserDto(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginUserDto> entity = new HttpEntity<>(loginUserDto, headers);
        String authServiceUrl = applicationProperties.getLoginPath();

        try{
            ResponseEntity<LoginResponse> response = restTemplate.exchange(authServiceUrl, HttpMethod.POST, entity, LoginResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.error("Failed to authenticate: received unexpected response status {}", response.getStatusCode());
                throw new AuthenticationFailedException("Failed to authenticate");
            }
        }catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new AuthenticationFailedException("Failed to authenticate");
            }
            throw e;
        }
    }

    public boolean validateToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String authServiceValidationUrl = applicationProperties.getValidatePath();

        try{
            ResponseEntity<String> response = restTemplate.exchange(authServiceValidationUrl, HttpMethod.GET, entity, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        }catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return false;
            }
            throw e;
        }
    }

    public String extractToken(String authHeader) {
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }

    public UserDetailsDto getUserDetails(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String userDetailsServiceUrl = applicationProperties.getUserDetailsPath();

        try {
            ResponseEntity<UserDetailsDto> response = restTemplate.exchange(userDetailsServiceUrl, HttpMethod.GET, entity, UserDetailsDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.error("Failed to retrieve user details: received unexpected response status {}", response.getStatusCode());
                throw new UserDetailsRetrievalException("Failed to retrieve user details");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UserDetailsRetrievalException("Unauthorized access");
            }
            throw e;
        }
    }

    public List<UserDetailsDto> getAllUsers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String userDetailsServiceUrl = applicationProperties.getUsersPath();

        try {
            ResponseEntity<UserDetailsDto[]> response = restTemplate.exchange(
                    userDetailsServiceUrl, HttpMethod.GET, entity, UserDetailsDto[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return List.of(response.getBody());
            } else {
                logger.error("Failed to retrieve user details: received unexpected response status {}", response.getStatusCode());
                throw new UserDetailsRetrievalException("Failed to retrieve user details");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UserDetailsRetrievalException("Unauthorized access");
            }
            throw e;
        }
    }

    public void deleteUser(String username, String token) {
        logger.info("Token received for deletion: {}", token);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String deleteUserUrl = applicationProperties.getDeleteUserPath(username);

        try{
            ResponseEntity<Void> response = restTemplate.exchange(deleteUserUrl, HttpMethod.DELETE, entity, Void.class);

            if(response.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("Successfully deleted user {}", username);
            }else {
                logger.error("Failed to delete user: received unexpected response status {}", response.getStatusCode());
                throw new UserDeletionFailedException("Failed to delete user");
            }
        }catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error deleting user: {}: HTTP Status - {}, Response Body - {}", username, e.getStatusCode(), e.getResponseBodyAsString());
            throw new UserDeletionFailedException("Failed to delete user");
        } catch (RestClientException e){
            logger.error("General error deleting user: {}", username, e);
            throw new UserDeletionFailedException("Failed to delete user");
        }
    }

    public boolean canEditOrDeleteBlogPost(String token, String blogPostAuthor) {
        UserDetailsDto userDetails = getUserDetails(token);
        String username = userDetails.getUsername();
        List<String> roles = userDetails.getRoles();

        return blogPostAuthor.equals(username) || roles.contains("ROLE_ADMIN");
    }
}