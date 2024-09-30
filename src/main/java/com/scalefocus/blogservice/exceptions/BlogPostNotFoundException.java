package com.scalefocus.blogservice.exceptions;

public class BlogPostNotFoundException extends RuntimeException {

    public BlogPostNotFoundException(Long id){
        super(String.format("Blogpost with id %s not found",id));
    }

}