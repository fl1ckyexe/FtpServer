package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.stats.model.UserStats;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class LiveStatsHandler implements HttpHandler {

    private final StatsService statsService;
    private final ConnectionLimiter connectionLimiter;

    public LiveStatsHandler(StatsService statsService, ConnectionLimiter connectionLimiter) {
        this.statsService = statsService;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Map<String, Integer> perUser = connectionLimiter.snapshotPerUserConnections();
            int connectedUsers = connectionLimiter.getConnectedUsersCount();
            int totalConnections = connectionLimiter.getCurrentConnections();

            List<UserStats> base = statsService.getAllUserStats();
            Map<String, UserStats> byUser = new HashMap<>();
            for (UserStats s : base) {
                byUser.put(s.username(), s);
            }

            for (String u : perUser.keySet()) {
                byUser.putIfAbsent(u, new UserStats(u, 0, 0L, 0L, null));
            }

            List<String> usernames = new ArrayList<>(byUser.keySet());
            usernames.sort(String::compareToIgnoreCase);

            StringBuilder usersJson = new StringBuilder();
            usersJson.append("[");
            boolean first = true;
            for (String u : usernames) {
                UserStats s = byUser.get(u);
                int connections = perUser.getOrDefault(u, 0);
                boolean connected = connections > 0;

                if (!first) usersJson.append(",");
                first = false;

                usersJson.append(String.format(
                        "{\"username\":\"%s\",\"connected\":%s,\"connections\":%d,\"bytesUploaded\":%d,\"bytesDownloaded\":%d,\"lastLogin\":\"%s\"}",
                        escapeJson(u),
                        connected,
                        connections,
                        s.bytesUploaded(),
                        s.bytesDownloaded(),
                        s.lastLogin() == null ? "" : escapeJson(String.valueOf(s.lastLogin()))
                ));
            }
            usersJson.append("]");

            String json = String.format(
                    "{\"connectedUsers\":%d,\"totalConnections\":%d,\"users\":%s}",
                    connectedUsers,
                    totalConnections,
                    usersJson
            );

            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte[] msg = ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(500, msg.length);
            exchange.getResponseBody().write(msg);
        } finally {
            exchange.close();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

