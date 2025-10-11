package com.example.demo.service;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.UserEntity;
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

    // 예약 생성
    @Transactional
    public ReservationEntity createReservation(UserEntity user, ReservationRequestDto dto) {
        // 1. 중복 예약 검증
        boolean exists = reservationRepository.existsByUserAndStatus(user, ReservationStatus.CONFIRMED);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_ACTIVE");
        }

        // 2. Entity 생성 및 저장
        ReservationEntity entity = ReservationEntity.builder()
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
                .build();

        return reservationRepository.save(entity);
    }


    // 사용자별 예약 조회
    public List<ReservationEntity> getReservationsByUser(UserEntity user) {
        return reservationRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public ReservationEntity getActiveReservationFromDb(Long userNum) {
        return reservationRepository
                .findTopByUser_UserNumAndStatusOrderByUpdatedAtDesc(userNum, ReservationStatus.CONFIRMED)
                .orElse(null);
    }


    // 예약 전체 조회 (관리자용)
    public List<ReservationEntity> getAllReservations() {
        return reservationRepository.findAll();
    }


}