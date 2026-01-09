package org.example.ftp.admin.dto;

public class CreateUserRequestDto {

    private final String username;
    private final String password;

    public CreateUserRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}