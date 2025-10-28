package com.example.demo.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AssignVehicleResponse {
    private String vehicleId;
    private String plateNo;
    private String routeId;
    private String routeName;
}
