package com.scalefocus.blogservice.dto;

import lombok.Data;

@Data
public class LoginUserDto extends UserCredentialsDto{
    public LoginUserDto(String username, String password) {
        super(username, password);
    }
}
