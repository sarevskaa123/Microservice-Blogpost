package com.scalefocus.blogservice.exceptions;

public class UserDeletionFailedException extends RuntimeException{
    public UserDeletionFailedException(String message){
        super(message);
    }
}
