// src/main/java/com/example/demo/service/ReservationBoardingService.java
package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReservationBoardingService {

    private final ReservationRepository reservationRepository;

    private ReservationEntity getOwned(Long id, UserEntity user) {
        ReservationEntity r = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND"));
        if (!r.getUser().getUserNum().equals(user.getUserNum())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_OWNER");
        }
        return r;
    }

    /** 탑승 확인(유저가 버튼 클릭) */
    @Transactional
    public void confirmBoard(Long id, UserEntity user) {
        ReservationEntity r = getOwned(id, user);
        if (r.getStatus() != ReservationStatus.CONFIRMED) return;
        r.setBoardingStage(BoardingStage.BOARDED);
        reservationRepository.save(r);
    }

    /** 탑승 타임아웃(일정 시간 지나도 버튼 X) → 예약 취소 */
    @Transactional
    public void boardTimeout(Long id, UserEntity user) {
        ReservationEntity r = getOwned(id, user);
        if (r.getStatus() != ReservationStatus.CONFIRMED) return;
        if (r.getBoardingStage() == BoardingStage.NOSHOW) {
            r.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(r);
        }
    }

    /** 하차 확인(유저가 버튼 클릭) → ALIGHTED + COMPLETED */
    @Transactional
    public void confirmAlight(Long id, UserEntity user) {
        ReservationEntity r = getOwned(id, user);
        if (r.getStatus() != ReservationStatus.CONFIRMED) return;

        r.setBoardingStage(BoardingStage.ALIGHTED);
        r.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(r);
    }

    /** 하차 타임아웃 → 자동 ALIGHTED + COMPLETED */
    @Transactional
    public void alightTimeout(Long id, UserEntity user) {
        ReservationEntity r = getOwned(id, user);
        if (r.getStatus() == ReservationStatus.COMPLETED) return;

        r.setBoardingStage(BoardingStage.ALIGHTED);
        r.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(r);
    }
}
