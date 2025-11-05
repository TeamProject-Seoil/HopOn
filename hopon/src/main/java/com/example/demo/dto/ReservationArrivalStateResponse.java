// src/main/java/com/example/demo/dto/ReservationArrivalStateResponse.java
package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationArrivalStateResponse {

    private Long reservationId;

    // 이번역/다음역
    private String currentStopId;
    private String currentStopName;
    private String nextStopId;
    private String nextStopName;

    // 승차/하차 관련 플래그
    /** 이번역이 승차 다음역인가? */
    private boolean atBoardStop;

    /** 이번역이 하차 다음역인가? (하차 직후 기준으로 다이얼로그) */
    private boolean atDestNext;

    // 🔔 알림용
    /** 이번역이 승차역인가? (알림용) */
    private boolean nearBoardStop;
    /** 이번역이 하차역인가? (알림용) */
    private boolean nearDestStop;
    
    /** 버스를 못 찾았거나 계산 불가하면 true */
    private boolean unknown;
}
