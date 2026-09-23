package com.evan.examsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Question {
    private Long id;
    private String content;
    private String options;       // JSON 字符串
    private String correctAnswer;
    private Integer score;
    private Integer type;         // 1单选 2多选 3判断
    private Integer difficulty;   // 1简单 2中等 3困难
    private String knowledgePoint;
    private LocalDateTime createTime;
}