package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusLocationDto {
	private String vehId;	//차량id
	private String plainNo;	//번호판
	private String busType;	//0:일반, 1:저상
	private String lastStnId ;	//다음정류장Id
	private String congetion;	//혼잡도(0:정보없음,3:여유,4:보통,5:혼잡,6:매우혼잡)
	private Double gpsX;	//x좌표
	private Double gpsY;	//y좌표
	private String sectOrd;	//구간순번
	private String stopFlag;	//정류소 도착 여부(1:정차, 0:운행)
}