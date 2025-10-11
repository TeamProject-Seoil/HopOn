// src/main/java/com/example/demo/repository/ReservationRepository.java
package com.example.demo.repository;

import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    // ✅ 존재 여부(중복 체크) — 지금 서비스 코드에서 사용
    boolean existsByUserAndStatus(UserEntity user, ReservationStatus status);

    // (선택) 활성 상태가 여러 개인 경우에 대비
    boolean existsByUserAndStatusIn(UserEntity user, List<ReservationStatus> statuses);
    boolean existsByUserAndStatusIn(UserEntity user, Set<ReservationStatus> statuses);

    // (선택) 사용자별 조회 최적화
    List<ReservationEntity> findByUser(UserEntity user);
    List<ReservationEntity> findByUserAndStatus(UserEntity user, ReservationStatus status);

    // 이미 있던 취소용 커스텀 쿼리 유지
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      update ReservationEntity r
         set r.status = :cancelled
       where r.id = :id
         and r.user.userNum = :userNum
         and r.status in :cancellable
    """)
    int cancelIfOwnedAndCancellable(@Param("id") Long id,
                                    @Param("userNum") Long userNum,
                                    @Param("cancelled") ReservationStatus cancelled,
                                    @Param("cancellable") Set<ReservationStatus> cancellable);

    // ✅ 최신 1건(활성) 조회
    Optional<ReservationEntity> findTopByUser_UserNumAndStatusOrderByUpdatedAtDesc(
            Long userNum, ReservationStatus status);

    // ✅ 최근 3건 조회 (ReservationQueryService에서 사용)
    List<ReservationEntity> findTop3ByUserOrderByUpdatedAtDesc(UserEntity user);
}
