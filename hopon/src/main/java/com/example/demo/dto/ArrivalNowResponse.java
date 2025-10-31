// src/main/java/com/example/demo/dto/ArrivalNowResponse.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArrivalNowResponse {

    /** 이번 정류장 ID (stops.st_id) */
    private String currentStopId;

    /** 이번 정류장 이름 */
    private String currentStopName;

    /** 다음 정류장 이름 */
    private String nextStopName;

    /** 다음 정류장까지 남은 시간(초). 없으면 null */
    private Integer etaSec;

    /** 다음 정류장 ID (stops.st_id) */
    private String nextStopId;

    /** 노선유형 코드 (서울시 busRouteType) */
    private Integer routeTypeCode;

    /** 노선유형 라벨(간선/지선/…) */
    private String  routeTypeLabel;
}
