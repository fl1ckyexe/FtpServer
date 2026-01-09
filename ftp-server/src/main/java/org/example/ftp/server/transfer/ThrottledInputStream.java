package org.example.ftp.server.transfer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.example.ftp.server.util.DebugLog;

public class ThrottledInputStream extends FilterInputStream {

    private final RateLimiter limiter;

    public ThrottledInputStream(InputStream in, RateLimiter limiter) {
        super(in);
        this.limiter = limiter;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        
        if (bytesRead > 0) {
            long acquireStart = System.nanoTime();
            limiter.acquire(bytesRead);
            long acquireEnd = System.nanoTime();
            
            long acquireTime = (acquireEnd - acquireStart) / 1_000_000;
            if (acquireTime > 10) {
                DebugLog.d("[ThrottledInputStream] read(" + len + ") -> " + bytesRead + " bytes, acquire took " + acquireTime + "ms");
            }
        }
        
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        limiter.acquire(1);
        return super.read();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        long transferred = 0;
        byte[] buffer = new byte[8192];
        int read;
        while ((read = this.read(buffer, 0, buffer.length)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }
}
