package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.AccessControl;
import org.example.ftp.server.fs.PathResolver;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Files;
import java.nio.file.Path;

public class RmdCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "RMD";
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

        Path target;
        try {
            target = PathResolver.resolve(session, argument);
        } catch (SecurityException e) {
            return FtpResponse.error(550, "Access denied.");
        }

        if (!AccessControl.can(session, target, Permission.EXECUTE)) {
            return FtpResponse.error(550, "Permission denied.");
        }

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return FtpResponse.error(550, "Not a directory.");
        }

        try (var stream = Files.list(target)) {
            if (stream.findAny().isPresent()) {
                return FtpResponse.error(550, "Directory not empty.");
            }
        } catch (Exception e) {
            return FtpResponse.error(550, "Failed to read directory.");
        }

        try {
            Files.delete(target);
        } catch (Exception e) {
            return FtpResponse.error(550, "Remove directory failed.");
        }

        return FtpResponse.ok(250, "Directory deleted.");
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}