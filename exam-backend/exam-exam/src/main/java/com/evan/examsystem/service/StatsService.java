package com.evan.examsystem.service;

import java.util.List;
import java.util.Map;

public interface StatsService {
    List<Map<String, Object>> myScores(Long userId);
    List<Map<String, Object>> knowledgeMastery(Long userId);
    List<Map<String, Object>> paperAccuracy(Long paperId);
}
