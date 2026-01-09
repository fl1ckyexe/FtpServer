package org.example.ftp.server.auth;

import org.example.ftp.server.auth.db.SqliteUserRepository;

import java.util.List;


public class AuthService {

    private final SqliteUserRepository users;
    private final PasswordHasher hasher;

    public AuthService(SqliteUserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    public boolean userExists(String username) {
        return users.findByUsername(username)
                .filter(User::enabled)
                .isPresent();
    }

    public boolean authenticate(String username, String password) {
        return users.findByUsername(username)
                .filter(User::enabled)
                .map(u -> hasher.verify(password, u.passwordHash()))
                .orElse(false);
    }

    public List<User> getAllUsers() {
        return users.findAll();
    }

    public void createUser(String username, String password) {
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        String hash = hasher.hash(password);
        users.create(username, hash);
    }

    public void deleteUser(String username) {
        users.delete(username);
    }

    public void updateUser(String username, boolean enabled, Long rateLimit) {
        users.update(username, enabled, rateLimit);
    }

    public void updateUser(String username, boolean enabled, Long rateLimit, Long uploadSpeed, Long downloadSpeed) {
        users.update(username, enabled, rateLimit, uploadSpeed, downloadSpeed);
    }
}
