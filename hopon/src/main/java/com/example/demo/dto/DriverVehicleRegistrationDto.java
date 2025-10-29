// src/main/java/com/example/demo/dto/DriverVehicleRegistrationDto.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DriverVehicleRegistrationDto {
    private String vehicleId;
    private String plateNo;
    private String routeId;
    private String routeName;
    private String createdAtIso; // YYYY-MM-DDTHH:mm:ssZ
    
    // ▼ 추가: 리스트 카드에서 바로 쓰는 필드
    private Integer routeTypeCode;   // 예: 3
    private String  routeTypeLabel;  // 예: "간선"
}
