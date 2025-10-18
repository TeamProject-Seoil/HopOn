// ReservationQueryService.java  (조회 전용 분리 추천)
package com.example.demo.service;

import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class ReservationQueryService {
    private final ReservationRepository reservationRepository;

//    @Transactional(readOnly = true)
//    public List<ReservationEntity> recentTop3(UserEntity user) {
//        return reservationRepository.findTop3ByUserOrderByUpdatedAtDesc(user);
//    }

    @Transactional(readOnly = true)
    public ReservationEntity getActiveReservationFromDb(Long userNum) {
        return reservationRepository
                .findTopByUser_UserNumAndStatusOrderByUpdatedAtDesc(userNum, ReservationStatus.CONFIRMED)
                .orElse(null);
    }
}
