package com.example.demo.service;

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

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository repo;
    private final NoticeReadRepository readRepo;

    @Transactional(readOnly = true)
    public Page<Notice> list(String q, String role, NoticeType type, Pageable pageable) {
        TargetRole t = TargetRole.from(role); // ✅ 안전 변환 (ROLE_ 프리픽스 포함 허용)
        return repo.findForClient(q, type, t, pageable);
    }

    @Transactional
    public Notice findAndIncrease(Long id, boolean inc) {
        Notice n = repo.findById(id).orElseThrow();
        if (inc) n.setViewCount(n.getViewCount() + 1);
        return n;
    }

    /** 사용자별 읽음 처리 */
    @Transactional
    public void markRead(String userId, Long noticeId) {
        // ✅ userId null 방지
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

    /** 사용자별 미확인 개수 */
    @Transactional(readOnly = true)
    public long unreadCount(String userId, String role) {
        if (userId == null || userId.isBlank()) return 0L; // ✅ 방어 코드
        TargetRole t = TargetRole.from(role);
        return repo.countUnreadForUser(userId, t);
    }
}
