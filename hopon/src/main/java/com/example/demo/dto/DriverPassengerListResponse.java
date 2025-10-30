// src/main/java/com/example/demo/dto/DriverPassengerListResponse.java
package com.example.demo.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverPassengerListResponse {
    private Long operationId;              // 현재 운행 ID
    private String routeId;                // 운행 노선
    private String routeName;
    private Integer count;                 // 승객 수
    private List<DriverPassengerDto> items;
}
