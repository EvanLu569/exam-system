package com.evan.examsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperDTO {
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<PaperQuestionDTO> questions;

    @Data
    public static class PaperQuestionDTO {
        private Long questionId;
        private Integer score;
        private Integer sortOrder;
    }
}