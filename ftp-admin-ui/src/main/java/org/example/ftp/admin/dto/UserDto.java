package org.example.ftp.admin.dto;

public class UserDto {

    private String username;

    
    private boolean enabled;

    
    private boolean admin;
    private int maxConnections;
    private long uploadLimit;
    private long downloadLimit;

    public UserDto() {
    }

    public UserDto(String username, boolean enabled) {
        this.username = username;
        this.enabled = enabled;
    }

    public UserDto(
            String username,
            boolean admin,
            int maxConnections,
            long uploadLimit,
            long downloadLimit
    ) {
        this.username = username;
        this.admin = admin;
        this.maxConnections = maxConnections;
        this.uploadLimit = uploadLimit;
        this.downloadLimit = downloadLimit;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public long getUploadLimit() {
        return uploadLimit;
    }

    public void setUploadLimit(long uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    public long getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(long downloadLimit) {
        this.downloadLimit = downloadLimit;
    }
}