package com.evan.examsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamPaper {
    private Long id;
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;          // 0未发布 1已发布 2已结束
    private LocalDateTime createTime;
}