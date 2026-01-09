package org.example.ftp.server.transfer;

public class RateLimiter {


    private volatile long bytesPerSecond;


    private volatile long available;


    private volatile long lastCheck;

    public RateLimiter(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
        this.available = bytesPerSecond;
        this.lastCheck = System.nanoTime();
    }


    public void acquire(int bytes) {
        if (bytes <= 0) return;
        if (bytesPerSecond <= 0) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        
        int maxChunk = Math.min(bytes, 128);
        int remaining = bytes;
        
        while (remaining > 0) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            int chunk = Math.min(remaining, maxChunk);
            
            long bytesToWait;
            synchronized (this) {
                refill();
                
                if (available >= chunk) {
                    available -= chunk;
                    remaining -= chunk;
                    continue;
                }
                
                bytesToWait = chunk - available;
            }
            
            try {
                long nanosToWait = bytesToWait * 1_000_000_000L / bytesPerSecond;
                long millisToWait = nanosToWait / 1_000_000;
                
                if (millisToWait > 0) {
                    Thread.sleep(Math.min(millisToWait, 1));
                } else {
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastCheck;

        long refill =
                elapsed * bytesPerSecond / 1_000_000_000L;

        if (refill > 0) {
            available = Math.min(bytesPerSecond, available + refill);
            lastCheck = now;
        }
    }


    public synchronized void setLimit(Long rate) {
        if (rate == null || rate < 0) {
            return;
        }

        this.bytesPerSecond = rate;

        if (available > rate) {
            available = rate;
        }

        lastCheck = System.nanoTime();
    }

 
    public long getLimit() {
        return bytesPerSecond;
    }
}
