package com.scalefocus.blogservice.api;

import com.scalefocus.blogservice.dto.ResponseTagDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tags", description = "Operations related to tags")
@Validated
public interface TagApi {

    @Operation(summary = "Get all tags")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of tags")
    @GetMapping
    List<ResponseTagDTO> getAllTags();

    @Operation(summary = "Create a new tag")
    @ApiResponse(responseCode = "200", description = "Successfully created the tag",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseTagDTO.class)))
    @PostMapping("/create-tag")
    ResponseEntity<ResponseTagDTO> createTag(@RequestHeader("Authorization") String authHeader,@RequestParam
                                             @NotBlank(message = "Tag name is mandatory")
                                             @NotNull(message = "Tag name is mandatory")
                                             @Size(max = 50, message = "Tag name must be at most 50 characters long") String tagName);

    @Operation(summary = "Delete a tag")
    @ApiResponse(responseCode = "204", description = "Successfully deleted the tag")
    @DeleteMapping()
    ResponseEntity<?> deleteTag(@RequestHeader("Authorization") String authHeader, @RequestParam @NotNull @NotBlank String tagName);
}



