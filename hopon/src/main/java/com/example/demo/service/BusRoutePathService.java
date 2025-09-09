package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.BusRoutePathDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class BusRoutePathService {
    private final WebClient webClient;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;


    
    public List<BusRoutePathDto> getStaionByRoute(String busRouteId) {
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
                             
        
        List<BusRoutePathDto> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode node : items) {
                list.add(new BusRoutePathDto(
                    node.path("gpsX").asDouble(),
                    node.path("gpsY").asDouble()
                ));
            }
        }
        return list;
    }
}
