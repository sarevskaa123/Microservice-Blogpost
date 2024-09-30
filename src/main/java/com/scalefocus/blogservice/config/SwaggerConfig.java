package com.scalefocus.blogservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Blog Post API")
                        .version("1.0")
                        .description("Comprehensive API documentation for managing blog posts, " +
                                "including creation, updates, tagging, and retrieval"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("blogpost")
                .pathsToMatch("/api/**")
                .build();
    }

}
