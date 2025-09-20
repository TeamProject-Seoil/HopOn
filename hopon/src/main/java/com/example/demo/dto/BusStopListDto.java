package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusStopListDto {
	private String busRouteId;	//노선Id
	private String busRouteNm;	//노선명
	private String direction;	//진행방향
	private int seq;	//정류장 순번
	private String stationNm;	//정류장 이름
	private String station;	//정류소Id
	private String arsId;	//정류소 arsId
	private String routeType;	//노선유형 (3:간선, 4:지선, 5:순환, 6:광역)
	private Double gpsX;	//x좌표
	private Double gpsY;	//y좌표
	private String trnstnid;	//회차지정류장 Id (ars말고 정류장ID)
    private String section;     //섹션 ID
}
