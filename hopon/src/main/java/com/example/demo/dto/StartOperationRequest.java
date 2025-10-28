package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 운행 시작 */
@Getter @Setter
public class StartOperationRequest {
    /** 기사 현재 위치(GPS) */
    @NotNull private Double lat;
    @NotNull private Double lon;

    /** 선택: 명시적으로 vehicleId를 바꾸고 싶을 때 */
    private String vehicleId;
}
