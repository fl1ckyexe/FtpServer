package org.example.ftp.server.command.visitor;

import org.example.ftp.server.command.handler.AbstractCommandHandler;

import java.util.List;

public class VisitorPipeline {

    private final List<CommandVisitor> visitors;

    public VisitorPipeline(List<CommandVisitor> visitors) {
        this.visitors = List.copyOf(visitors);
    }

    public void accept(AbstractCommandHandler handler) {
        for (CommandVisitor v : visitors) {
            handler.accept(v);
        }
    }
}