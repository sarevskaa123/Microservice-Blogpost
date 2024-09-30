package com.scalefocus.blogservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogPostDTO {
    private Long blogId;

    @NotBlank(message = "Title is mandatory")
    @NotNull(message = "Title is mandatory")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Text content is mandatory")
    @Size(min = 5, message = "Text must be at least 5 characters long")
    private String text;
    private List<ResponseTagDTO> tags;

    private String author;

    public BlogPostDTO(Long blogId, String title, String text, String author, List<ResponseTagDTO> responseTagDTOS) {
        this.blogId = blogId;
        this.title = title;
        this.author = author;
        this.text = text;
        this.tags = responseTagDTOS;
    }

    public BlogPostDTO(Long blogId, String title, String text, List<ResponseTagDTO> responseTagDTOS) {
        this.blogId = blogId;
        this.title = title;
        this.text = text;
        this.tags = responseTagDTOS;
    }
}
