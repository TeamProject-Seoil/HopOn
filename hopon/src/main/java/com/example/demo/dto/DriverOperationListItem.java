package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverOperationListItem {
    private Long id;
    private String routeId;
    private String routeName;
    private String vehicleId;
    private String plateNo;
    private String startedAt;   // ISO 문자열
    private String endedAt;     // ISO 문자열
    private Integer routeTypeCode;  // 선택
    private String  routeTypeLabel; // 선택
}
