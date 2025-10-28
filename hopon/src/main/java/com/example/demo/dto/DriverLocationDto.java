package com.example.demo.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DriverLocationDto {
    private Long operationId;
    private Double lat;
    private Double lon;
    private String updatedAtIso; // ISO-8601 (UTC Z)
    private boolean stale;       // 하트비트 지연 시 true
}
