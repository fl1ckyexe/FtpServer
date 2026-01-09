package org.example.ftp.server.fs;

import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Path;


public final class AccessControl {

    private AccessControl() {}

    public static boolean can(FtpSession session, Path resolvedPath, Permission required) {
        Path resolved = resolvedPath.normalize().toAbsolutePath();
        PathContext pathContext = createPathContext(session, resolved);
        
        if (isHomeDirectory(pathContext, resolved)) {
            return true;
        }
        
        if (isSharedOrRootDirectory(pathContext, resolved)) {
            return checkGlobalPermissions(session, required);
        }

        if (!isInUsersDirectory(pathContext, resolved)) {
            return checkGlobalPermissions(session, required);
        }

        return checkSharedFolderAccess(session, pathContext, resolved, required);
    }

    private static PathContext createPathContext(FtpSession session, Path resolved) {
        return new PathContext(session.getFtpRoot(), session.getHomeDirectory());
    }

    private static boolean isHomeDirectory(PathContext context, Path resolved) {
        return context.homeDirectory() != null && resolved.startsWith(context.homeDirectory());
    }

    private static boolean isSharedOrRootDirectory(PathContext context, Path resolved) {
        return resolved.startsWith(context.sharedDirectory());
    }

    private static boolean isInUsersDirectory(PathContext context, Path resolved) {
        return resolved.startsWith(context.usersDirectory());
    }

    private static boolean checkGlobalPermissions(FtpSession session, Permission required) {
        return session.getPermissionService().has(session.getUsername(), required);
    }

    private static boolean checkSharedFolderAccess(FtpSession session, PathContext context, Path resolved, Permission required) {
        String username = session.getUsername();
        String relativePath = context.usersDirectory().relativize(resolved).toString().replace('\\', '/');
        String ftpStyle = "/" + relativePath;

        var currentUserOpt = session.getUserRepository().findByUsername(username);
        if (currentUserOpt.isEmpty()) {
            return false;
        }
        long currentUserId = currentUserOpt.get().id();

        boolean hasSharePermission = session.getSharedFolderRepository().hasPermission(currentUserId, ftpStyle, required);
        
        if (!hasSharePermission && required == Permission.READ) {
            return checkHomeDirectoryAccess(session, relativePath, username, currentUserId);
        }
        
        return hasSharePermission;
    }

    private static boolean checkHomeDirectoryAccess(FtpSession session, String relativePath, String username, long currentUserId) {
        String[] parts = relativePath.split("/", 2);
        if (parts.length == 0 || parts[0].equals(username)) {
            return false;
        }
        String ownerUsername = parts[0];
        
        return session.getSharedFolderRepository().findByUserToShare(currentUserId).stream()
            .anyMatch(sf -> {
                var ownerOpt = session.getUserRepository().findById(sf.ownerUserId());
                return ownerOpt.isPresent() && ownerOpt.get().username().equals(ownerUsername);
            });
    }
}

