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

        // ✅ 로그인 유저면 읽음 포함, 아니면 기본
        if (id.userId() != null && !id.userId().isBlank()) {
            return service.listWithRead(q, id.role(), id.userId(), type, pageable);
        } else {
            return service.list(q, id.role(), type, pageable).map(NoticeDtos.Resp::from);
        }
    }

    /** 상세 조회 + 조회수 증가 + (선택)읽음처리 */
    @GetMapping("/{id}")
    public NoticeDtos.Resp detail(HttpServletRequest req,
                                  @PathVariable Long id,
                                  @RequestParam(defaultValue = "true") boolean increase,
                                  @RequestParam(defaultValue = "true") boolean markRead) {
        var who = resolver.resolve(req);
        var n = service.findAndIncrease(id, increase);
        if (markRead && who.userId() != null) {
            service.markRead(who.userId(), id);
        }
        // 상세에서도 읽음 여부 포함해서 내려주고 싶다면 readAt 조회해 합쳐도 됨(선택)
        return NoticeDtos.Resp.from(n /*, readAt */);
    }

    /** 미확인 개수 배지용 */
    @GetMapping("/unread-count")
    public UnreadCountResp unreadCount(HttpServletRequest req) {
        var who = resolver.resolve(req);
        long cnt = service.unreadCount(who.userId(), who.role());
        return new UnreadCountResp(cnt);
    }

    /** 읽음 처리 API */
    @PostMapping("/{id}/read")
    public void markRead(HttpServletRequest req, @PathVariable Long id) {
        var who = resolver.resolve(req);
        if (who.userId() != null) {
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
