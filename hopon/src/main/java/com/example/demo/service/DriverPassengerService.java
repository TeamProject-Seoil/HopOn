// src/main/java/com/example/demo/service/DriverPassengerService.java
package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.support.AuthUserResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverPassengerService {

    private final AuthUserResolver authUserResolver;
    private final DriverOperationRepository driverOperationRepository;
    private final ReservationRepository reservationRepository;

    private static final Set<ReservationStatus> ACTIVE =
            EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.BOARDED);

    @Transactional
    public DriverPassengerListResponse listActivePassengersNow(Authentication auth) {
        var user = authUserResolver.requireUser(auth);

        var op = driverOperationRepository
                .findFirstByUserNumAndStatus(user.getUserNum(), DriverOperationStatus.RUNNING)
                .orElse(null);

        if (op == null) {
            return DriverPassengerListResponse.builder()
                    .operationId(null).routeId(null).routeName(null)
                    .count(0).items(List.of()).build();
        }

        List<ReservationEntity> rows = List.of();

        // 1) apiVehId 우선
        if (op.getApiVehId() != null && !op.getApiVehId().isBlank()) {
            rows = reservationRepository.findActiveByApiVehId(op.getApiVehId(), ACTIVE);
        }
        // 2) 없으면 apiPlainNo
        if ((rows == null || rows.isEmpty())
                && op.getApiPlainNo() != null && !op.getApiPlainNo().isBlank()) {
            rows = reservationRepository.findActiveByApiPlainNo(op.getApiPlainNo(), ACTIVE);
        }
        // 3) 그래도 없으면 routeId로 전체
        if (rows == null || rows.isEmpty()) {
            rows = reservationRepository.findActiveByRoute(op.getRouteId(), ACTIVE);
        }

        var list = (rows == null ? List.<ReservationEntity>of() : rows)
                .stream().map(this::toDto).collect(Collectors.toList());

        return DriverPassengerListResponse.builder()
                .operationId(op.getId())
                .routeId(op.getRouteId())
                .routeName(op.getRouteName())
                .count(list.size())
                .items(list)
                .build();
    }

    private DriverPassengerDto toDto(ReservationEntity r) {
        var u = r.getUser();
        return DriverPassengerDto.builder()
                .reservationId(r.getId())
                .userNum(u != null ? u.getUserNum() : null)
                .username(u != null ? nz(u.getUsername()) : null)
                .userid(u != null ? nz(u.getUserid()) : null)
                // 엔티티(board*/dest*) → DTO(boarding*/alighting*) 매핑
                .boardingStopId(nz(r.getBoardStopId()))
                .boardingStopName(nz(r.getBoardStopName()))
                .alightingStopId(nz(r.getDestStopId()))
                .alightingStopName(nz(r.getDestStopName()))
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .createdAtIso(r.getRequestedAt() != null ? r.getRequestedAt().toInstant(ZoneOffset.UTC).toString() : null)
                .updatedAtIso(r.getUpdatedAt()  != null ? r.getUpdatedAt().toInstant(ZoneOffset.UTC).toString()  : null)
                .build();
    }

    private static String nz(String s){ return (s==null || s.isBlank()) ? null : s; }
}