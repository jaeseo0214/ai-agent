package org.example.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CurrentProblemStore {
    private static final ConcurrentMap<String, UUID> map = new ConcurrentHashMap<>();

    public void put(String username, UUID problemId) {
        if (username != null && problemId != null) map.put(username, problemId);
    }
    public static UUID get(String username) {
        return username == null ? null : map.get(username);
    }
    public void clear(String username) {
        if (username != null) map.remove(username);
    }
}
