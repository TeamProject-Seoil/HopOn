package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 기사-차량 배정(등록) 요청 */
@Getter @Setter
public class AssignVehicleRequest {
    /** vehicleId 또는 plateNo 중 하나는 필수 */
    private String vehicleId;
    private String plateNo;

    @NotBlank
    private String clientType; // DRIVER_APP 등(토큰 aud와 일치하도록)
}
