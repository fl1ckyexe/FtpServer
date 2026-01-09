package org.example.ftp.server.session;

import org.example.ftp.server.auth.AuthService;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.db.SqliteFolderPermissionRepository;
import org.example.ftp.server.auth.db.SqliteFolderRepository;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.transfer.RateLimiter;

import java.io.PrintWriter;
import java.nio.file.Path;


public final class FtpSessionBuilder {

    private PrintWriter writer;
    private Path ftpRoot;
    private AuthService authService;
    private PermissionService permissionService;
    private StatsService statsService;
    private ConnectionLimiter connectionLimiter;
    private RateLimiter globalUploadRateLimiter;
    private RateLimiter globalDownloadRateLimiter;
    private SqliteUserRepository userRepository;
    private SqliteFolderRepository folderRepository;
    private SqliteFolderPermissionRepository folderPermissionRepository;
    private SqliteSharedFolderRepository sharedFolderRepository;

    public static FtpSessionBuilder create() {
        return new FtpSessionBuilder();
    }

    public FtpSessionBuilder writer(PrintWriter writer) { this.writer = writer; return this; }
    public FtpSessionBuilder ftpRoot(Path ftpRoot) { this.ftpRoot = ftpRoot; return this; }
    public FtpSessionBuilder authService(AuthService authService) { this.authService = authService; return this; }
    public FtpSessionBuilder permissionService(PermissionService permissionService) { this.permissionService = permissionService; return this; }
    public FtpSessionBuilder statsService(StatsService statsService) { this.statsService = statsService; return this; }
    public FtpSessionBuilder connectionLimiter(ConnectionLimiter connectionLimiter) { this.connectionLimiter = connectionLimiter; return this; }
    public FtpSessionBuilder globalUploadRateLimiter(RateLimiter limiter) { this.globalUploadRateLimiter = limiter; return this; }
    public FtpSessionBuilder globalDownloadRateLimiter(RateLimiter limiter) { this.globalDownloadRateLimiter = limiter; return this; }
    public FtpSessionBuilder userRepository(SqliteUserRepository repo) { this.userRepository = repo; return this; }
    public FtpSessionBuilder folderRepository(SqliteFolderRepository repo) { this.folderRepository = repo; return this; }
    public FtpSessionBuilder folderPermissionRepository(SqliteFolderPermissionRepository repo) { this.folderPermissionRepository = repo; return this; }
    public FtpSessionBuilder sharedFolderRepository(SqliteSharedFolderRepository repo) { this.sharedFolderRepository = repo; return this; }

    public FtpSession build() {
        if (writer == null) throw new IllegalStateException("writer is required");
        if (ftpRoot == null) throw new IllegalStateException("ftpRoot is required");
        if (authService == null) throw new IllegalStateException("authService is required");
        if (permissionService == null) throw new IllegalStateException("permissionService is required");
        if (statsService == null) throw new IllegalStateException("statsService is required");
        if (connectionLimiter == null) throw new IllegalStateException("connectionLimiter is required");
        if (globalUploadRateLimiter == null) throw new IllegalStateException("globalUploadRateLimiter is required");
        if (globalDownloadRateLimiter == null) throw new IllegalStateException("globalDownloadRateLimiter is required");
        if (userRepository == null) throw new IllegalStateException("userRepository is required");
        if (folderRepository == null) throw new IllegalStateException("folderRepository is required");
        if (folderPermissionRepository == null) throw new IllegalStateException("folderPermissionRepository is required");
        if (sharedFolderRepository == null) throw new IllegalStateException("sharedFolderRepository is required");

        return new FtpSession(
                writer,
                ftpRoot,
                authService,
                permissionService,
                statsService,
                connectionLimiter,
                globalUploadRateLimiter,
                globalDownloadRateLimiter,
                userRepository,
                folderRepository,
                folderPermissionRepository,
                sharedFolderRepository
        );
    }
}

