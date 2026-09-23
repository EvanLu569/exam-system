package com.evan.examsystem.service;

import com.evan.examsystem.vo.WrongQuestionVO;

import java.util.List;
import java.util.Map;

public interface WrongQuestionService {
    Map<String, Object> page(Long userId, int page, int size);
    List<WrongQuestionVO> reviewToday(Long userId);
    void master(Long id, Long userId);
    void delete(Long id, Long userId);
}