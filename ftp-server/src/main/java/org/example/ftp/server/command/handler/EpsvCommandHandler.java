package org.example.ftp.server.command.handler;


import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.session.FtpSession;

public class EpsvCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "EPSV";
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        return FtpResponse.builder()
                .code(502)
                .message("EPSV not supported, use PASV.")
                .build();


    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.builder()
                .code(530)
                .message("Please login first.")
                .build();


    }
}
