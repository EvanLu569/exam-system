package com.evan.examsystem.service;

import com.evan.examsystem.dto.GeneratePaperDTO;
import com.evan.examsystem.vo.GenerateResultVO;

public interface PaperGenerateService {
    GenerateResultVO generate(GeneratePaperDTO dto);
}