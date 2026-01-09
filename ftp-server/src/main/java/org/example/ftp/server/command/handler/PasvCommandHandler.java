package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.session.FtpSession;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class PasvCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "PASV";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        try {
            session.openPassiveDataSocket();
            ServerSocket dataSocket = session.getPassiveDataSocket();

            InetAddress address = InetAddress.getLocalHost();
            int port = dataSocket.getLocalPort();

            int p1 = port / 256;
            int p2 = port % 256;

            String ip = address.getHostAddress().replace('.', ',');

            return Responses.ok(
                    227,
                    "Entering Passive Mode (" + ip + "," + p1 + "," + p2 + ")."
            );

        } catch (IOException e) {
            return Responses.error(425, "Can't open passive connection.");
        }
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}