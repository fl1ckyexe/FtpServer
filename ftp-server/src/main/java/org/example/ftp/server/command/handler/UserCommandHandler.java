package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpReplyCode;
import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;
import org.example.ftp.server.session.AwaitingPasswordState;

public class UserCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "USER";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        if (argument == null || argument.isBlank()) {
            return Responses.syntaxErrorInParameters();
        }

        if (!session.getAuthService().userExists(argument)) {
            return Responses.error(530, "User not found.");
        }

        session.setPendingUsername(argument);
        session.setState(new AwaitingPasswordState());

        return Responses.ok(
                FtpReplyCode.USERNAME_OK.getCode(),
                "User name okay, need password."
        );
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.error(FtpReplyCode.NOT_LOGGED_IN.getCode(), "Not allowed.");
    }
}