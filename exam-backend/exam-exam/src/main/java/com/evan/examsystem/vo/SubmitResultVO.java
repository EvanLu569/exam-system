package com.evan.examsystem.vo;

import lombok.Data;

import java.util.List;

@Data
public class SubmitResultVO {
    private Long recordId;
    private Integer totalScore;
    private Integer correctCount;
    private Integer totalCount;
    private List<Detail> details;

    @Data
    public static class Detail {
        private Long questionId;
        private String userAnswer;
        private String correctAnswer;
        private Integer isCorrect;
        private Integer score;
    }
}