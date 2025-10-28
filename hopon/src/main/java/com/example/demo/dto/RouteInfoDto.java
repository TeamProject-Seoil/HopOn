package com.example.demo.dto;

import lombok.Data;

@Data
public class RouteInfoDto {
    private String routeId;
    private Integer busRouteType; // 서울시 코드
    private String routeTypeName; // 선택: API가 주면 저장
}
