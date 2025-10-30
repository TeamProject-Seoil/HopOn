// src/main/java/com/example/demo/entity/DriverOperation.java
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

    @Column(name = "user_num", nullable = false)
    private Long userNum;

    @Column(name = "vehicle_id", length = 20, nullable = false)
    private String vehicleId;

    @Column(name = "route_id", length = 20, nullable = false)
    private String routeId;

    @Column(name = "route_name", length = 50, nullable = false)
    private String routeName;

    /** 운행 시작 시 스냅샷: 노선유형 */
    @Column(name = "route_type_code")
    private Integer routeTypeCode;

    @Column(name = "route_type_label", length = 20)
    private String routeTypeLabel;

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

    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lon")
    private Double lastLon;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
