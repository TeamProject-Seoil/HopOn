package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

/** 운행 종료 (사유/메모 등 확장 가능) */
@Getter @Setter
public class EndOperationRequest {
    private String memo;
    private Boolean unassign; // true면 운행종료 시 배정 해제
}