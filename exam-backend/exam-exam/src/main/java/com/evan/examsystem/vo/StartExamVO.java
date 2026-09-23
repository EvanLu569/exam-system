package com.evan.examsystem.vo;

import lombok.Data;

@Data
public class StartExamVO {
    private Long recordId;
    private ExamPaperVO paper;
    private Integer remainingSeconds;   // 剩余秒数
}