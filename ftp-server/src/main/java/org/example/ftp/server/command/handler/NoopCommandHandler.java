package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

public class NoopCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "NOOP";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        return Responses.ok(200, "OK.");
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.ok(200, "OK.");
    }
}