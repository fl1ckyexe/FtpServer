package org.example.ftp.server.auth;

import org.example.ftp.server.auth.db.SqliteUserRepository;


public class UserRateLimitService {

    private final SqliteUserRepository userRepository;

    public UserRateLimitService(SqliteUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserRateLimits getRateLimits(String username) {
        Long uploadSpeed = userRepository.getUploadSpeed(username);
        Long downloadSpeed = userRepository.getDownloadSpeed(username);
        Long legacyRateLimit = userRepository.getRateLimit(username);
        
        return new UserRateLimits(uploadSpeed, downloadSpeed, legacyRateLimit);
    }

    public record UserRateLimits(Long uploadSpeed, Long downloadSpeed, Long legacyRateLimit) {}
}

