package com.evan.examsystem.vo;

import lombok.Data;

import java.util.List;

@Data
public class GenerateResultVO {
    private Long paperId;
    private String name;
    private Integer totalScore;
    private Integer questionCount;
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        private Long questionId;
        private String content;
        private Integer type;
        private Integer difficulty;
        private String knowledgePoint;
        private Integer score;
    }
}