package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.dto.QuestionDTO;
import com.evan.examsystem.entity.Question;
import com.evan.examsystem.mapper.QuestionMapper;
import com.evan.examsystem.service.QuestionService;
import com.evan.examsystem.vo.QuestionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> page(Integer type, Integer difficulty, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Question> list = questionMapper.page(type, difficulty, keyword, offset, size);
        long total = questionMapper.count(type, difficulty, keyword);

        List<QuestionVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", voList);
        return result;
    }

    @Override
    public QuestionVO detail(Long id) {
        Question q = questionMapper.findById(id);
        if (q == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(q);
    }

    @Override
    public Long create(QuestionDTO dto) {
        Question q = toEntity(dto);
        questionMapper.insert(q);
        return q.getId();
    }

    @Override
    public void update(Long id, QuestionDTO dto) {
        Question exist = questionMapper.findById(id);
        if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND);
        Question q = toEntity(dto);
        q.setId(id);
        questionMapper.update(q);
    }

    @Override
    public void delete(Long id) {
        questionMapper.deleteById(id);
    }

    /** Entity → VO：options 从 JSON 字符串转数组 */
    private QuestionVO toVO(Question q) {
        QuestionVO vo = new QuestionVO();
        vo.setId(q.getId());
        vo.setContent(q.getContent());
        vo.setCorrectAnswer(q.getCorrectAnswer());
        vo.setScore(q.getScore());
        vo.setType(q.getType());
        vo.setDifficulty(q.getDifficulty());
        vo.setKnowledgePoint(q.getKnowledgePoint());
        try {
            if (q.getOptions() != null) {
                vo.setOptions(objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) {
            vo.setOptions(List.of());
        }
        return vo;
    }

    /** DTO → Entity：options 从数组转 JSON 字符串 */
    private Question toEntity(QuestionDTO dto) {
        Question q = new Question();
        q.setContent(dto.getContent());
        q.setCorrectAnswer(dto.getCorrectAnswer());
        q.setScore(dto.getScore() == null ? 5 : dto.getScore());
        q.setType(dto.getType() == null ? 1 : dto.getType());
        q.setDifficulty(dto.getDifficulty() == null ? 1 : dto.getDifficulty());
        q.setKnowledgePoint(dto.getKnowledgePoint());
        try {
            q.setOptions(objectMapper.writeValueAsString(dto.getOptions()));
        } catch (Exception e) {
            q.setOptions("[]");
        }
        return q;
    }
}