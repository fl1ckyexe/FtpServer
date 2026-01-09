package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.fs.log.ServerLogService;
import org.example.ftp.server.session.FtpSession;

public class LogsCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "LOGS";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {

        var logs = ServerLogService.getLogs();

        FtpResponse.Builder b = FtpResponse.builder().code(200);

        if (logs.isEmpty()) {
            b.line("No logs.");
        } else {
            b.line("Server logs:");
            for (String line : logs) {
                b.line(line);
            }
            b.line("End");
        }

        return b.build();
    }

    @Override
    protected FtpResponse notAllowed() {
        return FtpResponse.needLogin();
    }
}