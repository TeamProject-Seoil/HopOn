package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.BusLocationDto;
import com.example.demo.dto.BusRoutePathDto;
import com.example.demo.dto.BusStopListDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class BusLocationService {
    private final WebClient webClient;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;


    
    public List<BusLocationDto>getBusPosByRtid(String busRouteId) {
        JsonNode root = webClient.get()
            .uri(uri -> uri
                .path("/buspos/getBusPosByRtid")
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
                             
        
        List<BusLocationDto> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode node : items) {
                list.add(new BusLocationDto(
                    node.path("vehId").asText(),
                    node.path("plainNo").asText(),
                    node.path("busType").asText(),
                    node.path("nextStId").asText(),
                    node.path("congetion").asText(),
                    node.path("gpsX").asDouble(),
                    node.path("gpsY").asDouble(),
                    node.path("sectOrd").asText(),
                    node.path("stopFlag").asText()
                ));
            }
        }
        return list;
    }
}

