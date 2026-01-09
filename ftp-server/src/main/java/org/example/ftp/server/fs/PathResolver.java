package org.example.ftp.server.fs;

import org.example.ftp.server.session.FtpSession;

import java.nio.file.Path;


public final class PathResolver {

    private PathResolver() {}

    public static Path resolve(FtpSession session, String ftpPath) {
        Path base = resolveBasePath(session, ftpPath);
        Path normalized = base.normalize().toAbsolutePath();
        PathContext context = new PathContext(session.getFtpRoot(), session.getHomeDirectory());
        
        validatePathWithinAllowedRoots(normalized, context);
        validateSharedFolderAccess(session, normalized, context);

        return normalized;
    }

    private static Path resolveBasePath(FtpSession session, String ftpPath) {
        if (ftpPath == null || ftpPath.isBlank()) {
            return session.getCurrentDirectory();
        }
        
        if (ftpPath.startsWith("/")) {
            return resolveAbsolutePath(session, ftpPath);
        }
        
        return resolveRelativePath(session, ftpPath);
    }

    private static Path resolveAbsolutePath(FtpSession session, String ftpPath) {
        String normalized = ftpPath.replace('\\', '/');
        
        if (normalized.equals("/shared") || normalized.startsWith("/shared/")) {
            return resolveSharedPath(session, normalized);
        }
        
        String relative = normalized.substring(1);
        String username = session.getUsername();
        
        if (relative.equals(username)) {
            return session.getHomeDirectory();
        }
        
        if (relative.startsWith(username + "/")) {
            return resolveHomeSubPath(session, relative, username, normalized);
        }
        
        return resolveOtherUserPath(session, relative, normalized);
    }

    private static Path resolveSharedPath(FtpSession session, String normalized) {
        String relative = normalized.equals("/shared") ? "" : normalized.substring("/shared/".length());
        return session.getSharedDirectory().resolve(relative);
    }

    private static Path resolveHomeSubPath(FtpSession session, String relative, String username, String normalized) {
        String subPath = relative.substring(username.length() + 1);
        
        if (subPath.equals("admin") || subPath.equals("shared") || 
            subPath.startsWith("admin/") || subPath.startsWith("shared/")) {
            throw new SecurityException("Cannot access virtual folders: " + normalized);
        }
        
        return session.getHomeDirectory().resolve(subPath);
    }

    private static Path resolveOtherUserPath(FtpSession session, String relative, String normalized) {
        int firstSlash = relative.indexOf('/');
        String ownerUsername = extractOwnerUsername(relative, firstSlash);
        
        if (ownerUsername.isEmpty() || ownerUsername.equals(session.getUsername())) {
            throw new SecurityException("Invalid path format: " + normalized);
        }
        
        long currentUserId = getCurrentUserId(session);
        
        if (!session.getSharedFolderRepository().hasAccess(currentUserId, normalized)) {
            throw new SecurityException("Access denied: " + normalized);
        }
        
        Path ownerHome = session.getFtpRoot().resolve("users").resolve(ownerUsername);
        String subPath = firstSlash > 0 ? relative.substring(firstSlash + 1) : "";
        return subPath.isEmpty() ? ownerHome : ownerHome.resolve(subPath);
    }

    private static String extractOwnerUsername(String relative, int firstSlash) {
        if (firstSlash > 0) {
            return relative.substring(0, firstSlash);
        }
        return firstSlash == -1 ? relative : "";
    }

    private static long getCurrentUserId(FtpSession session) {
        return session.getUserRepository().findByUsername(session.getUsername())
            .map(org.example.ftp.server.auth.User::id)
            .orElseThrow(() -> new SecurityException("User not found"));
    }

    private static Path resolveRelativePath(FtpSession session, String ftpPath) {
        String normalized = ftpPath.replace('\\', '/');
        Path currentDir = session.getCurrentDirectory().normalize().toAbsolutePath();
        Path homeDir = session.getHomeDirectory().normalize().toAbsolutePath();
        boolean isInHomeDirectory = currentDir.equals(homeDir);
        String username = session.getUsername();

        if ((normalized.equals("shared") || normalized.startsWith("shared/")) && isInHomeDirectory) {
            String relative = normalized.equals("shared") ? "" : normalized.substring("shared/".length());
            return session.getSharedDirectory().resolve(relative);
        }
        
        if (normalized.equals(username) && isInHomeDirectory) {
            return session.getHomeDirectory();
        }
        
        return session.getCurrentDirectory().resolve(ftpPath);
    }

    private static void validatePathWithinAllowedRoots(Path normalized, PathContext context) {
        Path home = context.homeDirectory();
        Path shared = context.sharedDirectory();
        Path usersDir = context.usersDirectory();

        if (!normalized.startsWith(home) && !normalized.startsWith(shared) && !normalized.startsWith(usersDir)) {
            throw new SecurityException("Access outside allowed roots");
        }
    }

    private static void validateSharedFolderAccess(FtpSession session, Path normalized, PathContext context) {
        Path home = context.homeDirectory();
        Path usersDir = context.usersDirectory();

        if (!normalized.startsWith(usersDir) || normalized.startsWith(home)) {
            return;
        }

        String relativePath = usersDir.relativize(normalized).toString().replace('\\', '/');
        String ftpStyle = "/" + relativePath;

        var currentUserOpt = session.getUserRepository().findByUsername(session.getUsername());
        if (currentUserOpt.isEmpty()) {
            throw new SecurityException("User not found");
        }
        long currentUserId = currentUserOpt.get().id();

        boolean hasAccess = checkSharedFolderAccess(session, currentUserId, ftpStyle, relativePath);
        
        if (!hasAccess) {
            throw new SecurityException("Access denied: " + ftpStyle);
        }
    }

    private static boolean checkSharedFolderAccess(FtpSession session, long currentUserId, String ftpStyle, String relativePath) {
        boolean hasAccess = session.getSharedFolderRepository().hasAccess(currentUserId, ftpStyle);
        
        if (!hasAccess) {
            String[] parts = relativePath.split("/", 2);
            if (parts.length > 0 && !parts[0].equals(session.getUsername())) {
                hasAccess = checkHomeDirectoryAccess(session, currentUserId, parts[0]);
            }
        }
        
        return hasAccess;
    }

    private static boolean checkHomeDirectoryAccess(FtpSession session, long currentUserId, String ownerUsername) {
        return session.getSharedFolderRepository().findByUserToShare(currentUserId).stream()
            .anyMatch(sf -> {
                var ownerOpt = session.getUserRepository().findById(sf.ownerUserId());
                return ownerOpt.isPresent() && ownerOpt.get().username().equals(ownerUsername);
            });
    }
}