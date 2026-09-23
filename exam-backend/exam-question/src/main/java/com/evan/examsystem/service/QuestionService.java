package com.evan.examsystem.service;

import com.evan.examsystem.dto.QuestionDTO;
import com.evan.examsystem.vo.QuestionVO;

import java.util.List;
import java.util.Map;

public interface QuestionService {

    Map<String, Object> page(Integer type, Integer difficulty, String keyword, int page, int size);

    QuestionVO detail(Long id);

    Long create(QuestionDTO dto);

    void update(Long id, QuestionDTO dto);

    void delete(Long id);
}