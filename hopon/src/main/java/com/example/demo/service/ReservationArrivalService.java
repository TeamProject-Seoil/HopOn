// src/main/java/com/example/demo/service/ReservationArrivalService.java
package com.example.demo.service;

import com.example.demo.dto.ReservationArrivalStateResponse;
import com.example.demo.dto.BusLocationDto;
import com.example.demo.entity.ReservationEntity;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.StopCoord;
import com.example.demo.repository.StopRepository;
import com.example.demo.support.AuthUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationArrivalService {

    private final AuthUserResolver authUserResolver;
    private final ReservationRepository reservationRepository;
    private final BusLocationService busLocationService;
    private final StopRepository stopRepository;

    @SuppressWarnings("ConstantConditions")
    public ReservationArrivalStateResponse getArrivalState(Authentication auth, Long reservationId) {
        var user = authUserResolver.requireUser(auth);

        ReservationEntity r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND"));

        if (!r.getUser().getUserNum().equals(user.getUserNum())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_OWNER");
        }
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            // 진행중 아닌 예약은 unknown으로
            return ReservationArrivalStateResponse.builder()
                    .reservationId(r.getId())
                    .unknown(true)
                    .build();
        }

        String routeId = r.getRouteId();
        String boardArsId = r.getBoardArsId();
        String destArsId  = r.getDestArsId();

        // 정류장 seq
        Integer boardSeq = stopRepository.findSeq(routeId, boardArsId);
        Integer destSeq  = stopRepository.findSeq(routeId, destArsId);
        if (boardSeq == null || destSeq == null) {
            return ReservationArrivalStateResponse.builder()
                    .reservationId(r.getId())
                    .unknown(true)
                    .build();
        }

        // 현재 노선의 차량 위치
        List<BusLocationDto> buses = busLocationService.getBusPosByRtid(routeId);
        if (buses == null || buses.isEmpty()) {
            return ReservationArrivalStateResponse.builder()
                    .reservationId(r.getId())
                    .unknown(true)
                    .build();
        }

        // 예약에 매칭된 버스 찾기 (apiVehId / apiPlainNo 우선)
        BusLocationDto matched = findMatchedBus(r, buses);
        if (matched == null) {
            return ReservationArrivalStateResponse.builder()
                    .reservationId(r.getId())
                    .unknown(true)
                    .build();
        }

        int sect = safeInt(matched.getSectOrd());

        // 아주 단순한 근사:
        // sect ≈ 직전 정류장의 seq, sect+1 ≈ 이번 정류장 seq 라고 가정
        int currentSeq = sect + 1;
        int nextSeq    = currentSeq + 1;

        // 🔔 알림용: 이번역이 승차역 / 하차역인가?
        boolean nearBoardStop = (currentSeq == boardSeq);
        boolean nearDestStop  = (currentSeq == destSeq);

        // 이번역 = 승차역?
        boolean atBoardStop = (currentSeq == boardSeq + 1);

        // 이번역 = 하차 다음역? (즉, 한 정거장 지나왔을 때)
        boolean atDestNext = (currentSeq == destSeq + 1);

        // 정류장 이름은 StopRepository에 메서드 있으면 쓰고, 없으면 그냥 null
        String currentStopId = null;
        String currentStopName = null;
        String nextStopId = null;
        String nextStopName = null;

        StopCoord board = stopRepository.findCoord(routeId, boardArsId);
        StopCoord dest  = stopRepository.findCoord(routeId, destArsId);
        // 필요하다면 seq -> stopId/Name 맵핑용 메서드를 하나 더 만들면 됨

        return ReservationArrivalStateResponse.builder()
                .reservationId(r.getId())
                .currentStopId(currentStopId)
                .currentStopName(currentStopName)
                .nextStopId(nextStopId)
                .nextStopName(nextStopName)
                .nearBoardStop(nearBoardStop)   // 🔔
                .nearDestStop(nearDestStop)     // 🔔
                .atBoardStop(atBoardStop)       // ✅
                .atDestNext(atDestNext)         // ✅
                .unknown(false)
                .build();
    }

    private BusLocationDto findMatchedBus(ReservationEntity r, List<BusLocationDto> buses) {
        if (StringUtils.hasText(r.getApiVehId())) {
            var found = buses.stream()
                    .filter(b -> r.getApiVehId().equals(b.getVehId()))
                    .findFirst()
                    .orElse(null);
            if (found != null) return found;
        }
        if (StringUtils.hasText(r.getApiPlainNo())) {
            String norm = normalizePlate(r.getApiPlainNo());
            var found = buses.stream()
                    .filter(b -> norm.equals(normalizePlate(b.getPlainNo())))
                    .findFirst()
                    .orElse(null);
            if (found != null) return found;
        }

        // 혹시 안 맞으면, 승차역 기준으로 제일 먼저 들어올 버스 선택(옵션)
        Integer boardSeq = stopRepository.findSeq(r.getRouteId(), r.getBoardArsId());
        if (boardSeq == null) return null;

        return buses.stream()
                .filter(b -> safeInt(b.getSectOrd()) <= boardSeq)
                .min(Comparator.comparingInt(b -> boardSeq - safeInt(b.getSectOrd())))
                .orElse(null);
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(String.valueOf(s).trim()); }
        catch (Exception e) { return Integer.MIN_VALUE; }
    }

    private String normalizePlate(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9가-힣A-Za-z]", "").toUpperCase();
    }
}
