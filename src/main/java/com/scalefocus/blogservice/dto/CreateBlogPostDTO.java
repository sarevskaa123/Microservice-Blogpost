package com.scalefocus.blogservice.dto;

import java.util.List;

import jakarta.validation.constraints.*;

public record CreateBlogPostDTO(
        @NotBlank(message = "Title is mandatory")
        @NotNull(message = "Title is mandatory")
        @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
        String title,

        @NotBlank(message = "Text content is mandatory")
        @Size(min = 5, message = "Text must be at least 5 characters long")
        String text,

        List<ResponseTagDTO> tags
) {
}
