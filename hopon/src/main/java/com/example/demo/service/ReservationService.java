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
    private final ArrivalNowService arrivalNowService;
    
    /** 예약 생성 */
    @Transactional
    public ReservationEntity createReservation(UserEntity user, ReservationRequestDto dto) {
        if (reservationRepository.existsByUserAndStatus(user, ReservationStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_ACTIVE");
        }

        // 노선유형 계산
        var meta = arrivalNowService.resolveRouteType(dto.getRouteId()); // code, label

        var entity = reservationRepository.save(
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
                        .busRouteType(meta.code)          // ✅ 저장
                        .routeTypeName(meta.label)        // ✅ 저장
                        .build()
        );

        // 이후 매칭/운행 연결 로직은 기존 그대로…
        var match = busMatcher.pickBest(dto.getRouteId(), dto.getBoardArsId());
        if (match != null) {
            entity.setApiVehId(match.apiVehId());
            entity.setApiPlainNo(match.apiPlainNo());
            driverOperationRepository
                .findByRouteIdAndStatusOrderByUpdatedAtDesc(dto.getRouteId(), DriverOperationStatus.RUNNING)
                .stream()
                .filter(op -> (op.getApiVehId()!=null && op.getApiVehId().equals(match.apiVehId()))
                           || norm(op.getApiPlainNo()).equals(norm(match.apiPlainNo())))
                .findFirst()
                .ifPresent(op -> entity.setOperationId(op.getId()));
        }

        return reservationRepository.save(entity);
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
