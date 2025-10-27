package com.example.demo.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StartOperationResponse {
    private Long operationId;

    private String vehicleId;
    private String plateNo;

    private String routeId;
    private String routeName;

    /** 공공API 매칭 결과 */
    private String apiVehId;
    private String apiPlainNo;
}
