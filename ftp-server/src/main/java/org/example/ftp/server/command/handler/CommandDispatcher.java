package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.command.visitor.LoggingVisitor;
import org.example.ftp.server.command.visitor.MetricsVisitor;
import org.example.ftp.server.command.visitor.VisitorPipeline;
import org.example.ftp.server.session.FtpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandDispatcher {

    private final Map<String, AbstractCommandHandler> handlers = new HashMap<>();

    private final VisitorPipeline visitorPipeline = new VisitorPipeline(
            
            Boolean.getBoolean("ftp.debug")
                    ? List.of(new LoggingVisitor(), new MetricsVisitor())
                    : List.of(new MetricsVisitor())
    );

    public CommandDispatcher() {
        handlers.put("USER", new UserCommandHandler());
        handlers.put("PASS", new PassCommandHandler());
        handlers.put("PWD",  new PwdCommandHandler());
        handlers.put("QUIT", new QuitCommandHandler());
        handlers.put("TYPE", new TypeCommandHandler());
        handlers.put("NOOP", new NoopCommandHandler());
        handlers.put("ABOR", new AborCommandHandler());
        handlers.put("PASV", new PasvCommandHandler());
        handlers.put("LIST", new ListCommandHandler());
        handlers.put("CWD", new CwdCommandHandler());
        handlers.put("CDUP", new CdupCommandHandler());
        handlers.put("RETR", new RetrCommandHandler());
        handlers.put("STOR", new StorCommandHandler());
        handlers.put("MKD", new MkdCommandHandler());
        handlers.put("MLSD", new MlsdCommandHandler());
        handlers.put("DELE", new DeleCommandHandler());
        handlers.put("RMD", new RmdCommandHandler());
        handlers.put("LOGS", new LogsCommandHandler());
        handlers.put("EPSV", new EpsvCommandHandler());
        handlers.put("EPRT", new EpsvCommandHandler());
        handlers.put("OPTS", new OptsCommandHandler());
        handlers.put("FEAT", new FeatCommandHandler());
        handlers.put("SYST", new SystCommandHandler());
    }

    public FtpResponse dispatch(FtpSession session, String line) {

        if (line == null || line.isBlank()) {
            return Responses.emptyCommand();
        }

        String command;
        int space = line.indexOf(' ');

        if (space > 0) {
            command = line.substring(0, space).toUpperCase();
        } else {
            command = line.toUpperCase();
        }

        AbstractCommandHandler handler = handlers.get(command);

        if (handler == null) {
            return Responses.notImplemented();
        }

        visitorPipeline.accept(handler);

        return handler.handle(session, line);
    }
}