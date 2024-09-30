package com.scalefocus.blogservice.exceptions;

public class TagAlreadyExistsException extends RuntimeException {

    public TagAlreadyExistsException(String tagName){
        super(String.format("Tag with name %s already exists", tagName));
    }

}
