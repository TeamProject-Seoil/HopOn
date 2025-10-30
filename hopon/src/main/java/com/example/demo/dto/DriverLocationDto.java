package com.example.demo.dto;

import lombok.*;

//server: DriverLocationDto.java
@Getter @Builder
public class DriverLocationDto {
 private Double lat;
 @com.fasterxml.jackson.annotation.JsonProperty("lng")
 private Double lon;         // 필드명은 lon이어도 JSON은 "lng"로 나감
 private Long operationId;
 private String updatedAtIso;
 private boolean stale;
}
