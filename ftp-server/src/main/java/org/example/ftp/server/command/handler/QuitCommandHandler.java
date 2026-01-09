package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

public class QuitCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "QUIT";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        session.requestClose();
        return Responses.goodbye();
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.goodbye();
    }
}