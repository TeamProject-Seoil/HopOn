package com.example.demo.repository;

import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.ReservationEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    // 필요하다면 사용자별 예약 조회 메서드도 추가 가능
    // List<Reservation> findByUser(UserEntity user);

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
                                    @Param("cancellable") java.util.Set<ReservationStatus> cancellable);
}