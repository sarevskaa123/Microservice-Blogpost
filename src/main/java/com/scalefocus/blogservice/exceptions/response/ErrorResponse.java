package com.scalefocus.blogservice.exceptions.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String errorMessage;
    private LocalDateTime timestamp;
}
