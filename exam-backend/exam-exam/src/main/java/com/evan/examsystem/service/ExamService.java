package com.evan.examsystem.service;

import com.evan.examsystem.dto.SubmitExamDTO;
import com.evan.examsystem.vo.StartExamVO;
import com.evan.examsystem.vo.SubmitResultVO;

import java.util.List;
import java.util.Map;

public interface ExamService {
    StartExamVO start(Long paperId, Long userId);
    void save(Long recordId, SubmitExamDTO dto);
    SubmitResultVO submit(Long recordId, SubmitExamDTO dto);
    Map<String, Object> myRecords(Long userId, int page, int size);
    List<Map<String, Object>> available(Long userId);
    Map<String, Object> recordDetail(Long recordId, Long userId, String role);
}