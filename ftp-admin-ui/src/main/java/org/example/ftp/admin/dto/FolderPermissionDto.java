package org.example.ftp.admin.dto;

public class FolderPermissionDto {
    private String folder;
    private boolean read;
    private boolean write;
    private boolean execute;

    public FolderPermissionDto() {}

    public FolderPermissionDto(String folder, boolean read, boolean write, boolean execute) {
        this.folder = folder;
        this.read = read;
        this.write = write;
        this.execute = execute;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean isExecute() {
        return execute;
    }

    public void setExecute(boolean execute) {
        this.execute = execute;
    }
}