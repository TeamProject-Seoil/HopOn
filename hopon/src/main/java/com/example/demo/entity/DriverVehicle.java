package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "driver_vehicle", uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_vehicle_user", columnNames = {"user_num"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DriverVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.user_num */
    @Column(name = "user_num", nullable = false)
    private Long userNum;

    /** bus_vehicle.vehicle_id */
    @Column(name = "vehicle_id", length = 20, nullable = false)
    private String vehicleId;
}
