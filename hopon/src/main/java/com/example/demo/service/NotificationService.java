// src/main/java/com/example/demo/service/NotificationService.java
package com.example.demo.service;

import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    // 실제론 FCM/푸시 연동
    public void sendDelayNotification(UserEntity user, ReservationEntity reservation) {
        // TODO: FCM 토큰 조회해서 "운행 지연" 푸시 발송
        // 제목: [지연 안내] 7016번 버스
        // 내용: "○○역 승차 예정 승객님, 현재 버스가 지연되고 있습니다."
        System.out.println("[NOTIFY] delay to user=" + user.getUserNum()
                + " res=" + reservation.getId());
    }
}
