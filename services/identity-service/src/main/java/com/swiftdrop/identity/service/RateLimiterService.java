package com.swiftdrop.identity.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private final Map<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        LoginAttempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            return false;
        }

        if (attempt.isLocked()) {
            if (Instant.now().toEpochMilli() - attempt.getLastAttemptTime() > LOCK_DURATION_MS) {
                attemptsCache.remove(key);
                return false;
            }
            return true;
        }

        return false;
    }

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        attemptsCache.compute(key, (k, attempt) -> {
            if (attempt == null) {
                return new LoginAttempt(1, Instant.now().toEpochMilli(), false);
            }
            int newCount = attempt.getAttempts() + 1;
            boolean locked = newCount >= MAX_ATTEMPTS;
            return new LoginAttempt(newCount, Instant.now().toEpochMilli(), locked);
        });
    }

    private static class LoginAttempt {
        private final int attempts;
        private final long lastAttemptTime;
        private final boolean locked;

        public LoginAttempt(int attempts, long lastAttemptTime, boolean locked) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
            this.locked = locked;
        }

        public int getAttempts() {
            return attempts;
        }

        public long getLastAttemptTime() {
            return lastAttemptTime;
        }

        public boolean isLocked() {
            return locked;
        }
    }
}
