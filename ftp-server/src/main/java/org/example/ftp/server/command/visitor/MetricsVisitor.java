package org.example.ftp.server.command.visitor;

import org.example.ftp.server.command.handler.AbstractCommandHandler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class MetricsVisitor implements CommandVisitor {

    private static final ConcurrentHashMap<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    @Override
    public void visit(AbstractCommandHandler handler) {
        String key = handler.getCommandName();
        COUNTERS.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public static Map<String, Long> snapshot() {
        Map<String, Long> out = new LinkedHashMap<>();
        COUNTERS.keySet().stream().sorted().forEach(cmd -> out.put(cmd, COUNTERS.get(cmd).sum()));
        return Collections.unmodifiableMap(out);
    }

    public static void reset() {
        COUNTERS.clear();
    }
}