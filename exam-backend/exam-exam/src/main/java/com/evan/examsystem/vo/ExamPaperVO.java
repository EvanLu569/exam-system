package com.evan.examsystem.vo;

import lombok.Data;

import java.util.List;

@Data
public class ExamPaperVO {
    private Long paperId;
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        private Long id;
        private String content;
        private List<String> options;
        private Integer score;
        private Integer type;
        private Integer sortOrder;
        // 注意：没有 correctAnswer
    }
}