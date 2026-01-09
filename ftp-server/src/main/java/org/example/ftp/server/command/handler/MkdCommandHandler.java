package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.AccessControl;
import org.example.ftp.server.fs.PathResolver;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Files;
import java.nio.file.Path;

public class MkdCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "MKD";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {

        if (argument == null || argument.isBlank()) {
            return FtpResponse.error(501, "Directory name required.");
        }

        Path dir;
        try {
            dir = PathResolver.resolve(session, argument);
        } catch (SecurityException e) {
            return FtpResponse.error(550, "Access denied.");
        }

        if (!AccessControl.can(session, dir, Permission.WRITE)) {
            return FtpResponse.error(550, "Permission denied.");
        }

        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            return FtpResponse.error(550, "Failed to create directory.");
        }

        return FtpResponse.ok(257, "\"" + argument + "\" directory created.");
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}