package com.evan.examsystem.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuestionVO {
    private Long id;
    private String content;
    private List<String> options;
    private String correctAnswer;
    private Integer score;
    private Integer type;
    private Integer difficulty;
    private String knowledgePoint;
}