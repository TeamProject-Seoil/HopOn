package com.example.demo.service;

import com.example.demo.dto.NoticeDtos;
import com.example.demo.entity.Notice;
import com.example.demo.entity.NoticeRead;
import com.example.demo.entity.NoticeType;
import com.example.demo.entity.TargetRole;
import com.example.demo.repository.NoticeReadRepository;
import com.example.demo.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository repo;
    private final NoticeReadRepository readRepo;

    @Transactional(readOnly = true)
    public Page<Notice> list(String q, String role, NoticeType type, Pageable pageable) {
        TargetRole t = TargetRole.from(role);
        return repo.findForClient(q, type, t, pageable);
    }

    /** ✅ 읽음 정보 포함 목록 */
    @Transactional(readOnly = true)
    public Page<NoticeDtos.Resp> listWithRead(String q, String role, String userId, NoticeType type, Pageable pageable) {
        TargetRole t = TargetRole.from(role);
        Page<Notice> page = repo.findForClient(q, type, t, pageable);

        // 비로그인(또는 userId 없음)일 땐 그냥 기본 DTO 반환
        if (userId == null || userId.isBlank()) {
            return page.map(NoticeDtos.Resp::from);
        }

        // 현재 페이지 공지 id 수집
        List<Long> ids = page.getContent().stream()
                .map(Notice::getId)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return page.map(NoticeDtos.Resp::from);
        }

        // ✅ NoticeRead -> notice.id 기준으로 readAt 맵 구성
        List<NoticeRead> reads = readRepo.findByUserIdAndNoticeIdIn(userId, ids);
        Map<Long, LocalDateTime> readAtMap = reads.stream()
                .filter(nr -> nr.getNotice() != null && nr.getNotice().getId() != null)
                .collect(Collectors.toMap(
                        nr -> nr.getNotice().getId(),
                        NoticeRead::getReadAt,
                        (a, b) -> a
                ));

        // 읽음 정보 포함 DTO 매핑
        return page.map(n -> NoticeDtos.Resp.from(n, readAtMap.get(n.getId())));
    }

    @Transactional
    public Notice findAndIncrease(Long id, boolean inc) {
        Notice n = repo.findById(id).orElseThrow();
        if (inc) n.setViewCount(n.getViewCount() + 1);
        return n;
    }

    @Transactional
    public void markRead(String userId, Long noticeId) {
        if (userId == null || userId.isBlank()) return;
        if (!readRepo.existsByUserIdAndNoticeId(userId, noticeId)) {
            Notice n = repo.findById(noticeId).orElseThrow();
            NoticeRead r = NoticeRead.builder()
                    .userId(userId)
                    .notice(n)
                    .build();
            readRepo.save(r);
        }
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId, String role) {
        if (userId == null || userId.isBlank()) return 0L;
        TargetRole t = TargetRole.from(role);
        return repo.countUnreadForUser(userId, t);
    }
}
