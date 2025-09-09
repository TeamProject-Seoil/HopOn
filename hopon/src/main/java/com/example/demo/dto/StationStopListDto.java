package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class StationStopListDto {
	private String rtNm;	//노선명
	private String busRouteId;	//노선Id
	private String adirection;	//방향
	private String routeType; //노선유형 (1.공항, 2.마을, 3.간선, 4.지선, 5.순환, 6.광역, 9.폐지)
	private String arrmsg1;	//첫번째 도착예정 버스의 도착정보메시지
	private String arrmsg2;	//두번째 도착예정 버스의 도착정보메시지
	private String busType1;	//첫번째 도착 예정 버스 차량유형 (0.일반버스, 1.저상)
	private String busType2;	//두번째 도착 예정 버스 차량유형
	private String congestion1;	//재차구분코드 1:잔여좌석, 2=재차인원, 3=만자, 4=혼잡도 (1번째 도착)
	private String congestion2;	//2번째 도착
	private String rerdieDiv1;	//재차구분 1 ->잔여좌석, 2 ->재차인원, 3 -> 만차, 4 -> 혼잡도 (3 : 여유, 4 : 보통, 5 : 혼잡, 6 : 매우혼잡)
	private String rerdieDiv2;
}
