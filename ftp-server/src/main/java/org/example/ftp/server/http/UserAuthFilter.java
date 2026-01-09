package org.example.ftp.server.http;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.example.ftp.server.auth.AuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;


public class UserAuthFilter extends Filter {

    private final AuthService authService;

    public UserAuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
            throws IOException {

        List<String> auth = exchange
                .getRequestHeaders()
                .get("Authorization");

        if (auth == null || auth.isEmpty() || !auth.get(0).startsWith("Basic ")) {
            exchange.sendResponseHeaders(401, -1);
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"FTP Server\"");
            return;
        }

        String basicAuth = auth.get(0);
        String encoded = basicAuth.substring(6); 

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(401, -1);
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"FTP Server\"");
            return;
        }

        int colonIndex = decoded.indexOf(':');
        if (colonIndex < 0) {
            exchange.sendResponseHeaders(401, -1);
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"FTP Server\"");
            return;
        }

        String username = decoded.substring(0, colonIndex);
        String password = decoded.substring(colonIndex + 1);

        if (!authService.authenticate(username, password)) {
            exchange.sendResponseHeaders(401, -1);
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"FTP Server\"");
            return;
        }

        
        exchange.setAttribute("authenticatedUsername", username);
        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "Basic auth filter for user authentication";
    }
}
