package com.evan.examsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitExamDTO {
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}