package org.example.ftp.admin.dto;

public class UserPermissionsDto {

    private final String username;
    private final boolean read;
    private final boolean write;
    private final boolean execute;

    public UserPermissionsDto(String username, boolean read, boolean write, boolean execute) {
        this.username = username;
        this.read = read;
        this.write = write;
        this.execute = execute;
    }

    public String getUsername() {
        return username;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isWrite() {
        return write;
    }

    public boolean isExecute() {
        return execute;
    }
}