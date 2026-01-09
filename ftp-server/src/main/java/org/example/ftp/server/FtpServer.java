package org.example.ftp.server;

import org.example.ftp.server.auth.AuthService;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.Sha256PasswordHasher;
import org.example.ftp.server.auth.db.SqliteFolderPermissionRepository;
import org.example.ftp.server.auth.db.SqliteFolderRepository;
import org.example.ftp.server.auth.db.SqlitePermissionsRepository;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.db.Db;
import org.example.ftp.server.db.SqliteServerSettingsRepository;
import org.example.ftp.server.http.AdminHttpServer;
import org.example.ftp.server.http.AdminTokenService;
import org.example.ftp.server.session.ActiveSessionRegistry;
import org.example.ftp.server.session.FtpSession;
import org.example.ftp.server.session.FtpSessionBuilder;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.stats.db.SqliteStatsRepository;
import org.example.ftp.server.transfer.RateLimiter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.awt.Desktop;
import java.net.SocketException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class FtpServer {

    private final int port;

    private AuthService authService;
    private PermissionService permissionService;
    private ConnectionLimiter connectionLimiter;
    private RateLimiter globalUploadRateLimiter;
    private RateLimiter globalDownloadRateLimiter;
    private StatsService statsService;
    
    private SqliteUserRepository userRepo;
    private SqliteFolderRepository folderRepo;
    private SqliteFolderPermissionRepository folderPermRepo;
    private SqliteSharedFolderRepository sharedFolderRepo;

    private final int adminPort;
    private final ActiveSessionRegistry sessionRegistry = new ActiveSessionRegistry();
    private AdminTokenService adminTokenService;

    public FtpServer(int port) {
        this(port, 9090);
    }

    public FtpServer(int port, int adminPort) {
        this.port = port;
        this.adminPort = adminPort;
    }

    public void start() throws Exception {
        Path ftpRoot = resolveFtpRoot();
        Files.createDirectories(ftpRoot);

        Db db = new Db(ftpRoot.resolve("ftp.db"));
        db.initSchema();

        var statsRepo = new SqliteStatsRepository(db);
        this.statsService = new StatsService(statsRepo);

        var userRepo = new SqliteUserRepository(db);
        var permRepo = new SqlitePermissionsRepository(db);

        this.authService = new AuthService(userRepo, new Sha256PasswordHasher());
        this.permissionService = new PermissionService(permRepo);

        
        this.connectionLimiter = new ConnectionLimiter(20);
        this.globalUploadRateLimiter = new RateLimiter(200_000);
        this.globalDownloadRateLimiter = new RateLimiter(200_000);

        SqliteServerSettingsRepository settingsRepo = new SqliteServerSettingsRepository(db);
        var settings = settingsRepo.get();
        if (settings != null) {
            this.connectionLimiter.setMaxConnections(settings.globalMaxConnections());
            
            this.globalUploadRateLimiter.setLimit(settings.globalUploadLimit());
            this.globalDownloadRateLimiter.setLimit(settings.globalDownloadLimit());
        }

        
        this.adminTokenService = new AdminTokenService(settingsRepo.getAdminToken());

        this.folderRepo = new SqliteFolderRepository(db);
        this.folderPermRepo = new SqliteFolderPermissionRepository(db);
        this.userRepo = userRepo;
        this.sharedFolderRepo = new SqliteSharedFolderRepository(db);

        
        System.out.println("Resolved ftp-root: " + ftpRoot.toAbsolutePath().normalize());
        System.out.println("Resolved ftp-db  : " + ftpRoot.resolve("ftp.db").toAbsolutePath().normalize());

        AdminHttpServer.start(
                this.authService,
                this.connectionLimiter,
                this.globalUploadRateLimiter,
                this.globalDownloadRateLimiter,
                this.statsService,
                this.permissionService,
                this.                sessionRegistry,
                this.adminTokenService,
                userRepo,
                folderRepo,
                folderPermRepo,
                this.sharedFolderRepo,
                settingsRepo,
                ftpRoot,
                this.adminPort
        );

        
        openBrowserSilently("http://localhost:" + this.adminPort + "/");

        System.out.println("FTP Server starting on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client, ftpRoot)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path resolveFtpRoot() {
        
        Path configured = resolveConfiguredFtpRoot();
        if (configured != null) {
            return configured;
        }

        
        try {
            var uri = FtpServer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(uri).toAbsolutePath().normalize();
            Path baseDir = Files.isDirectory(codePath) ? codePath : codePath.getParent();
            if (baseDir != null) {
                Path nearBinary = baseDir.resolve("ftp-root").toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(nearBinary)) {
                    return nearBinary;
                }
            }
        } catch (Exception ignored) {
        }

        Path userData = resolveUserDataRoot();
        if (userData != null) {
            return userData;
        }

        Path p = Path.of("ftp-root");
        if (Files.exists(p)) return p.toAbsolutePath().normalize();

        Path p2 = Path.of("..", "ftp-root");
        if (Files.exists(p2)) return p2.toAbsolutePath().normalize();

        return p.toAbsolutePath().normalize();
    }

    private static Path resolveConfiguredFtpRoot() {
        
        try {
            String prop = System.getProperty("ftp.root");
            if (prop != null && !prop.isBlank()) {
                Path p = Path.of(prop).toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(p)) return p;
            }
        } catch (Exception ignored) {}

        
        try {
            String env = System.getenv("FTP_ROOT");
            if (env != null && !env.isBlank()) {
                Path p = Path.of(env).toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(p)) return p;
            }
        } catch (Exception ignored) {}

        
        try {
            for (Path cfg : resolveFtpRootConfigPaths()) {
                if (!Files.exists(cfg)) continue;

                List<String> lines = Files.readAllLines(cfg, StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;

                String raw = lines.get(0).trim();
                if (raw.isBlank()) continue;

                Path p = Path.of(raw);
                if (!p.isAbsolute()) {
                    
                    Path base = cfg.toAbsolutePath().normalize().getParent();
                    if (base != null) p = base.resolve(p);
                }
                p = p.toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(p)) return p;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static List<Path> resolveFtpRootConfigPaths() {
        java.util.ArrayList<Path> candidates = new java.util.ArrayList<>();

        
        try {
            var uri = FtpServer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(uri).toAbsolutePath().normalize();
            Path baseDir = Files.isDirectory(codePath) ? codePath : codePath.getParent();
            if (baseDir != null) {
                candidates.add(baseDir.resolve("ftp-root.path").toAbsolutePath().normalize());
            }
        } catch (Exception ignored) {}

        
        try {
            String programData = System.getenv("PROGRAMDATA");
            if (programData != null && !programData.isBlank()) {
                candidates.add(Path.of(programData, "FtpServer", "ftp-root.path").toAbsolutePath().normalize());
            }
        } catch (Exception ignored) {}

        
        try {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                candidates.add(Path.of(localAppData, "FtpServer", "ftp-root.path").toAbsolutePath().normalize());
            }
        } catch (Exception ignored) {}

        return candidates;
    }

    private static Path resolveUserDataRoot() {
        try {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                Path p = Path.of(localAppData, "FtpServer", "ftp-root").toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(p)) return p;
            }
        } catch (Exception ignored) {}

        try {
            String home = System.getProperty("user.home");
            if (home != null && !home.isBlank()) {
                Path p = Path.of(home, "FtpServer", "ftp-root").toAbsolutePath().normalize();
                if (isWritableDirectoryOrCanCreate(p)) return p;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static boolean isWritableDirectoryOrCanCreate(Path dir) {
        try {
            Files.createDirectories(dir);
            return Files.isWritable(dir);
        } catch (Exception e) {
            return false;
        }
    }

    private static void openBrowserSilently(String url) {
        try {
            URI uri = URI.create(url);

            if (Desktop.isDesktopSupported()) {
                Desktop d = Desktop.getDesktop();
                if (d.isSupported(Desktop.Action.BROWSE)) {
                    d.browse(uri);
                    return;
                }
            }

            try {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url)
                        .inheritIO()
                        .start();
            } catch (Exception ignored) {
            }

        } catch (Exception ignored) {
        }
    }

    
    private void handleClient(Socket socket, Path ftpRoot) {
        FtpSession session = null;

        try (
                socket;
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                        true
                );
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                )
        ) {
            sessionRegistry.register(socket);
            out.print("220 FTP Server Ready\r\n");
            out.flush();

            session = FtpSessionBuilder.create()
                    .writer(out)
                    .ftpRoot(ftpRoot)
                    .authService(authService)
                    .permissionService(permissionService)
                    .statsService(statsService)
                    .connectionLimiter(connectionLimiter)
                    .globalUploadRateLimiter(globalUploadRateLimiter)
                    .globalDownloadRateLimiter(globalDownloadRateLimiter)
                    .userRepository(userRepo)
                    .folderRepository(folderRepo)
                    .folderPermissionRepository(folderPermRepo)
                    .sharedFolderRepository(sharedFolderRepo)
                    .build();

            while (true) {
                final String line;
                try {
                    line = in.readLine();
                } catch (SocketException se) {
                    
                    if (isClientDisconnect(se)) {
                        break;
                    }
                    throw se;
                }

                if (line == null) {
                    
                    break;
                }

                try {
                    session.handle(line);
                } catch (Throwable t) {
                    
                    t.printStackTrace();
                    break;
                }

                if (session.isCloseRequested()) {
                    break;
                }
            }

        } catch (IOException e) {
            if (!isClientDisconnect(e)) {
                e.printStackTrace();
            }
        } finally {
            sessionRegistry.unregister(socket);
            if (session != null && session.isAuthenticated()) {
                connectionLimiter.release(session.getUsername());
            }
        }
    }

    private static boolean isClientDisconnect(IOException e) {
        if (e == null) return false;
        if (e instanceof SocketException) return true;
        String msg = e.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("connection reset")
                || m.contains("socket closed")
                || m.contains("forcibly closed")
                || m.contains("was aborted")
                || m.contains("terminated")
                || m.contains("forcibly")
                || m.contains("reset");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ConnectionLimiter getConnectionLimiter() {
        return connectionLimiter;
    }

    public RateLimiter getGlobalRateLimiter() {
        return globalDownloadRateLimiter;
    }
}