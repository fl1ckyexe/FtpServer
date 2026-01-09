package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

public class TypeCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "TYPE";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        String mode = (argument != null && !argument.isBlank()) ? argument : "I";
        return Responses.ok(200, "Type set to " + mode + ".");
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}