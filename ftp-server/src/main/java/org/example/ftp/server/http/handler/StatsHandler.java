package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.stats.model.ConnectionStat;
import org.example.ftp.server.stats.model.UserStats;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class StatsHandler implements HttpHandler {

    private final StatsService statsService;

    public StatsHandler(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {

            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            var stats = statsService.getAllUserStats();

            String json = stats.stream()
                    .map(s -> String.format(
                            "{\"username\":\"%s\",\"logins\":%d,\"bytesUploaded\":%d,\"bytesDownloaded\":%d,\"lastLogin\":\"%s\"}",
                            s.username(),
                            s.logins(),
                            s.bytesUploaded(),
                            s.bytesDownloaded(),
                            s.lastLogin()
                    ))
                    .collect(Collectors.joining(",", "[", "]"));

            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }

        } catch (Exception e) {
            e.printStackTrace();

            byte[] msg = ("ERROR: " + e.getMessage())
                    .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(500, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
        }
    }


}
