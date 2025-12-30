package com.studyblock.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyUserDto {
    private String month;      // "1월", "2월"
    private Integer count;     // 신규 가입자 수
}

