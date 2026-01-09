package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpReplyCode;
import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;
import org.example.ftp.server.session.UnauthenticatedState;

public class PassCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "PASS";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        if (session.getPendingUsername() == null) {
            return Responses.loginWithUserFirst();
        }

        String username = session.getPendingUsername();
        String password = argument == null ? "" : argument;

        boolean ok = session.getAuthService().authenticate(username, password);
        if (!ok) {
            session.setPendingUsername(null);
            session.setState(new UnauthenticatedState());
            return Responses.loginIncorrect();
        }

        if (!session.getConnectionLimiter().tryAcquire(username)) {
            session.requestClose();
            session.setState(new UnauthenticatedState());
            return Responses.error(421, "Too many connections.");
        }

        session.authenticate(username);

        
        if (session.getPermissionService().getPermissions(username) == null) {
            session.getPermissionService().setPermissions(username, true, true, true);
        }

        session.getStatsService().onLogin(username);

        return Responses.ok(FtpReplyCode.USER_LOGGED_IN.getCode(), "User logged in, proceed.");
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.loginWithUserFirst();
    }
}