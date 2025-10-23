// src/main/java/com/example/demo/repository/InquiryRepository.java
package com.example.demo.repository;

import com.example.demo.entity.Inquiry;
import com.example.demo.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /** 공개 목록: 전체(비로그인 포함). 검색/상태 필터 */
    @Query("""
        select i from Inquiry i
         where (:status is null or i.status = :status)
           and (:q is null or lower(i.title) like lower(concat('%', :q, '%')) )
    """)
    Page<Inquiry> findPublic(@Param("q") String q,
                             @Param("status") InquiryStatus status,
                             Pageable pageable);

    /** 내 목록(이메일 기준) */
    @Query("""
        select i from Inquiry i
         where i.email = :email
           and (:status is null or i.status = :status)
           and (:q is null or lower(i.title) like lower(concat('%', :q, '%')) )
    """)
    Page<Inquiry> findForClient(@Param("email") String email,
                                @Param("q") String q,
                                @Param("status") InquiryStatus status,
                                Pageable pageable);

    /** 소유권(이메일) 확인 포함 단건 조회 */
    Optional<Inquiry> findByIdAndEmail(Long id, String email);
}
