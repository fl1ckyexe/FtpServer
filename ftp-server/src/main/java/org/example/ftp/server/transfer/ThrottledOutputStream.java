package org.example.ftp.server.transfer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ThrottledOutputStream extends FilterOutputStream {

    private final RateLimiter limiter;

    public ThrottledOutputStream(OutputStream out, RateLimiter limiter) {
        super(out);
        this.limiter = limiter;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        limiter.acquire(len);
        super.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        limiter.acquire(1);
        super.write(b);
    }
}
