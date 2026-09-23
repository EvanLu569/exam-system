package com.evan.examsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GeneratePaperDTO {
    private String name;
    private Integer durationMinutes;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private GenerateRules rules;

    @Data
    public static class GenerateRules {
        /** 题型分布：key=题型(1/2/3)，value=数量 */
        private Map<Integer, Integer> typeDistribution;
        /** 难度分布：key=难度(1/2/3)，value=数量 */
        private Map<Integer, Integer> difficultyDistribution;
        /** 知识点列表，每个知识点至少抽 1 道 */
        private List<String> knowledgePoints;
    }
}