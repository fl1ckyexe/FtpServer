package org.example.ftp.admin.dto;

public class UserStatsDto {

    private String username;
    private int logins;
    private long bytesUploaded;
    private long bytesDownloaded;
    private String lastLogin;

    public UserStatsDto() {
    }

    public UserStatsDto(String username, int logins, long bytesUploaded, long bytesDownloaded, String lastLogin) {
        this.username = username;
        this.logins = logins;
        this.bytesUploaded = bytesUploaded;
        this.bytesDownloaded = bytesDownloaded;
        this.lastLogin = lastLogin;
    }

    public String getUsername() {
        return username;
    }

    public int getLogins() {
        return logins;
    }

    public long getBytesUploaded() {
        return bytesUploaded;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setLogins(int logins) {
        this.logins = logins;
    }

    public void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
}
