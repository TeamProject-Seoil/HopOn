package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NearStationRequestDto {
    private double x;
    private double y;
    private int radius;
}
