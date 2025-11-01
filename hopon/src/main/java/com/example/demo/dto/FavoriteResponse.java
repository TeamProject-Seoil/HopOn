package com.example.demo.dto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteResponse {
    private Long id;
    private String routeId;
    private String direction;
    private String boardStopId;
    private String boardStopName;
    private String boardArsId;
    private String destStopId;
    private String destStopName;
    private String destArsId;
    private String routeName;
    
    // ⬇ 추가
    private Integer busRouteType;
    private String routeTypeName;
}