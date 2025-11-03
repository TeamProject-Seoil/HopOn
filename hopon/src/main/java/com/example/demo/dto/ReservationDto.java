package com.example.demo.dto;

import com.example.demo.entity.ReservationEntity;

public record ReservationDto(
        Long id,
        String routeId,
        String direction,
        String boardStopId,
        String boardStopName,
        String boardArsId,
        String destStopId,
        String destStopName,
        String destArsId,
        String status,
        String routeName,
        java.time.LocalDateTime updatedAt,
        Integer busRouteType,
        String routeTypeName,
        Boolean delayed,
        String boardingStage          // ✅ 추가
) {
    public static ReservationDto from(ReservationEntity e, boolean delayed) {
        return new ReservationDto(
                e.getId(),
                e.getRouteId(),
                e.getDirection(),
                e.getBoardStopId(),
                e.getBoardStopName(),
                e.getBoardArsId(),
                e.getDestStopId(),
                e.getDestStopName(),
                e.getDestArsId(),
                e.getStatus().name(),
                e.getRouteName(),
                e.getUpdatedAt(),
                e.getBusRouteType(),
                e.getRouteTypeName(),
                delayed,
                e.getBoardingStage() != null ? e.getBoardingStage().name() : null
        );
    }
}