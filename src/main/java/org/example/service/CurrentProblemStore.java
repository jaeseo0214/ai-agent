package org.example.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CurrentProblemStore {
    private static final ConcurrentMap<Long, UUID> map = new ConcurrentHashMap<>();

    public void put(Long userId, UUID problemId) {
        if (userId != null && problemId != null) map.put(userId, problemId);
    }
    public static UUID get(Long userId) {
        return userId == null ? null : map.get(userId);
    }
    public void clear(Long userId) { if (userId != null) map.remove(userId);}
}
