// src/main/java/com/example/demo/dto/ArrivalNowResponse.java
package com.example.demo.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArrivalNowResponse {
    private String currentStopName; // 이번 정류장
    private String nextStopName;    // 다음 정류장
    private Integer etaSec;         // 다음정류장까지 남은 시간(초) - 없으면 null

    private Boolean lowFloor;        // 저상 여부 (앞서 추가)

    private Integer routeTypeCode;   // ← 노선유형 코드 (서울시 busRouteType)
    private String  routeTypeLabel;  // ← 라벨(간선/지선/…)
}
