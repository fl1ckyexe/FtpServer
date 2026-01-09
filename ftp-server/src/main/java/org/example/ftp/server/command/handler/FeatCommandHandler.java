package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.session.FtpSession;

public class FeatCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "FEAT";
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        return FtpResponse.builder()
                .code(211)
                .line("Features:")
                .line(" UTF8")
                .line("End")
                .build();
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.builder()
                .code(211)
                .line("Features:")
                .line(" UTF8")
                .line("End")
                .build();
    }
}