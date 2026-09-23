package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.entity.Question;
import com.evan.examsystem.entity.WrongQuestion;
import com.evan.examsystem.mapper.QuestionMapper;
import com.evan.examsystem.mapper.WrongQuestionMapper;
import com.evan.examsystem.service.WrongQuestionService;
import com.evan.examsystem.vo.WrongQuestionVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrongQuestionServiceImpl implements WrongQuestionService {

    @Autowired private WrongQuestionMapper wrongQuestionMapper;
    @Autowired private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> page(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<WrongQuestion> list = wrongQuestionMapper.findByUserId(userId, offset, size);
        long total = wrongQuestionMapper.countByUserId(userId);

        List<WrongQuestionVO> voList = toVOList(list);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", voList);
        return result;
    }

    @Override
    public List<WrongQuestionVO> reviewToday(Long userId) {
        List<WrongQuestion> list = wrongQuestionMapper.findReviewToday(userId, LocalDateTime.now());
        return toVOList(list);
    }

    @Override
    public void master(Long id, Long userId) {
        WrongQuestion wq = wrongQuestionMapper.findById(id);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        wrongQuestionMapper.updateMastered(id, 1);
    }

    @Override
    public void delete(Long id, Long userId) {
        WrongQuestion wq = wrongQuestionMapper.findById(id);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        wrongQuestionMapper.deleteById(id);
    }

    private List<WrongQuestionVO> toVOList(List<WrongQuestion> list) {
        if (list.isEmpty()) return new ArrayList<>();

        List<Long> qids = list.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.findByIds(qids);
        Map<Long, Question> qMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<WrongQuestionVO> voList = new ArrayList<>();
        for (WrongQuestion wq : list) {
            Question q = qMap.get(wq.getQuestionId());
            if (q == null) continue;
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(wq.getId());
            vo.setQuestionId(q.getId());
            vo.setContent(q.getContent());
            vo.setCorrectAnswer(q.getCorrectAnswer());
            vo.setType(q.getType());
            vo.setDifficulty(q.getDifficulty());
            vo.setKnowledgePoint(q.getKnowledgePoint());
            vo.setWrongCount(wq.getWrongCount());
            vo.setLastWrongTime(wq.getLastWrongTime());
            vo.setNextReviewTime(wq.getNextReviewTime());
            vo.setMastered(wq.getMastered());
            try {
                if (q.getOptions() != null) {
                    vo.setOptions(objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {}));
                }
            } catch (Exception e) {
                vo.setOptions(new ArrayList<>());
            }
            voList.add(vo);
        }
        return voList;
    }
}