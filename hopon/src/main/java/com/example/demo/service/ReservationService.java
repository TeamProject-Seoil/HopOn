package com.example.demo.service;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.DriverOperationStatus;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.DriverOperationRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ReservationBusMatcher busMatcher;
    private final DriverOperationRepository driverOperationRepository;

    /** 예약 생성 */
    @Transactional
    public ReservationEntity createReservation(UserEntity user, ReservationRequestDto dto) {
        // 1) 중복 예약 검증
        boolean exists = reservationRepository.existsByUserAndStatus(user, ReservationStatus.CONFIRMED);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_ACTIVE");
        }

     // 2) 기본 예약 저장
        ReservationEntity entity = reservationRepository.save(
                ReservationEntity.builder()
                        .user(user)
                        .routeId(dto.getRouteId())
                        .direction(dto.getDirection())
                        .boardStopId(dto.getBoardStopId())
                        .boardStopName(dto.getBoardStopName())
                        .boardArsId(dto.getBoardArsId())
                        .destStopId(dto.getDestStopId())
                        .destStopName(dto.getDestStopName())
                        .destArsId(dto.getDestArsId())
                        .status(ReservationStatus.CONFIRMED)
                        .routeName(dto.getRouteName())
                        .build()
        );

        // 3) 가장 먼저 도착할 버스 매칭
        var match = busMatcher.pickBest(dto.getRouteId(), dto.getBoardArsId());
        if (match != null) {
            entity.setApiVehId(match.apiVehId());
            entity.setApiPlainNo(match.apiPlainNo());

            // 4) 운행 연결(있으면)
            driverOperationRepository
                    .findByRouteIdAndStatusOrderByUpdatedAtDesc(dto.getRouteId(), DriverOperationStatus.RUNNING)
                    .stream()
                    .filter(op ->
                            equalsIgnoreNull(op.getApiVehId(), match.apiVehId()) ||
                            norm(op.getApiPlainNo()).equals(norm(match.apiPlainNo()))
                    )
                    .findFirst()
                    .ifPresent(op -> entity.setOperationId(op.getId()));
        }

        // 5) 최종 저장 (재할당 금지!)
        reservationRepository.save(entity);
        return entity;
    }

    private String norm(String s) {
        return s == null ? "" : s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }

    private boolean equalsIgnoreNull(String a, String b) {
        return a != null && a.equals(b);
    }

    /** 사용자별 예약 조회 */
    public List<ReservationEntity> getReservationsByUser(UserEntity user) {
        return reservationRepository.findByUser(user);
    }

    /** 활성 예약 1건(DB) */
    @Transactional(readOnly = true)
    public ReservationEntity getActiveReservationFromDb(Long userNum) {
        return reservationRepository
                .findTopByUser_UserNumAndStatusOrderByUpdatedAtDesc(userNum, ReservationStatus.CONFIRMED)
                .orElse(null);
    }

    /** 전체(내 예약) 최신순 */
    @Transactional(readOnly = true)
    public List<ReservationEntity> getAll(UserEntity user) {
        return reservationRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    /** (관리자) 전체 예약 */
    public List<ReservationEntity> getAllReservations() {
        return reservationRepository.findAll();
    }
}
