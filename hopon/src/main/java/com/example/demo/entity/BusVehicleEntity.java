package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bus_vehicle")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BusVehicleEntity {

    @Id
    @Column(name = "vehicle_id", length = 20)
    private String vehicleId;

    @Column(name = "route_id", length = 20, nullable = false)
    private String routeId;

    @Column(name = "route_name", length = 50, nullable = false)
    private String routeName;

    @Column(name = "plate_no", length = 20, nullable = false, unique = true)
    private String plateNo;
}
