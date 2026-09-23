package com.evan.examsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaperVO {
    private Long id;
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer questionCount;   // 题目数量
}