package com.evan.examsystem.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WrongQuestionVO {
    private Long id;
    private Long questionId;
    private String content;
    private List<String> options;
    private String correctAnswer;
    private Integer type;
    private Integer difficulty;
    private String knowledgePoint;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private LocalDateTime nextReviewTime;
    private Integer mastered;
}