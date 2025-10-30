package com.example.demo.service;

import com.example.demo.dto.BusStopListDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class BusStopListService {
    private final WebClient webClient;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;


    @Cacheable(value = "routePathCache", key = "#busRouteId")
    public List<BusStopListDto> getStaionByRoute(String busRouteId) {
        JsonNode root = webClient.get()
            .uri(uri -> uri
                .path("/busRouteInfo/getStaionByRoute")
                .queryParam("serviceKey", serviceKey)
                .queryParam("busRouteId",busRouteId)
                .queryParam("resultType","json")
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (root == null) return List.of();

        JsonNode items = root.path("msgBody")
                             .path("itemList");
                             

        List<BusStopListDto> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode node : items) {
                list.add(new BusStopListDto(
                    node.path("busRouteId").asText(),
                    node.path("busRouteNm").asText(),
                    node.path("direction").asText(),
                    node.path("seq").asInt(),
                    node.path("stationNm").asText(),
                    node.path("station").asText(),
                    node.path("arsId").asText(),
                    node.path("routeType").asText(),
                    node.path("gpsX").asDouble(),
                    node.path("gpsY").asDouble(),
                    node.path("trnstnid").asText()
                ));
            }
        }
        return list;
    }
}
