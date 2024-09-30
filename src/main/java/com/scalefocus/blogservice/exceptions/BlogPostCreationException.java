package com.scalefocus.blogservice.exceptions;

public class BlogPostCreationException extends RuntimeException {
    public BlogPostCreationException(String message) {
        super(message);
    }
}
