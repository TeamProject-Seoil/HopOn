package com.example.demo.dto;

import com.example.demo.entity.ReservationEntity;

public record ReservationDto(
        Long id,
        String routeId,
        String direction,
        String boardStopId,     // ✅ 추가
        String boardStopName,
        String boardArsId,
        String destStopId,      // ✅ 추가
        String destStopName,
        String destArsId,
        String status,
        String routeName,
        java.time.LocalDateTime updatedAt,
        Integer busRouteType,
        String routeTypeName
) {
    public static ReservationDto from(ReservationEntity e) {
        return new ReservationDto(
                e.getId(),
                e.getRouteId(),
                e.getDirection(),
                e.getBoardStopId(),   // ✅ 매핑
                e.getBoardStopName(),
                e.getBoardArsId(),
                e.getDestStopId(),    // ✅ 매핑
                e.getDestStopName(),
                e.getDestArsId(),
                e.getStatus().name(),
                e.getRouteName(),
                e.getUpdatedAt(),
                e.getBusRouteType(), 
                e.getRouteTypeName()
        );
    }
}