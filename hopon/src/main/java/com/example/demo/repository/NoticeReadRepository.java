package com.example.demo.repository;

import com.example.demo.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    boolean existsByUserIdAndNoticeId(String userId, Long noticeId);

    // ✅ 한 번에 현재 페이지의 공지들(readAt 포함)을 가져오도록
    List<NoticeRead> findByUserIdAndNoticeIdIn(String userId, Collection<Long> noticeIds);
}
