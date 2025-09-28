package com.example.demo.dto;

import lombok.Data;

@Data
public class ReservationRequestDto {
    private String routeId;
    private String direction;
    private String boardStopId;
    private String boardStopName;
    private String boardArsId;
    private String destStopId;
    private String destStopName;
    private String destArsId;
}