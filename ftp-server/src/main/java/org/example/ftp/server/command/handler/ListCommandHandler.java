package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.ListFormatter;
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
import java.util.stream.Stream;

public class ListCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "LIST";
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

        try (
                Socket dataConnection = session.getPassiveDataSocket().accept();
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(dataConnection.getOutputStream(), StandardCharsets.UTF_8),
                        true
                )
        ) {
            Path home = session.getHomeDirectory().normalize().toAbsolutePath();
            Path shared = session.getSharedDirectory().normalize().toAbsolutePath();
            Path requested = dir.normalize().toAbsolutePath();

            
            if (requested.equals(home)) {
                
                
                Path ftpRoot = session.getFtpRoot().normalize().toAbsolutePath();
                Path usersDir = ftpRoot.resolve("users").normalize().toAbsolutePath();
                String currentUsername = session.getUsername();
                
                try (Stream<Path> stream = Files.list(dir)) {
                    stream
                        .filter(p -> {
                            Path normalized = p.normalize().toAbsolutePath();
                            
                            
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
                        })
                        .map(ListFormatter::format)
                        .forEach(out::println);
                }
                out.flush();
            }
            
            
            else if (requested.equals(shared)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.map(ListFormatter::format).forEach(out::println);
                }
                out.flush();
            }
            
            
            else {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.map(ListFormatter::format).forEach(out::println);
                }
                out.flush();
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

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}