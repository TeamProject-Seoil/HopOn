package com.example.demo.dto;

import com.example.demo.entity.ReservationEntity;

public record ReservationDto(
        Long id,
        String routeId,
        String direction,
        String boardStopName,
        String boardArsId,
        String destStopName,
        String destArsId,
        String status,
        String routeName,
        java.time.LocalDateTime updatedAt
) {
    public static ReservationDto from(ReservationEntity e) {
        return new ReservationDto(
                e.getId(), e.getRouteId(), e.getDirection(),
                e.getBoardStopName(), e.getBoardArsId(),
                e.getDestStopName(), e.getDestArsId(),
                e.getStatus().name(),e.getRouteName(),e.getUpdatedAt()
        );
    }
}
