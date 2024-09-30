package com.scalefocus.blogservice.controller;

import com.scalefocus.blogservice.api.TagApi;
import com.scalefocus.blogservice.dto.ResponseTagDTO;
import com.scalefocus.blogservice.service.implementation.AuthClient;
import com.scalefocus.blogservice.service.implementation.TagServiceImpl;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/tags")
public class TagController implements TagApi {
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);
    private final TagServiceImpl tagService;
    private final AuthClient authClient;

    @GetMapping
    public List<ResponseTagDTO> getAllTags() {
        logger.info("Fetching all tags");
        return tagService.getAllTags();
    }

    @PostMapping("/create-tag")
    public ResponseEntity<ResponseTagDTO> createTag(@RequestHeader("Authorization") String authHeader,
                                                    @RequestParam
                                                    @NotBlank(message = "Tag name is mandatory")
                                                    @NotNull(message = "Tag name is mandatory")
                                                    @Size(max = 50, message = "Tag name must be at most 50 characters long") String tagName) {

        logger.info("Creating new tag: {}", tagName);
        ResponseTagDTO responseTagDTO = tagService.createTag(tagName);
        logger.info("Tag created with ID: {}", responseTagDTO.tagId());
        return ResponseEntity.ok().body(responseTagDTO);

    }

    @DeleteMapping()
    public ResponseEntity<?> deleteTag(@RequestHeader("Authorization") String authHeader,
                                       @RequestParam @NotNull @NotBlank String tagName) {

        logger.info("Deleting tag: {}", tagName);
        tagService.deleteTag(tagName);
        logger.info("Tag '{}' deleted successfully", tagName);
        return ResponseEntity.noContent().build();

    }

}
