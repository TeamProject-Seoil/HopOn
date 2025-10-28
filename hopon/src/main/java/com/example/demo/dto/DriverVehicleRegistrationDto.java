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
}
