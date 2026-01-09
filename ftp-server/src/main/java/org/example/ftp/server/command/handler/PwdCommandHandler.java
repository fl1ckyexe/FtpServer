package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpReplyCode;
import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.session.FtpSession;

import java.nio.file.Path;

public class PwdCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "PWD";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {

        Path current = session.getCurrentDirectory().normalize().toAbsolutePath();

        Path shared = session.getSharedDirectory().normalize().toAbsolutePath();
        if (current.startsWith(shared)) {
            Path rel = shared.relativize(current);
            String ftpPath = rel.toString().replace('\\', '/');
            ftpPath = ftpPath.isEmpty() ? "/shared" : "/shared/" + ftpPath;

            return FtpResponse.ok(
                    FtpReplyCode.PATHNAME_CREATED.getCode(),
                    "\"" + ftpPath + "\""
            );
        }

        Path home = session.getHomeDirectory().normalize().toAbsolutePath();
        Path relative = home.relativize(current);
        String ftpPath = relative.toString().replace('\\', '/');

        if (ftpPath.isEmpty()) {
            
            ftpPath = "/" + session.getUsername();
        } else {
            ftpPath = "/" + session.getUsername() + "/" + ftpPath;
        }

        return FtpResponse.ok(
                FtpReplyCode.PATHNAME_CREATED.getCode(),
                "\"" + ftpPath + "\""
        );
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}