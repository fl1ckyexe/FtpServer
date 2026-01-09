package org.example.ftp.server.http;


public class AdminTokenService {

    private volatile String token; 

    public AdminTokenService(String initialToken) {
        this.token = normalize(initialToken);
    }

    public String getToken() {
        return token;
    }

    public boolean isTokenSet() {
        String t = token;
        return t != null && !t.isBlank();
    }

    public void setToken(String newToken) {
        this.token = normalize(newToken);
    }

    private static String normalize(String t) {
        if (t == null) return null;
        String s = t.trim();
        return s.isEmpty() ? null : s;
    }
}

