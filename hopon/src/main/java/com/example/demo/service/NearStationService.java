package com.example.demo.service;

import com.example.demo.dto.NearStationDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class NearStationService {
    private final WebClient webClient;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;


    
    public List<NearStationDto> getStationsByPos(double longitude, double latitude, int radius) {
        JsonNode root = webClient.get()
            .uri(uri -> uri
                .path("/stationinfo/getStationByPos")
                .queryParam("serviceKey", serviceKey)
                .queryParam("tmX",longitude)
                .queryParam("tmY",latitude)
                .queryParam("radius",radius)
                .queryParam("resultType","json")
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (root == null) return List.of();

        JsonNode items = root.path("msgBody")
                             .path("itemList");
                             

        List<NearStationDto> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode node : items) {
                list.add(new NearStationDto(
                    node.path("arsId").asText(),
                    node.path("stationId").asInt(),
                    node.path("stationNm").asText(),
                    node.path("gpsX").asDouble(),
                    node.path("gpsY").asDouble()
                ));
            }
        }
        return list;
    }
}
