package com.example.demo.repository;

import com.example.demo.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 필요하다면 사용자별 예약 조회 메서드도 추가 가능
    // List<Reservation> findByUser(UserEntity user);
}