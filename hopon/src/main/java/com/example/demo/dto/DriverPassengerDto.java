// src/main/java/com/example/demo/dto/DriverPassengerDto.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverPassengerDto {
    private Long   reservationId;
    private Long   userNum;
    private String username;
    private String userid;

    private String boardingStopId;
    private String boardingStopName;
    private String alightingStopId;
    private String alightingStopName;

    private String status;          // CONFIRMED / BOARDED
    private String createdAtIso;    // ISO-8601 (UTC)
    private String updatedAtIso;    // ISO-8601 (UTC)
}
