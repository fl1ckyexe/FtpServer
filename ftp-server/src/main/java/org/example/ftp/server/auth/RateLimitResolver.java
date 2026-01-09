package org.example.ftp.server.auth;


public final class RateLimitResolver {

    private RateLimitResolver() {}

    public static RateLimitResult resolve(long globalLimit, Long userSpecific, Long legacyRateLimit) {
        if (userSpecific != null && userSpecific > 0) {
            return new RateLimitResult(userSpecific, false);
        }
        if (legacyRateLimit != null && legacyRateLimit > 0) {
            return new RateLimitResult(legacyRateLimit, false);
        }
        return new RateLimitResult(globalLimit, true);
    }

    public record RateLimitResult(long limit, boolean isGlobal) {}
}

