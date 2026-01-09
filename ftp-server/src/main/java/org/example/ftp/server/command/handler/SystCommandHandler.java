package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

public class SystCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "SYST";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        
        
        return Responses.ok(215, "UNIX Type: L8");
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}

