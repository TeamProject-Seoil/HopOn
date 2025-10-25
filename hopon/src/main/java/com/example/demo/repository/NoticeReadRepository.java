package com.example.demo.repository;

import com.example.demo.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {
    boolean existsByUserIdAndNoticeId(String userId, Long noticeId);
}
