package org.example.ftp.server.command.visitor;

import org.example.ftp.server.command.handler.AbstractCommandHandler;


public interface CommandVisitor {
    
    void visit(AbstractCommandHandler handler);
}
