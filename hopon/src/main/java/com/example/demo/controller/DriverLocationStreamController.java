package com.example.demo.controller;

import com.example.demo.service.DriverLocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class DriverLocationStreamController {

    private final DriverLocationStreamService streamService;

    // 특정 운행의 위치를 SSE로 구독
    @GetMapping(path = "/driver/operations/{operationId}/location", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeOperation(@PathVariable Long operationId) {
        return streamService.subscribe(operationId);
    }
}
