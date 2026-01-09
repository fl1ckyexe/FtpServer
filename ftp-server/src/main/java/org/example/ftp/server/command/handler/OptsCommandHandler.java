package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

public class OptsCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "OPTS";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        if ("UTF8 ON".equalsIgnoreCase(argument)) {
            return Responses.ok(200, "UTF8 mode enabled.");
        }
        return Responses.notImplemented();
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.notImplemented();
    }
}