// src/main/java/com/example/demo/entity/DriverVehicleRegistration.java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_vehicle_registration",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dvr_user_vehicle", columnNames = {"user_num", "vehicle_id"})
        },
        indexes = {
                @Index(name = "idx_dvr_user_created", columnList = "user_num, created_at")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DriverVehicleRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.user_num */
    @Column(name = "user_num", nullable = false)
    private Long userNum;

    /** bus_vehicle.vehicle_id */
    @Column(name = "vehicle_id", length = 64, nullable = false)
    private String vehicleId;

    /** 차량번호(번호판) - 배정 시 스냅샷 */
    @Column(name = "plate_no", length = 20)
    private String plateNo;

    /** 노선명 - 배정 시 스냅샷 ★추가 */
    @Column(name = "route_name", length = 50)
    private String routeName;
    
    /** 노선유형 코드(예: 3=간선) - 배정 시 스냅샷 */
    @Column(name = "route_type_code")
    private Integer routeTypeCode;

    /** 노선유형 라벨(예: "간선") - 배정 시 스냅샷 */
    @Column(name = "route_type_label", length = 20)
    private String routeTypeLabel;

    /** 최초 등록 시각 (DB DEFAULT CURRENT_TIMESTAMP 사용) */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
