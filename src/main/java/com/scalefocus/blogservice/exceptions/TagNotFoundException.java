package com.scalefocus.blogservice.exceptions;

public class TagNotFoundException extends RuntimeException{
    public TagNotFoundException(String tagName){
        super("Tag with name " + tagName + " not found");
    }
}
