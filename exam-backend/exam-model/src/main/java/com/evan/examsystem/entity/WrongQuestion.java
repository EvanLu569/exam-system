package com.evan.examsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongQuestion {
    private Long id;
    private Long userId;
    private Long questionId;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private LocalDateTime nextReviewTime;
    private Integer mastered;
}