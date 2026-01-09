package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.command.FtpCommandHandler;
import org.example.ftp.server.command.visitor.CommandVisitor;
import org.example.ftp.server.fs.log.ServerLogService;
import org.example.ftp.server.session.FtpSession;
import org.example.ftp.server.session.memento.SessionMemento;

public abstract class AbstractCommandHandler implements FtpCommandHandler {

    public final FtpResponse handle(FtpSession session, String commandLine) {

        if (commandLine == null || commandLine.isBlank()) {
            return Responses.emptyCommand();
        }

        log(session, commandLine);

        SessionMemento snapshot = session.save();

        if (!checkState(session)) {
            return notAllowed();
        }

        try {
            return execute(session, extractArgument(commandLine));
        } catch (Exception e) {
            session.restore(snapshot);
            return Responses.requestedActionAbortedLocalError();
        }
    }

    protected void log(FtpSession session, String commandLine) {
        String safeCmd = sanitizeCommand(commandLine);
        String safeUser = extractUsername(session);
        ServerLogService.log(safeUser + " >> " + safeCmd);
    }

    private String sanitizeCommand(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return "";
        }
        String cmd = commandLine.trim();
        if (cmd.regionMatches(true, 0, "PASS", 0, 4)) {
            return "PASS ******";
        }
        return cmd;
    }

    private String extractUsername(FtpSession session) {
        if (session == null) {
            return "unknown-session";
        }
        if (session.getUsername() != null) {
            return session.getUsername();
        }
        if (session.getPendingUsername() != null) {
            return session.getPendingUsername();
        }
        return "anonymous";
    }

    @Override
    public void accept(CommandVisitor visitor) {
        visitor.visit(this);
    }

    protected String getStateKey() {
        return getCommandName();
    }

    protected boolean checkState(FtpSession session) {
        return session.getState().canExecute(getStateKey());
    }

    public abstract String getCommandName();

    protected abstract FtpResponse execute(FtpSession session, String argument);

    protected abstract FtpResponse notAllowed();

    protected String extractArgument(String commandLine) {
        int idx = commandLine.indexOf(' ');
        return idx > 0 ? commandLine.substring(idx + 1) : null;
    }
}
