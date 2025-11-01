package com.example.demo.dto;
import lombok.Data;

@Data
public class FavoriteCreateRequest {
    private String routeId;
    private String direction;
    private String boardStopId;
    private String boardStopName;
    private String boardArsId;
    private String destStopId;
    private String destStopName;
    private String destArsId;
    private String routeName;
    
    private Integer busRouteType;     // 예: 1(간선), 2(지선) 등
    private String routeTypeName;     // 예: "간선", "지선"
}