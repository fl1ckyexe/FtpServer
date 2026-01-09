package org.example.ftp.server.command;

import org.example.ftp.server.command.visitor.CommandVisitor;

public interface FtpCommandHandler {
    void accept(CommandVisitor visitor);
}
