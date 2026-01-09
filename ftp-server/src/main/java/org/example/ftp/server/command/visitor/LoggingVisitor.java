package org.example.ftp.server.command.visitor;

import org.example.ftp.server.command.handler.AbstractCommandHandler;
import org.example.ftp.server.util.DebugLog;

public class LoggingVisitor implements CommandVisitor {

    @Override
    public void visit(AbstractCommandHandler handler) {
        
        DebugLog.d("Command executed: " + handler.getCommandName());
    }
}
