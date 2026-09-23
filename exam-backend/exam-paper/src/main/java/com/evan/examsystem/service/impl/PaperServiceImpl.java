package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.dto.PaperDTO;
import com.evan.examsystem.entity.ExamPaper;
import com.evan.examsystem.entity.PaperQuestion;
import com.evan.examsystem.entity.Question;
import com.evan.examsystem.mapper.ExamPaperMapper;
import com.evan.examsystem.mapper.PaperQuestionMapper;
import com.evan.examsystem.mapper.QuestionMapper;
import com.evan.examsystem.service.PaperService;
import com.evan.examsystem.vo.PaperDetailVO;
import com.evan.examsystem.vo.PaperVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> page(int page, int size) {
        int offset = (page - 1) * size;
        List<ExamPaper> list = examPaperMapper.page(offset, size);
        long total = examPaperMapper.count();

        List<PaperVO> voList = list.stream().map(p -> {
            PaperVO vo = new PaperVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setDurationMinutes(p.getDurationMinutes());
            vo.setTotalScore(p.getTotalScore());
            vo.setStartTime(p.getStartTime());
            vo.setEndTime(p.getEndTime());
            vo.setStatus(p.getStatus());
            vo.setQuestionCount(paperQuestionMapper.countByPaperId(p.getId()));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", voList);
        return result;
    }

    @Override
    public PaperDetailVO detail(Long id) {
        ExamPaper paper = examPaperMapper.findById(id);
        if (paper == null) throw new BusinessException(ResultCode.NOT_FOUND);

        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(paper.getId());
        vo.setName(paper.getName());
        vo.setDurationMinutes(paper.getDurationMinutes());
        vo.setTotalScore(paper.getTotalScore());
        vo.setStartTime(paper.getStartTime());
        vo.setEndTime(paper.getEndTime());
        vo.setStatus(paper.getStatus());

        List<PaperQuestion> pqs = paperQuestionMapper.findByPaperId(id);
        if (pqs.isEmpty()) {
            vo.setQuestions(new ArrayList<>());
            return vo;
        }

        List<Long> questionIds = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.findByIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<PaperDetailVO.QuestionItem> items = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMap.get(pq.getQuestionId());
            if (q == null) continue;
            PaperDetailVO.QuestionItem item = new PaperDetailVO.QuestionItem();
            item.setQuestionId(q.getId());
            item.setContent(q.getContent());
            item.setCorrectAnswer(q.getCorrectAnswer());
            item.setScore(pq.getScore());
            item.setType(q.getType());
            item.setDifficulty(q.getDifficulty());
            item.setKnowledgePoint(q.getKnowledgePoint());
            item.setSortOrder(pq.getSortOrder());
            try {
                if (q.getOptions() != null) {
                    item.setOptions(objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {}));
                }
            } catch (Exception e) {
                item.setOptions(new ArrayList<>());
            }
            items.add(item);
        }
        vo.setQuestions(items);
        return vo;
    }

    @Override
    @Transactional
    public Long create(PaperDTO dto) {
        ExamPaper paper = new ExamPaper();
        paper.setName(dto.getName());
        paper.setDurationMinutes(dto.getDurationMinutes());
        paper.setTotalScore(dto.getTotalScore());
        paper.setStartTime(dto.getStartTime());
        paper.setEndTime(dto.getEndTime());
        paper.setStatus(0);
        examPaperMapper.insert(paper);

        savePaperQuestions(paper.getId(), dto.getQuestions());
        return paper.getId();
    }

    @Override
    @Transactional
    public void update(Long id, PaperDTO dto) {
        ExamPaper exist = examPaperMapper.findById(id);
        if (exist == null) throw new BusinessException(ResultCode.NOT_FOUND);

        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setName(dto.getName());
        paper.setDurationMinutes(dto.getDurationMinutes());
        paper.setTotalScore(dto.getTotalScore());
        paper.setStartTime(dto.getStartTime());
        paper.setEndTime(dto.getEndTime());
        examPaperMapper.update(paper);

        paperQuestionMapper.deleteByPaperId(id);
        savePaperQuestions(id, dto.getQuestions());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        paperQuestionMapper.deleteByPaperId(id);
        examPaperMapper.deleteById(id);
    }

    @Override
    public void publish(Long id) {
        ExamPaper paper = examPaperMapper.findById(id);
        if (paper == null) throw new BusinessException(ResultCode.NOT_FOUND);
        examPaperMapper.updateStatus(id, 1);
    }

    private void savePaperQuestions(Long paperId, List<PaperDTO.PaperQuestionDTO> questions) {
        if (questions == null || questions.isEmpty()) return;
        List<PaperQuestion> list = new ArrayList<>();
        int order = 1;
        for (PaperDTO.PaperQuestionDTO dto : questions) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(dto.getQuestionId());
            pq.setScore(dto.getScore() == null ? 5 : dto.getScore());
            pq.setSortOrder(dto.getSortOrder() == null ? order : dto.getSortOrder());
            list.add(pq);
            order++;
        }
        paperQuestionMapper.batchInsert(list);
    }
}