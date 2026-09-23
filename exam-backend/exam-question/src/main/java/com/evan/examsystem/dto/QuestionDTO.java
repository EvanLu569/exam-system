package com.evan.examsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionDTO {
    private String content;
    private List<String> options;   // 前端传数组，后端转 JSON 字符串
    private String correctAnswer;
    private Integer score;
    private Integer type;
    private Integer difficulty;
    private String knowledgePoint;
}