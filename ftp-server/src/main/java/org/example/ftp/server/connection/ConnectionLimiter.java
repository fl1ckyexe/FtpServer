package org.example.ftp.server.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionLimiter {

    private final AtomicInteger globalLimit;

    private final AtomicInteger totalConnections = new AtomicInteger(0);

    private final Map<String, AtomicInteger> perUser =
            new ConcurrentHashMap<>();

    public ConnectionLimiter(int globalLimit) {
        this.globalLimit = new AtomicInteger(globalLimit);
    }


    public boolean tryAcquire(String username) {

        if (totalConnections.incrementAndGet() > globalLimit.get()) {
            totalConnections.decrementAndGet();
            return false;
        }

        perUser
                .computeIfAbsent(username, u -> new AtomicInteger(0))
                .incrementAndGet();

        return true;
    }

    public void release(String username) {

        totalConnections.decrementAndGet();

        AtomicInteger cnt = perUser.get(username);
        if (cnt != null && cnt.decrementAndGet() <= 0) {
            perUser.remove(username);
        }
    }


    public void setMaxConnections(Integer maxConn) {
        if (maxConn == null || maxConn < 1) {
            return;
        }
        globalLimit.set(maxConn);
    }


    public int getMaxConnections() {
        return globalLimit.get();
    }


    public int getCurrentConnections() {
        return totalConnections.get();
    }

    
    public int getConnectedUsersCount() {
        return perUser.size();
    }

    
    public int getUserConnections(String username) {
        AtomicInteger cnt = perUser.get(username);
        return cnt == null ? 0 : cnt.get();
    }

    
    public Map<String, Integer> snapshotPerUserConnections() {
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> e : perUser.entrySet()) {
            out.put(e.getKey(), e.getValue().get());
        }
        return out;
    }
}
