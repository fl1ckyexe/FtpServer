package org.example.ftp.server.session;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.server.auth.AuthService;
import org.example.ftp.server.auth.PermissionService;
import org.example.ftp.server.auth.db.SqliteFolderPermissionRepository;
import org.example.ftp.server.auth.db.SqliteFolderRepository;
import org.example.ftp.server.auth.db.SqliteSharedFolderRepository;
import org.example.ftp.server.auth.db.SqliteUserRepository;
import org.example.ftp.server.command.handler.CommandDispatcher;
import org.example.ftp.server.connection.ConnectionLimiter;
import org.example.ftp.server.auth.RateLimitResolver;
import org.example.ftp.server.auth.UserRateLimitService;
import org.example.ftp.server.session.memento.SessionMemento;
import org.example.ftp.server.stats.StatsService;
import org.example.ftp.server.transfer.RateLimiter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

public class FtpSession {

    private String pendingUsername;
    private String username;
    private boolean authenticated;

    private final AuthService authService;
    private final PermissionService permissionService;
    private final StatsService statsService;
    private final UserRateLimitService rateLimitService;
    
    private final SqliteUserRepository userRepository;
    private final SqliteFolderRepository folderRepository;
    private final SqliteFolderPermissionRepository folderPermissionRepository;
    private final SqliteSharedFolderRepository sharedFolderRepository;

    private final Path ftpRoot;
    private final Path sharedDirectory;

    private Path homeDirectory;
    private Path currentDirectory;
    private boolean hasExplicitlyChangedDirectory = false; 

    private ServerSocket passiveDataSocket;

    private SessionState state;
    private final PrintWriter writer;
    private final CommandDispatcher dispatcher = new CommandDispatcher();

    private final ConnectionLimiter connectionLimiter;
    private final RateLimiter globalUploadRateLimiter;
    private final RateLimiter globalDownloadRateLimiter;
    private RateLimiter uploadRateLimiter;
    private RateLimiter downloadRateLimiter;
    private boolean usesGlobalUploadLimit;
    private boolean usesGlobalDownloadLimit;

    private volatile boolean closeRequested;

    
    private volatile boolean transferAbortRequested;
    private volatile Socket activeDataConnection;
    private volatile Thread activeTransferThread;

    public FtpSession(
            PrintWriter writer,
            Path ftpRoot,
            AuthService authService,
            PermissionService permissionService,
            StatsService statsService,
            ConnectionLimiter connectionLimiter,
            RateLimiter globalUploadRateLimiter,
            RateLimiter globalDownloadRateLimiter,
            SqliteUserRepository userRepository,
            SqliteFolderRepository folderRepository,
            SqliteFolderPermissionRepository folderPermissionRepository,
            SqliteSharedFolderRepository sharedFolderRepository
    ) {
        this.writer = writer;
        this.ftpRoot = ftpRoot;
        this.authService = authService;
        this.permissionService = permissionService;
        this.statsService = statsService;
        this.rateLimitService = new UserRateLimitService(userRepository);
        this.connectionLimiter = connectionLimiter;
        this.globalUploadRateLimiter = globalUploadRateLimiter;
        this.globalDownloadRateLimiter = globalDownloadRateLimiter;
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
        this.folderPermissionRepository = folderPermissionRepository;
        this.sharedFolderRepository = sharedFolderRepository;

        this.state = new UnauthenticatedState();
        this.authenticated = false;

        try {
            this.sharedDirectory = ftpRoot.resolve("shared").normalize().toAbsolutePath();
            Files.createDirectories(this.sharedDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to init shared directory", e);
        }
    }

    public void handle(String commandLine) {
        FtpResponse response = dispatcher.dispatch(this, commandLine);
        writer.print(response.toProtocolString());
        writer.flush();
    }
    
    public void sendResponse(FtpResponse response) {
        writer.print(response.toProtocolString());
        writer.flush();
    }

    public void authenticate(String username) {
        this.username = username;
        this.pendingUsername = null;
        this.authenticated = true;
        this.state = new AuthenticatedState();

        try {
            initializeHomeDirectory(username);
            initializeRateLimiters(username);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize session directories", e);
        }
    }

    private void initializeHomeDirectory(String username) throws IOException {
        homeDirectory = ftpRoot.resolve("users").resolve(username).normalize().toAbsolutePath();
        Files.createDirectories(homeDirectory);
        currentDirectory = homeDirectory;
    }

    private void initializeRateLimiters(String username) {
        var limits = rateLimitService.getRateLimits(username);

        RateLimitResolver.RateLimitResult uploadResult = RateLimitResolver.resolve(
            globalUploadRateLimiter.getLimit(), limits.uploadSpeed(), limits.legacyRateLimit());
        RateLimitResolver.RateLimitResult downloadResult = RateLimitResolver.resolve(
            globalDownloadRateLimiter.getLimit(), limits.downloadSpeed(), limits.legacyRateLimit());

        uploadRateLimiter = new RateLimiter(uploadResult.limit());
        usesGlobalUploadLimit = uploadResult.isGlobal();
        
        downloadRateLimiter = new RateLimiter(downloadResult.limit());
        usesGlobalDownloadLimit = downloadResult.isGlobal();
    }

    
    public RateLimiter getEffectiveRateLimiter() {
        return getDownloadRateLimiter();
    }

    public RateLimiter getUploadRateLimiter() {
        if (uploadRateLimiter == null) return null;
        if (usesGlobalUploadLimit) {
            uploadRateLimiter.setLimit(globalUploadRateLimiter.getLimit());
        }
        return uploadRateLimiter;
    }

    public RateLimiter getDownloadRateLimiter() {
        if (downloadRateLimiter == null) return null;
        if (usesGlobalDownloadLimit) {
            downloadRateLimiter.setLimit(globalDownloadRateLimiter.getLimit());
        }
        return downloadRateLimiter;
    }

    public void requestClose() {
        this.closeRequested = true;
    }

    public void requestTransferAbort() {
        this.transferAbortRequested = true;

        
        Socket s = this.activeDataConnection;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }

        
        try {
            closePassiveDataSocket();
        } catch (IOException ignored) {}

        
        Thread t = this.activeTransferThread;
        if (t != null) {
            t.interrupt();
        }
    }

