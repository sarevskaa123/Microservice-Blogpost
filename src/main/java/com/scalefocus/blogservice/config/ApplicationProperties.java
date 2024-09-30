package com.scalefocus.blogservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Setter
@ConfigurationProperties(prefix = "blog.authservice")
public class ApplicationProperties {
    @NotBlank
    private String baseUrl;

    @NotBlank
    private String loginPath;

    @NotBlank
    private String validatePath;

    @NotBlank
    private String userDetailsPath;

    @NotBlank
    private String usersPath;

    @NotBlank
    private String deleteUserPath;

    public String getLoginPath() {
        return baseUrl + loginPath;
    }

    public String getValidatePath() {
        return baseUrl + validatePath;
    }

    public String getUsersPath() {
        return baseUrl + usersPath;
    }

    public String getDeleteUserPath(String username) {
        return baseUrl + deleteUserPath + username;
    }

    public String getUserDetailsPath() {
        return baseUrl + userDetailsPath;
    }

}
