package com.evan.examsystem.entity;

import lombok.Data;

@Data
public class PaperQuestion {
    private Long id;
    private Long paperId;
    private Long questionId;
    private Integer score;
    private Integer sortOrder;
}