package com.example.demo.service;

import com.example.demo.dto.DriverLocationDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class DriverLocationStreamService {

    // opId -> 구독자 목록
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> streams = new ConcurrentHashMap<>();

    // 구독 (SSE)
    public SseEmitter subscribe(Long operationId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis()); // 30분 타임아웃
        streams.computeIfAbsent(operationId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(operationId, emitter));
        emitter.onTimeout(() -> remove(operationId, emitter));
        emitter.onError(e -> remove(operationId, emitter));

        // 연결 직후 더미 이벤트로 핑
        try {
            emitter.send(SseEmitter.event().name("ping").data("ok"));
        } catch (IOException ignored) {}

        return emitter;
    }

    // 위치 업데이트 브로드캐스트
    public void publish(DriverLocationDto dto) {
        List<SseEmitter> list = streams.get(dto.getOperationId());
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("location").data(dto));
            } catch (IOException e) {
                emitter.complete();
            }
        }
    }

    private void remove(Long opId, SseEmitter emitter) {
        List<SseEmitter> list = streams.get(opId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) streams.remove(opId);
        }
    }
}
