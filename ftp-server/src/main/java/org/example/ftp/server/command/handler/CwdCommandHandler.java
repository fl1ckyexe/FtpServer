package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.AccessControl;
import org.example.ftp.server.fs.PathResolver;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Files;
import java.nio.file.Path;

public class CwdCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "CWD";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {

        try {
            if (argument == null || argument.isBlank()
                    || argument.equals("/") || argument.equals("\\")) {

                session.setCurrentDirectory(session.getHomeDirectory());
                
                session.resetDirectoryChangeFlag();
                return FtpResponse.ok(250, "Directory successfully changed.");
            }

            Path target;
            try {
                target = PathResolver.resolve(session, argument);
            } catch (SecurityException e) {
                return FtpResponse.error(550, "Access denied.");
            }

            
            if (!AccessControl.can(session, target, Permission.READ)) {
                return FtpResponse.error(550, "Permission denied.");
            }

            if (!Files.isDirectory(target)) {
                return FtpResponse.error(550, "Not a directory.");
            }

            session.setCurrentDirectory(target);
            
            session.markDirectoryChanged();
            return FtpResponse.ok(250, "Directory successfully changed.");

        } catch (Exception e) {
            return FtpResponse.error(550, "Failed to change directory.");
        }
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}