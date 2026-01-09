package org.example.ftp.server.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
 
public class StaticAdminUiHandler implements HttpHandler {

    private static final String ROOT = "admin-ui/";
    private static final String LOGO_JPG = "logo.jpg";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isBlank()) path = "/";

        if (path.startsWith("/api/")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        if (path.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        if (path.equals("/")) path = "/index.html";

        if (!path.contains(".")) path = "/index.html";

        String normalized = stripLeadingSlash(path);

        if (LOGO_JPG.equals(normalized)) {
            byte[] body = readResourceBytes(ROOT + LOGO_JPG);
            if (body == null) {
                body = readLogoFromFs();
            }
            if (body == null) {
                body = generateLogoJpeg();
            }

            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");

            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
            return;
        }

        String resource = ROOT + normalized;
        byte[] body = readResourceBytes(resource);
        if (body == null) {
            body = readResourceBytes(ROOT + "index.html");
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            resource = ROOT + "index.html";
        }

        String contentType = guessContentType(resource);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.getResponseHeaders().add("Cache-Control", "no-store");

        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String stripLeadingSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private static byte[] readResourceBytes(String classpathPath) throws IOException {
        ClassLoader cl = StaticAdminUiHandler.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(classpathPath)) {
            if (is == null) return null;
            return is.readAllBytes();
        }
    }

    private static String guessContentType(String resourcePath) {
        String ct = URLConnection.guessContentTypeFromName(resourcePath);
        if (ct != null) return ct;

        if (resourcePath.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (resourcePath.endsWith(".css")) return "text/css; charset=utf-8";
        if (resourcePath.endsWith(".html")) return "text/html; charset=utf-8";
        if (resourcePath.endsWith(".json")) return "application/json; charset=utf-8";
        if (resourcePath.endsWith(".svg")) return "image/svg+xml";
        if (resourcePath.endsWith(".jpg") || resourcePath.endsWith(".jpeg")) return "image/jpeg";
        if (resourcePath.endsWith(".png")) return "image/png";
        if (resourcePath.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static byte[] readLogoFromFs() {
        Path p1 = Path.of("ftp-admin-ui", "src", "main", "resources", "icons", "logo.jpg");
        Path p2 = Path.of("..", "ftp-admin-ui", "src", "main", "resources", "icons", "logo.jpg");

        try {
            if (Files.exists(p1)) return Files.readAllBytes(p1);
        } catch (Exception ignored) {
            
        }

        try {
            if (Files.exists(p2)) return Files.readAllBytes(p2);
        } catch (Exception ignored) {
            
        }

        return null;
    }

    private static byte[] generateLogoJpeg() {
        try {
            int w = 256, h = 256;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g.setColor(Color.BLACK);
                g.fillRect(0, 0, w, h);

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 72));
                g.drawString("FTP", 36, 110);

                g.setFont(new Font("SansSerif", Font.PLAIN, 42));
                g.drawString("admin", 50, 190);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}

