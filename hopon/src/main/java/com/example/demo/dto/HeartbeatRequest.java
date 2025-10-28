package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 운행 중 주기적 위치 보고 */
@Getter @Setter
public class HeartbeatRequest {
    @NotNull private Double lat;
    @NotNull private Double lon;
}
