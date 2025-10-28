package com.example.demo.controller;

import com.example.demo.service.DriverLocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 필요하면 특정 오리진만 열어주세요.
// @CrossOrigin(origins = {"http://localhost:5173", "https://your-admin.app"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class DriverLocationStreamController {

    private final DriverLocationStreamService streamService;

    // 특정 운행의 위치를 SSE로 구독
    @GetMapping(
        path = "/driver/operations/{operationId}/location",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public ResponseEntity<SseEmitter> subscribeOperation(@PathVariable Long operationId) {
        SseEmitter emitter = streamService.subscribe(operationId);
        return ResponseEntity.ok()
            .header("Cache-Control", "no-cache")     // 캐시 금지
            .header("X-Accel-Buffering", "no")       // nginx 버퍼링 끄기
            .body(emitter);
    }
}
