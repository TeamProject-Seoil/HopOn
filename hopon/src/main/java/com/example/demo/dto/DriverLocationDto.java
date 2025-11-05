package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

//server: DriverLocationDto.java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverLocationDto {
    private Double lat;

    @JsonProperty("lng")  // ← 키 이름을 클라에 맞춤
    private Double lon;

    private Long operationId;
    private String updatedAtIso;
    private Boolean stale;
    private String plainNo;

    private String  routeType;
    private String  routeTypeLabel;
    private Integer routeTypeCode;
    
    private Boolean delayed;
    
}

