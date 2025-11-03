package com.example.demo.service;

import com.example.demo.dto.CancelResult;
import com.example.demo.entity.BoardingStage;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;

    private static final java.util.EnumSet<ReservationStatus> CANCELLABLE =
            java.util.EnumSet.of(ReservationStatus.CONFIRMED);

 // ReservationCommandService.java
    @Transactional
    public CancelResult cancel(Long reservationId, UserEntity user) {

        int updated = reservationRepository.cancelIfOwnedAndCancellable(
                reservationId,
                user.getUserNum(),
                ReservationStatus.CANCELLED,
                CANCELLABLE   // { CONFIRMED } 그대로 사용
        );

        if (updated == 1) {
            return new CancelResult(reservationId, "CANCELLED");
        }

        ReservationEntity r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("RESERVATION_NOT_FOUND"));

        if (!r.getUser().getUserNum().equals(user.getUserNum())) {
            throw new SecurityException("NOT_OWNER");
        }

        // ✅ 이제는 BoardingStage 기준으로 예외 처리
        if (r.getBoardingStage() == BoardingStage.BOARDED || r.getBoardingStage() == BoardingStage.ALIGHTED) {
            throw new IllegalStateException("CANNOT_CANCEL_AFTER_BOARDING");
        }

        if (r.getStatus() == ReservationStatus.CANCELLED) {
            return new CancelResult(reservationId, "ALREADY_CANCELLED");
        }

        throw new IllegalStateException("CANNOT_CANCEL_IN_THIS_STATUS");
    }

}


