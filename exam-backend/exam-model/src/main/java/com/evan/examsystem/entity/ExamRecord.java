package com.evan.examsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamRecord {
    private Long id;
    private Long paperId;
    private Long userId;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer status;   // 0进行中 1已交卷
}