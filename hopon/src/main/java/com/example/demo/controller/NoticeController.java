package com.example.demo.controller;

import com.example.demo.dto.NoticeDtos;
import com.example.demo.entity.NoticeType;
import com.example.demo.service.NoticeService;
import com.example.demo.support.AppIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService service;
    private final AppIdentityResolver resolver;

    @GetMapping
    public Page<NoticeDtos.Resp> list(HttpServletRequest req,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(defaultValue = "updatedAt,desc") String sort,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) NoticeType type) {
        var id = resolver.resolve(req);
        Pageable pageable = PageRequest.of(page, size, Sort.by(parse(sort)));
        return service.list(q, id.role(), type, pageable).map(NoticeDtos.Resp::from);
    }

    /** 상세 조회 + 조회수 증가 + (선택)읽음처리 */
    @GetMapping("/{id}")
    public NoticeDtos.Resp detail(HttpServletRequest req,
                                  @PathVariable Long id,
                                  @RequestParam(defaultValue = "true") boolean increase,
                                  @RequestParam(defaultValue = "true") boolean markRead) {
        var who = resolver.resolve(req);
        var n = service.findAndIncrease(id, increase);
        if (markRead && who.userId() != null) { // ✅ null 가드 추가
            service.markRead(who.userId(), id);
        }
        return NoticeDtos.Resp.from(n);
    }

    /** 미확인 개수 배지용 */
    @GetMapping("/unread-count")
    public UnreadCountResp unreadCount(HttpServletRequest req) {
        var who = resolver.resolve(req);
        long cnt = service.unreadCount(who.userId(), who.role());
        return new UnreadCountResp(cnt);
    }

    /** 읽음 처리 API(리스트에서 스와이프 읽음 등 쓸 때) */
    @PostMapping("/{id}/read")
    public void markRead(HttpServletRequest req, @PathVariable Long id) {
        var who = resolver.resolve(req);
        if (who.userId() != null) { // ✅ null 가드 추가
            service.markRead(who.userId(), id);
        }
    }

    private Sort.Order parse(String s) {
        String[] arr = s.split(",");
        return new Sort.Order(
                (arr.length > 1 && "asc".equalsIgnoreCase(arr[1]))
                        ? Sort.Direction.ASC : Sort.Direction.DESC,
                arr[0]
        );
    }

    public record UnreadCountResp(long count) {}
}
