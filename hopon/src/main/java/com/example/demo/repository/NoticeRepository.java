package com.example.demo.repository;

import com.example.demo.entity.Notice;
import com.example.demo.entity.NoticeType;
import com.example.demo.entity.TargetRole;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("""
        select n from Notice n
        where (:type is null or n.noticeType=:type)
          and (:target='ALL' or n.targetRole='ALL' or n.targetRole=:target)
          and (:q is null or lower(n.title) like lower(concat('%',:q,'%')))
    """)
    Page<Notice> findForClient(@Param("q") String q,
                               @Param("type") NoticeType type,
                               @Param("target") TargetRole target,
                               Pageable pageable);
}
