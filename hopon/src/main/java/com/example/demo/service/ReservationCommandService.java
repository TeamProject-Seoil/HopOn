package com.example.demo.service;

import com.example.demo.dto.CancelResult;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
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

    @Transactional
    public CancelResult cancel(Long reservationId, com.example.demo.entity.UserEntity user) { // ★ UserEntity
        int updated = reservationRepository.cancelIfOwnedAndCancellable(
                reservationId,
                user.getUserNum(),                     // ★ user_num 사용
                ReservationStatus.CANCELLED,
                CANCELLABLE
        );

        if (updated == 1) {
            return new CancelResult(reservationId, "CANCELLED");
        }

        // 실패 시 원인 파악
        ReservationEntity r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("RESERVATION_NOT_FOUND"));

        if (!r.getUser().getUserNum().equals(user.getUserNum())) {
            throw new SecurityException("NOT_OWNER");
        }

        return switch (r.getStatus()) {
            case CANCELLED -> new CancelResult(reservationId, "ALREADY_CANCELLED");
            case BOARDED, NOSHOW -> throw new IllegalStateException("CANNOT_CANCEL_AFTER_BOARDING");
            default -> throw new IllegalStateException("CANNOT_CANCEL_IN_THIS_STATUS");
        };
    }
}


