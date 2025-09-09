package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearStationDto {
    private String arsId;
    private int stationId;
    private String stationName;
    private double x;
    private double y;
}
