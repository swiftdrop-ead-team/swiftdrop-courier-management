package com.swiftdrop.tracking.service;

import com.swiftdrop.tracking.dto.TrackingResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.*;

@Service
public class TrackingStreamService {
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String trackingNumber, TrackingResponse initial) {
        SseEmitter emitter = new SseEmitter(30 * 60_000L);
        Runnable remove = () -> remove(trackingNumber, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(initial));
            emitters.computeIfAbsent(trackingNumber, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
            return emitter;
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            throw new IllegalStateException("Unable to initialize tracking stream", exception);
        }
    }

    public void broadcast(String trackingNumber, TrackingResponse response) {
        CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(trackingNumber);
        if (subscribers == null) {
            return;
        }
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().name("status").data(response));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                remove(trackingNumber, emitter);
            }
        }
    }

    @Scheduled(fixedDelay = 25_000L)
    public void heartbeat() {
        emitters.forEach((trackingNumber, subscribers) -> subscribers.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                remove(trackingNumber, emitter);
            }
        }));
    }

    private void remove(String trackingNumber, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(trackingNumber);
        if (subscribers != null) {
            subscribers.remove(emitter);
            if (subscribers.isEmpty()) {
                emitters.remove(trackingNumber, subscribers);
            }
        }
    }
}
