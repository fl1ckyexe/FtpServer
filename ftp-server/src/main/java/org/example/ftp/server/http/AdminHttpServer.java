package org.example.ftp.server.http;

import com.sun.net.httpserver.HttpServer;
import org.example.ftp.server.auth.AuthService;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.db.SqliteFolderPermissionRepository;
import org.example.ftp.server.auth.db.SqliteFolderRepository;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.db.SqliteServerSettingsRepository;
import org.example.ftp.server.http.handler.*;
import org.example.ftp.server.session.ActiveSessionRegistry;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.transfer.RateLimiter;

import java.net.InetSocketAddress;
import java.net.BindException;
import java.util.concurrent.Executors;

public class AdminHttpServer {

    public static void start(
            AuthService authService,
            ConnectionLimiter connectionLimiter,
            RateLimiter uploadRateLimiter,
            RateLimiter downloadRateLimiter,
            StatsService statsService,
            PermissionService permissionService,
            ActiveSessionRegistry sessionRegistry,
            AdminTokenService adminTokenService,

            SqliteUserRepository userRepo,
            SqliteFolderRepository folderRepo,
            SqliteFolderPermissionRepository folderPermRepo,
            SqliteSharedFolderRepository sharedFolderRepo,
            SqliteServerSettingsRepository settingsRepo,
            java.nio.file.Path ftpRoot
    ) throws Exception
    {

        start(
                authService,
                connectionLimiter,
                uploadRateLimiter,
                downloadRateLimiter,
                statsService,
                permissionService,
                sessionRegistry,
                adminTokenService,
                userRepo,
                folderRepo,
                folderPermRepo,
                sharedFolderRepo,
                settingsRepo,
                ftpRoot,
                9090
        );
    }

    public static void start(
            AuthService authService,
            ConnectionLimiter connectionLimiter,
            RateLimiter uploadRateLimiter,
            RateLimiter downloadRateLimiter,
            StatsService statsService,
            PermissionService permissionService,
            ActiveSessionRegistry sessionRegistry,
            AdminTokenService adminTokenService,

            SqliteUserRepository userRepo,
            SqliteFolderRepository folderRepo,
            SqliteFolderPermissionRepository folderPermRepo,
            SqliteSharedFolderRepository sharedFolderRepo,
            SqliteServerSettingsRepository settingsRepo,
            java.nio.file.Path ftpRoot,

            int port
    ) throws Exception {

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException be) {
            throw new BindException("Admin HTTP port " + port + " is already in use. " +
                    "Stop the previous server instance or run with -Dadmin.port=<freePort>.");
        }

        server.createContext(
                "/api/admin-token",
                new AdminTokenHandler(settingsRepo, adminTokenService)
        );

        server.createContext(
                "/api/bootstrap",
                new BootstrapHandler(adminTokenService)
        );

        server.createContext(
                "/api/runtime-info",
                new RuntimeInfoHandler(ftpRoot, adminTokenService)
        );

        var userAuthFilter = new UserAuthFilter(authService);

        server.createContext(
                "/api/users",
                new UsersHandler(authService, permissionService, userRepo)
        );

        server.createContext(
                "/api/user-permissions",
                new PermissionsHandler(permissionService)
        );

        server.createContext(
                "/api/limits",
                new LimitsHandler(connectionLimiter, uploadRateLimiter, downloadRateLimiter, settingsRepo, sessionRegistry)
        );

        server.createContext(
                "/api/stats",
                new StatsHandler(statsService)
        );

        server.createContext(
                "/api/stats/live",
                new LiveStatsHandler(statsService, connectionLimiter)
        );

        server.createContext(
                "/api/folders",
                new FoldersHandler(folderRepo)
        );

        server.createContext(
                "/api/folders/permissions",
                new FolderPermissionsGetHandler(userRepo, folderRepo, folderPermRepo)
        );

        server.createContext(
                "/api/folders/permissions/save",
                new FolderPermissionsPostHandler(userRepo, folderRepo, folderPermRepo)
        );

        
        server.createContext(
                "/api/shared-folders/all",
                new SharedFoldersAdminHandler(userRepo, sharedFolderRepo)
        );

        
        server.createContext(
                "/api/shared-folders/all/delete",
                new SharedFoldersAdminDeleteHandler(sharedFolderRepo)
        );

        
        server.createContext(
                "/api/shared-folders",
                new SharedFoldersGetHandler(userRepo, sharedFolderRepo)
        ).getFilters().add(userAuthFilter);

        server.createContext(
                "/api/shared-folders/share",
                new SharedFoldersPostHandler(userRepo, sharedFolderRepo)
        ).getFilters().add(userAuthFilter);

        server.createContext(
                "/api/shared-folders/delete",
                new SharedFoldersDeleteHandler(userRepo, sharedFolderRepo)
        ).getFilters().add(userAuthFilter);

        RootHandler rootHandler = new RootHandler(ftpRoot);
        server.createContext(
                "/api/root",
                rootHandler
        );
        server.createContext(
                "/api/root/create",
                rootHandler
        );

        server.createContext(
                "/api/metrics",
                new MetricsHandler()
        );

        server.createContext(
                "/api/metrics/reset",
                new MetricsHandler()
        );

        
        server.createContext(
                "/",
                new StaticAdminUiHandler()
        );

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Admin HTTP API started on port " + port);
    }

}