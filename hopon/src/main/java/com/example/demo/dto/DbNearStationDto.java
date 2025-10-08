package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // Native Query 결과를 매핑할 때 기본 생성자가 필요할 수 있습니다.
public class DbNearStationDto {

    private String stId;
    private String arsId;
    private String name;
    private Double lon;
    private Double lat;
    //private Double distance; // 계산된 거리 (미터 단위)
}
