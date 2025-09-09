package com.example.demo.service;

import com.example.demo.dto.StationStopListDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor

public class StationStopListService {
    private final WebClient webClient;

    @Value("${publicdata.serviceKey}")
    private String serviceKey;


    
    public List<StationStopListDto> getStationByUid(String arsId) {
        JsonNode root = webClient.get()
            .uri(uri -> uri
                .path("/stationinfo/getStationByUid")
                .queryParam("serviceKey", serviceKey)
                .queryParam("arsId",arsId)
                .queryParam("resultType","json")
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        if (root == null) return List.of();

        JsonNode items = root.path("msgBody")
                             .path("itemList");
                             

        List<StationStopListDto> list = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode node : items) {
                list.add(new StationStopListDto(
                    node.path("rtNm").asText(),
                    node.path("busRouteId").asText(),
                    node.path("adirection").asText(),
                    node.path("routeType").asText(),
                    node.path("arrmsg1").asText(),
                    node.path("arrmsg2").asText(),
                    node.path("busType1").asText(),
                    node.path("busType2").asText(),
                    node.path("congestion1").asText(),
                    node.path("congestion2").asText(),
                    node.path("rerdieDiv1").asText(),
                    node.path("rerdieDiv2").asText()
                ));
            }
        }
        return list;
    }
}
