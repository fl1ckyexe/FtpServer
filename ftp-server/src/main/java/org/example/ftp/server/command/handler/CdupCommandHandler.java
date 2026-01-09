package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Path;

public class CdupCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "CDUP";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {

        Path current = session.getCurrentDirectory().normalize().toAbsolutePath();
        Path home = session.getHomeDirectory().normalize().toAbsolutePath();
        Path shared = session.getSharedDirectory().normalize().toAbsolutePath();

        Path root = current.startsWith(shared) ? shared : home;

        if (current.equals(root)) {
            return FtpResponse.ok(250, "Directory successfully changed.");
        }

        Path parent = current.getParent();
        if (parent == null || !parent.startsWith(root)) {
            parent = root;
        }

        session.setCurrentDirectory(parent);

        return FtpResponse.ok(250, "Directory successfully changed.");
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}