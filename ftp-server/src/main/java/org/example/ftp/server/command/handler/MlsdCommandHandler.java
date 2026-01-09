package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.AccessControl;
import org.example.ftp.server.fs.PathResolver;
import org.example.ftp.server.session.FtpSession;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class MlsdCommandHandler extends AbstractCommandHandler {

    private static final DateTimeFormatter MLSD_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    @Override
    public String getCommandName() {
        return "MLSD";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        if (session.getPassiveDataSocket() == null) {
            return Responses.usePasvFirst();
        }

        Path dir;
        try {
            dir = PathResolver.resolve(session, argument);
        } catch (SecurityException e) {
            return Responses.accessDenied();
        }

        
        if (!AccessControl.can(session, dir, Permission.READ)) {
            return Responses.permissionDenied();
        }

        Path home = session.getHomeDirectory().normalize().toAbsolutePath();
        Path requested = dir.normalize().toAbsolutePath();
        boolean listingHomeRoot = requested.equals(home);

        try (
                Socket dataConnection = session.getPassiveDataSocket().accept();
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(dataConnection.getOutputStream(), StandardCharsets.UTF_8),
                        true
                )
        ) {
            
            
            Path ftpRoot = session.getFtpRoot().normalize().toAbsolutePath();
            Path usersDir = ftpRoot.resolve("users").normalize().toAbsolutePath();
            String currentUsername = session.getUsername();
            boolean isHomeDirectory = listingHomeRoot;
            
            
            var virtualOwnerNames = new java.util.HashSet<String>();
            if (isHomeDirectory) {
                var currentUserOpt2 = session.getUserRepository().findByUsername(session.getUsername());
                if (currentUserOpt2.isPresent()) {
                    long currentUserId2 = currentUserOpt2.get().id();
                    var sharedFolders2 = session.getSharedFolderRepository().findByUserToShare(currentUserId2);
                    sharedFolders2.stream()
                        .map(sf -> {
                            var ownerOpt = session.getUserRepository().findById(sf.ownerUserId());
                            return ownerOpt.map(u -> u.username()).orElse(null);
                        })
                        .filter(username -> username != null)
                        .forEach(virtualOwnerNames::add);
                }
            }

            try (Stream<Path> stream = Files.list(dir)) {
                stream
                    .filter(p -> {
                        if (isHomeDirectory) {
                            Path normalized = p.normalize().toAbsolutePath();
                            String fileName = p.getFileName().toString();
                            
                            
                            if (virtualOwnerNames.contains(fileName)) {
                                return false;
                            }
                            
                            
                            if (normalized.startsWith(usersDir)) {
                                Path relative = usersDir.relativize(normalized);
                                if (relative.getNameCount() > 0) {
                                    String firstComponent = relative.getName(0).toString();
                                    
                                    if (!firstComponent.equals(currentUsername)) {
                                        
                                        return false;
                                    }
                                }
                            }
                            
                            
                            return normalized.startsWith(home);
                        }
                        
                        return true;
                    })
                    .forEach(p -> out.println(toMlsdLine(p, p.getFileName().toString())));
            }

        } catch (SocketTimeoutException e) {
            return Responses.connectionClosedTransferAborted();
        } catch (Exception e) {
            return Responses.connectionClosedTransferAborted();
        } finally {
            try {
                session.closePassiveDataSocket();
            } catch (Exception ignored) {}
        }

        return Responses.directorySendOk();
    }

    private String toMlsdLine(Path path, String name) {
        try {
            boolean isDir = Files.isDirectory(path);
            long size = isDir ? 0L : Files.size(path);

            FileTime ft = Files.getLastModifiedTime(path);
            String modify = MLSD_TIME.format(ft.toInstant());

            return "type=" + (isDir ? "dir" : "file")
                    + ";modify=" + modify
                    + ";size=" + size
                    + "; " + name;
        } catch (Exception e) {
            return "type=unknown; " + name;
        }
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}