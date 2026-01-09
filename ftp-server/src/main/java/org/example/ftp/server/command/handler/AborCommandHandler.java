package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;


public class AborCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "ABOR";
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        
        session.requestTransferAbort();
        return Responses.ok(226, "Abort successful.");
    }

    @Override
    protected FtpResponse notAllowed() {
        
        return Responses.ok(226, "Abort successful.");
    }
}

