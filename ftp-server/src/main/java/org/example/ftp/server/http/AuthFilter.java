package org.example.ftp.server.http;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

public class AuthFilter extends Filter {

    private final AdminTokenService tokenService;

    public AuthFilter(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
            throws IOException {

        String expected = tokenService == null ? null : tokenService.getToken();
        if (expected == null || expected.isBlank()) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        List<String> auth = exchange
                .getRequestHeaders()
                .get("Authorization");

        if (auth == null ||
                auth.isEmpty() ||
                !auth.get(0).equals("Bearer " + expected)) {

            exchange.sendResponseHeaders(401, -1);
            return;
        }

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "Bearer auth filter";
    }
}
