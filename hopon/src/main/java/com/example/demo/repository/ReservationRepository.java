// src/main/java/com/example/demo/repository/ReservationRepository.java
package com.example.demo.repository;

import com.example.demo.entity.BoardingStage;
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

    // ===== 기존 메서드들 유지 =====
    boolean existsByUserAndStatus(UserEntity user, ReservationStatus status);
    boolean existsByUserAndStatusIn(UserEntity user, List<ReservationStatus> statuses);
    boolean existsByUserAndStatusIn(UserEntity user, Set<ReservationStatus> statuses);

    List<ReservationEntity> findByUser(UserEntity user);
    List<ReservationEntity> findByUserAndStatus(UserEntity user, ReservationStatus status);

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

    Optional<ReservationEntity> findTopByUser_UserNumAndStatusOrderByUpdatedAtDesc(
            Long userNum, ReservationStatus status);

    List<ReservationEntity> findByUserOrderByUpdatedAtDesc(UserEntity user);

    // ===== 여기부터 운행 중 승객 조회용 추가 =====
    @Query("""
        select r from ReservationEntity r
         where r.apiVehId = :apiVehId
           and r.status in :statuses
         order by r.updatedAt desc
    """)
    List<ReservationEntity> findActiveByApiVehId(@Param("apiVehId") String apiVehId,
                                                 @Param("statuses") Set<ReservationStatus> statuses);

    @Query("""
        select r from ReservationEntity r
         where r.apiPlainNo = :apiPlainNo
           and r.status in :statuses
         order by r.updatedAt desc
    """)
    List<ReservationEntity> findActiveByApiPlainNo(@Param("apiPlainNo") String apiPlainNo,
                                                   @Param("statuses") Set<ReservationStatus> statuses);

    @Query("""
        select r from ReservationEntity r
         where r.routeId = :routeId
           and r.status in :statuses
         order by r.updatedAt desc
    """)
    List<ReservationEntity> findActiveByRoute(@Param("routeId") String routeId,
                                              @Param("statuses") Set<ReservationStatus> statuses);

 // ReservationRepository.java
    @Query("""
           select r
           from ReservationEntity r
           where r.operationId = :opId
             and r.status = :status
             and r.boardingStage = :stage
           """)
    List<ReservationEntity> findActiveNoshowByOperation(
            @Param("opId") Long operationId,
            @Param("status") ReservationStatus status,
            @Param("stage") BoardingStage stage
    );

}
