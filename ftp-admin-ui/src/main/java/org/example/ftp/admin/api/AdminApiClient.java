package org.example.ftp.admin.api;

import org.example.ftp.admin.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AdminApiClient {

    private static final String BASE_URL = "http://localhost:9090";


    private static final String TOKEN = "ADMIN_SECRET";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();


    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + TOKEN);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static IOException httpError(HttpResponse<String> resp) {
        String body = resp.body();
        String msg = "HTTP " + resp.statusCode() + (body == null || body.isBlank() ? "" : (": " + body));
        return new IOException(msg);
    }

    public UserPermissionsDto getUserPermissions(String username)
            throws IOException, InterruptedException {

        HttpRequest req = request("/api/user-permissions?user=" + urlEncode(username))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) throw httpError(resp);

        String obj = resp.body();
        return new UserPermissionsDto(
                extractString(obj, "username"),
                extractBoolean(obj, "read"),
                extractBoolean(obj, "write"),
                extractBoolean(obj, "execute")
        );
    }

    public void savePermissions(String username, boolean r, boolean w, boolean e)
            throws IOException, InterruptedException {

        String json = """
            {
              "username":"%s",
              "read":%s,
              "write":%s,
              "execute":%s
            }
            """.formatted(
                escapeJson(username),
                r, w, e
        );

        HttpRequest req = request("/api/user-permissions")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 204) throw httpError(resp);
    }


    public List<UserDto> getUsers() throws IOException, InterruptedException {
        HttpRequest req = request("/api/users").GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) throw httpError(resp);
        return parseUsersArray(resp.body());
    }

    
    public void createUser(CreateUserRequestDto user) throws IOException, InterruptedException {
        String json = """
            {
              "username":"%s",
              "password":"%s"
            }
            """.formatted(
                escapeJson(user.getUsername()),
                escapeJson(user.getPassword())
        );

        HttpRequest req = request("/api/users")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 201) throw httpError(resp);
    }

    
    public void deleteUser(String username) throws IOException, InterruptedException {
        HttpRequest req = request("/api/users/" + urlEncode(username)).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 204) throw httpError(resp);
    }

    
    public void updateUser(String username, boolean enabled, Long rateLimit)
            throws IOException, InterruptedException {

        String json = (rateLimit == null)
                ? """
                   { "enabled": %s }
                   """.formatted(enabled)
                : """
                   { "enabled": %s, "rateLimit": %d }
                   """.formatted(enabled, rateLimit);

        HttpRequest req = request("/api/users/" + urlEncode(username))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) throw httpError(resp);
    }

    
    public List<UserStatsDto> getStats() throws IOException, InterruptedException {
        HttpRequest req = request("/api/stats").GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        
        if (resp.statusCode() != 200) throw httpError(resp);
        return parseStatsArray(resp.body());
    }

    
    public List<FolderPermissionDto> getFolderPermissions(String username)
            throws IOException, InterruptedException {

        HttpRequest req = request("/api/folders/permissions?username=" + urlEncode(username))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) throw httpError(resp);

        return parseFolderPermissions(resp.body());
    }

    
    public void saveFolderPermission(String user, String folder, boolean r, boolean w, boolean e)
            throws IOException, InterruptedException {

        String json = """
            {
              "user":"%s",
              "folder":"%s",
              "r":%s,
              "w":%s,
              "e":%s
            }
            """.formatted(
                escapeJson(user),
                escapeJson(folder),
                r, w, e
        );

        HttpRequest req = request("/api/folders/permissions/save")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 204) throw httpError(resp);
    }

    
    private List<UserDto> parseUsersArray(String json) {
        List<UserDto> out = new ArrayList<>();
        for (String obj : splitArrayObjects(json)) {
            out.add(new UserDto(
                    extractString(obj, "username"),
                    extractBoolean(obj, "enabled")
            ));
        }
        return out;
    }

    private List<UserStatsDto> parseStatsArray(String json) {
        List<UserStatsDto> out = new ArrayList<>();
        for (String obj : splitArrayObjects(json)) {
            out.add(new UserStatsDto(
                    extractString(obj, "username"),
                    extractInt(obj, "logins"),
                    extractLong(obj, "bytesUploaded"),
                    extractLong(obj, "bytesDownloaded"),
                    extractString(obj, "lastLogin")
            ));
        }
        return out;
    }

    private List<FolderPermissionDto> parseFolderPermissions(String json) {
        List<FolderPermissionDto> out = new ArrayList<>();
        for (String obj : splitArrayObjects(json)) {
            out.add(new FolderPermissionDto(
                    extractString(obj, "folder"),
                    extractBoolean(obj, "read"),
                    extractBoolean(obj, "write"),
                    extractBoolean(obj, "execute")
            ));
        }
        return out;
    }

    
    private List<String> splitArrayObjects(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;

        String s = json.trim();
        if (s.isEmpty() || s.equals("[]")) return out;
        if (!s.startsWith("[") || !s.endsWith("]")) return out;

        String body = s.substring(1, s.length() - 1).trim();
        if (body.isEmpty()) return out;

        String[] items = body.split("\\},\\s*\\{");
        for (String it : items) {
            String obj = it;
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";
            out.add(obj);
        }
        return out;
    }

    private String extractString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        i += p.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }

    private boolean extractBoolean(String json, String key) {
        String p = "\"" + key + "\":";
        int i = json.indexOf(p);
        if (i < 0) return false;
        i += p.length();
        return json.startsWith("true", i);
    }

    private int extractInt(String json, String key) {
        return (int) extractLong(json, key);
    }

    private long extractLong(String json, String key) {
        String p = "\"" + key + "\":";
        int i = json.indexOf(p);
        if (i < 0) return 0L;

        i += p.length();
        int j = json.indexOf(",", i);
        if (j < 0) j = json.indexOf("}", i);
        if (j < 0) return 0L;

        try {
            return Long.parseLong(json.substring(i, j).trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}