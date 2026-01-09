package org.example.ftp.server.auth;

public interface PasswordHasher {

    String hash(String plainPassword);

    boolean verify(String plainPassword, String passwordHash);
}
