package com.evan.examsystem.service;

import com.evan.examsystem.dto.PaperDTO;
import com.evan.examsystem.vo.PaperDetailVO;

import java.util.Map;

public interface PaperService {

    Map<String, Object> page(int page, int size);

    PaperDetailVO detail(Long id);

    Long create(PaperDTO dto);

    void update(Long id, PaperDTO dto);

    void delete(Long id);

    void publish(Long id);
}