package org.example.ftp.admin.dto;

public class PermissionDto {

    private final String username;
    private final String path;
    private final boolean read;
    private final boolean write;
    private final boolean execute;

    public PermissionDto(
            String username,
            String path,
            boolean read,
            boolean write,
            boolean execute
    ) {
        this.username = username;
        this.path = path;
        this.read = read;
        this.write = write;
        this.execute = execute;
    }

    public String getUsername() { return username; }
    public String getPath() { return path; }
    public boolean isRead() { return read; }
    public boolean isWrite() { return write; }
    public boolean isExecute() { return execute; }
}
