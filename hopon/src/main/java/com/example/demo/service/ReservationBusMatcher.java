package com.example.demo.service;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.repository.StopCoord;
import com.example.demo.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationBusMatcher {

    private final BusLocationService busLocationService;
    private final StopRepository stopRepository;

    /** 승차 정류장 기준으로 가장 먼저 들어올 버스(같은 노선) 1대 선정 */
    public MatchResult pickBest(String routeId, String boardArsId) {
        Integer boardSeq = stopRepository.findSeq(routeId, boardArsId);
        StopCoord board  = stopRepository.findCoord(routeId, boardArsId);
        if (boardSeq == null || board == null) return null;

        List<BusLocationDto> buses = busLocationService.getBusPosByRtid(routeId);
        if (buses == null || buses.isEmpty()) return null;

        return buses.stream()
                .filter(b -> safeInt(b.getSectOrd()) <= boardSeq) // 정류장 '직전~도착' 구간
                .min(Comparator
                        .comparingInt((BusLocationDto b) -> boardSeq - safeInt(b.getSectOrd()))
                        .thenComparingDouble(b -> dist2(
                                nz(b.getGpsY()), nz(b.getGpsX()),
                                nz(board.getLat()), nz(board.getLon())
                        )))
                .map(b -> new MatchResult(b.getVehId(), b.getPlainNo()))
                .orElse(null);
    }

    public record MatchResult(String apiVehId, String apiPlainNo) {}

    private int safeInt(String s) { try { return Integer.parseInt(String.valueOf(s).trim()); } catch (Exception e) { return Integer.MIN_VALUE; } }
    private double nz(Double d) { return d == null ? 0.0 : d; }
    private double dist2(double lat1, double lon1, double lat2, double lon2) { double dy = lat1-lat2, dx = lon1-lon2; return dx*dx+dy*dy; }
}
