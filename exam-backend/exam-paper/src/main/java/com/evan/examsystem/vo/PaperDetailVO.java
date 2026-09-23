package com.evan.examsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperDetailVO {
    private Long id;
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        private Long questionId;
        private String content;
        private List<String> options;
        private String correctAnswer;
        private Integer score;
        private Integer type;
        private Integer difficulty;
        private String knowledgePoint;
        private Integer sortOrder;
    }
}