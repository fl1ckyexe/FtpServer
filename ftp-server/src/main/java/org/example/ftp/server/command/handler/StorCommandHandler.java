package org.example.ftp.server.command.handler;

import org.example.ftp.common.protocol.FtpResponse;
import org.example.ftp.common.protocol.Responses;
import org.example.ftp.server.auth.Permission;
import org.example.ftp.server.fs.AccessControl;
import org.example.ftp.server.fs.PathResolver;
import org.example.ftp.server.session.FtpSession;
import org.example.ftp.server.transfer.RateLimiter;
import org.example.ftp.server.transfer.ThrottledInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StorCommandHandler extends AbstractCommandHandler {

    @Override
    public String getCommandName() {
        return "STOR";
    }

    @Override
    protected boolean checkState(FtpSession session) {
        return super.checkState(session);
    }

    @Override
    protected FtpResponse execute(FtpSession session, String argument) {
        if (session.getPassiveDataSocket() == null) {
            return Responses.usePasvFirst();
        }

        if (argument == null || argument.isBlank()) {
            return Responses.missingFileName();
        }

        Path target;
        try {
            target = PathResolver.resolve(session, argument);
        } catch (SecurityException e) {
            return Responses.accessDenied();
        }

        if (!AccessControl.can(session, target, Permission.WRITE)) {
            return Responses.permissionDenied();
        }

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            
            session.sendResponse(Responses.ok(150, "Opening data connection."));
        } catch (IOException e) {
            return Responses.connectionClosedTransferAborted();
        }
        
        
        session.clearTransferAbort();
        session.setActiveTransferThread(Thread.currentThread());

        boolean transferCompleted = false;
        boolean wasAborted = false;
        boolean eofReceived = false; 
        OutputStream fileOutputStream = null;
        Socket activeDataConn = null;
        long bytes = 0;
        
        try (
                Socket dataConnection = session.getPassiveDataSocket().accept();
                InputStream rawIn = dataConnection.getInputStream();
                InputStream in = wrapInputWithLimiter(session, rawIn)
        ) {
            activeDataConn = dataConnection;
            session.setActiveDataConnection(dataConnection);

            
            fileOutputStream = Files.newOutputStream(
                    target,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            
            dataConnection.setSoTimeout(50); 
            
            
            byte[] buffer = new byte[512];
            int bytesRead;
            try {
                while (true) {
                    
                    if (session.isTransferAbortRequested()) {
                        wasAborted = true;
                        break;
                    }
                    
                    if (dataConnection.isClosed() || !dataConnection.isConnected()) {
                        wasAborted = true;
                        break;
                    }
                    
                    try {
                        bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            
                            eofReceived = true; 
                            if (session.isTransferAbortRequested()) {
                                wasAborted = true;
                            }
                            break;
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        
                        if (session.isTransferAbortRequested()) {
                            wasAborted = true;
                            break;
                        }
                        if (dataConnection.isClosed() || !dataConnection.isConnected()) {
                            wasAborted = true;
                            break;
                        }
                        
                        continue;
                    }
                    
                    fileOutputStream.write(buffer, 0, bytesRead);
                    bytes += bytesRead;
                }
            } catch (IOException e) {
                
                String msg = e.getMessage();
                if (session.isTransferAbortRequested()) {
                    wasAborted = true;
                } else if (dataConnection.isClosed() || !dataConnection.isConnected() || 
                    (msg != null && (msg.contains("closed") || msg.contains("reset") || msg.contains("Connection reset")))) {
                    wasAborted = true;
                } else {
                    throw e; 
                }
            }
            
            if (wasAborted) {
                
                transferCompleted = false;
            } else {
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                }
                transferCompleted = true; 
                session.getStatsService().onUpload(session.getUsername(), bytes);
            }
        } catch (SocketTimeoutException e) {
            
            wasAborted = true;
            transferCompleted = false;
        } catch (IOException e) {
            
            if (eofReceived && transferCompleted) {
                
                
            } else {
                
                String msg = e.getMessage();
                
                if (msg != null && (msg.contains("closed") || msg.contains("reset") || msg.contains("Connection reset") || msg.contains("Broken pipe"))) {
                    if (eofReceived) {
                        
                        
                    } else {
                        
                        wasAborted = true;
                        transferCompleted = false;
                    }
                } else {
                    wasAborted = true;
                    transferCompleted = false;
                }
            }
        } finally {
            
            if (activeDataConn != null) {
                session.clearActiveDataConnection(activeDataConn);
            }
            session.clearActiveTransferThread(Thread.currentThread());

            
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    
                }
            }
            
            
            try {
                session.closePassiveDataSocket();
            } catch (IOException ignored) {}
            
            
            if (!transferCompleted || wasAborted) {
                deletePartialFileAsync(target);
            }
        }

        if (wasAborted || !transferCompleted) {
            return Responses.connectionClosedTransferAborted();
        }

        return Responses.transferComplete();
    }

    private InputStream wrapInputWithLimiter(FtpSession session, InputStream in) {
        
        RateLimiter limiter = session.getUploadRateLimiter();
        if (limiter == null) {
            return in;
        }
        return new ThrottledInputStream(in, limiter);
    }

    private void deletePartialFileAsync(Path target) {
        
        Thread t = new Thread(() -> {
            for (int attempt = 1; attempt <= 10; attempt++) {
                try {
                    Files.deleteIfExists(target);
                    return;
                } catch (IOException e) {
                    if (attempt == 10) {
                        return;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "stor-delete-partial");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected FtpResponse notAllowed() {
        return Responses.needLogin();
    }
}