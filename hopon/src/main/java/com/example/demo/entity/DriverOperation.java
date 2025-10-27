package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_operation",
        indexes = {
                @Index(name = "ix_driver_operation_user", columnList = "user_num, status")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DriverOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.user_num */
    @Column(name = "user_num", nullable = false)
    private Long userNum;

    /** bus_vehicle.vehicle_id */
    @Column(name = "vehicle_id", length = 20, nullable = false)
    private String vehicleId;

    /** HopOn 노선 */
    @Column(name = "route_id", length = 20, nullable = false)
    private String routeId;

    @Column(name = "route_name", length = 50, nullable = false)
    private String routeName;

    /** 공공API 상 차량 식별(plainNo/vehId 매칭 결과) */
    @Column(name = "api_veh_id", length = 32)
    private String apiVehId;

    @Column(name = "api_plain_no", length = 32)
    private String apiPlainNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private DriverOperationStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** 기사 현재 위치 마지막 보고 */
    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lon")
    private Double lastLon;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
