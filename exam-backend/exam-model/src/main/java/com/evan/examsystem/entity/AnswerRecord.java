package com.evan.examsystem.entity;

import lombok.Data;

@Data
public class AnswerRecord {
    private Long id;
    private Long recordId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;   // 0错 1对
    private Integer score;
}