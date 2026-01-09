package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.command.visitor.MetricsVisitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        if ("GET".equals(method)) {
            handleGet(exchange);
            return;
        }

        if ("POST".equals(method) && exchange.getRequestURI().getPath().endsWith("/reset")) {
            handleReset(exchange);
            return;
        }

        exchange.sendResponseHeaders(405, -1);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        Map<String, Long> metrics = MetricsVisitor.snapshot();

        String json = metrics.entrySet().stream()
                .map(e -> String.format("{\"command\":\"%s\",\"count\":%d}",
                        escape(e.getKey()),
                        e.getValue()
                ))
                .collect(Collectors.joining(",", "[", "]"));

        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        MetricsVisitor.reset();
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}