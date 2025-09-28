package com.example.demo.service;

import com.example.demo.dto.ReservationRequestDto;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    // 예약 생성
    @Transactional
    public Reservation createReservation(String userid, ReservationRequestDto reservationRequestDto) {
        UserEntity user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new IllegalStateException("USER_NOT_FOUND"));

        // 2) 예약 생성
        Reservation reservation = Reservation.builder()
                .user(user) // ★ FK 채움
                .routeId(reservationRequestDto.getRouteId())
                .direction(reservationRequestDto.getDirection())
                .boardStopId(reservationRequestDto.getBoardStopId())
                .boardStopName(reservationRequestDto.getBoardStopName())
                .boardArsId(reservationRequestDto.getBoardArsId())
                .destStopId(reservationRequestDto.getDestStopId())
                .destStopName(reservationRequestDto.getDestStopName())
                .destArsId(reservationRequestDto.getDestArsId())
                .build();

        return reservationRepository.save(reservation);
    }

    // 사용자별 예약 조회
    public List<Reservation> getReservationsByUser(UserEntity user) {
        return reservationRepository.findAll()
                .stream()
                .filter(r -> r.getUser().getUserNum().equals(user.getUserNum()))
                .toList();
    }

    // 예약 전체 조회 (관리자용)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }
}