    public boolean isTransferAbortRequested() {
        return transferAbortRequested;
    }

    public void clearTransferAbort() {
        this.transferAbortRequested = false;
    }

    public void setActiveDataConnection(Socket socket) {
        this.activeDataConnection = socket;
    }

    public void clearActiveDataConnection(Socket socket) {
        if (this.activeDataConnection == socket) {
            this.activeDataConnection = null;
        }
    }

    public void setActiveTransferThread(Thread t) {
        this.activeTransferThread = t;
    }

    public void clearActiveTransferThread(Thread t) {
        if (this.activeTransferThread == t) {
            this.activeTransferThread = null;
        }
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    
    public boolean isAuthenticated() { return authenticated; }

    public String getUsername() { return username; }

    public void setPendingUsername(String u) { pendingUsername = u; }

    public String getPendingUsername() { return pendingUsername; }

    public AuthService getAuthService() { return authService; }

    public PermissionService getPermissionService() { return permissionService; }

    public StatsService getStatsService() { return statsService; }

    public ConnectionLimiter getConnectionLimiter() { return connectionLimiter; }

    public Path getSharedDirectory() { return sharedDirectory; }
    
    public Path getFtpRoot() { return ftpRoot; }

    public Path getHomeDirectory() { return homeDirectory; }

    public Path getCurrentDirectory() { return currentDirectory; }

    public void setCurrentDirectory(Path p) { currentDirectory = p; }
    
    public void markDirectoryChanged() { hasExplicitlyChangedDirectory = true; }
    
    public void resetDirectoryChangeFlag() { hasExplicitlyChangedDirectory = false; }
    
    public boolean hasExplicitlyChangedDirectory() { return hasExplicitlyChangedDirectory; }

    public ServerSocket getPassiveDataSocket() { return passiveDataSocket; }

    public void openPassiveDataSocket() throws IOException {
        
        try {
            closePassiveDataSocket();
        } catch (IOException ignored) {}

        passiveDataSocket = new ServerSocket(0);
        passiveDataSocket.setReuseAddress(true);
        
        try {
            passiveDataSocket.setSoTimeout(Integer.getInteger("ftp.data.timeoutMs", 15000));
        } catch (Exception ignored) {}
    }

    public void closePassiveDataSocket() throws IOException {
        if (passiveDataSocket != null) {
            passiveDataSocket.close();
            passiveDataSocket = null;
        }
    }

    public SessionState getState() { return state; }

    public void setState(SessionState s) { state = s; }

    public SqliteUserRepository getUserRepository() { return userRepository; }
    public SqliteFolderRepository getFolderRepository() { return folderRepository; }
    public SqliteFolderPermissionRepository getFolderPermissionRepository() { return folderPermissionRepository; }
    public SqliteSharedFolderRepository getSharedFolderRepository() { return sharedFolderRepository; }

    
    public SessionMemento save() {
        return new SessionMemento(
                state,
                pendingUsername,
                username,
                authenticated,
                currentDirectory,
                homeDirectory
        );
    }

    public void restore(SessionMemento m) {
        state = m.getState();
        pendingUsername = m.getPendingUsername();
        username = m.getUsername();
        authenticated = m.isAuthenticated();
        currentDirectory = m.getCurrentDirectory();
        homeDirectory = m.getHomeDirectory();
    }
}