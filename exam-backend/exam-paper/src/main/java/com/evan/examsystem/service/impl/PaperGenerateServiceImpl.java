package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.dto.GeneratePaperDTO;
import com.evan.examsystem.entity.ExamPaper;
import com.evan.examsystem.entity.PaperQuestion;
import com.evan.examsystem.entity.Question;
import com.evan.examsystem.mapper.ExamPaperMapper;
import com.evan.examsystem.mapper.PaperQuestionMapper;
import com.evan.examsystem.mapper.QuestionMapper;
import com.evan.examsystem.service.PaperGenerateService;
import com.evan.examsystem.vo.GenerateResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaperGenerateServiceImpl implements PaperGenerateService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Override
    @Transactional
    public GenerateResultVO generate(GeneratePaperDTO dto) {
        GeneratePaperDTO.GenerateRules rules = dto.getRules();
        if (rules == null || rules.getTypeDistribution() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 已选中的题目 ID，防止重复
        Set<Long> selectedIds = new HashSet<>();
        // 最终选中的题目
        List<Question> selected = new ArrayList<>();

        // ========== 第一步：按题型处理 ==========
        for (Map.Entry<Integer, Integer> entry : rules.getTypeDistribution().entrySet()) {
            Integer type = entry.getKey();
            Integer needCount = entry.getValue();
            if (needCount == null || needCount <= 0) continue;

            // 拿到这个题型的所有候选 ID
            List<Long> candidateIds = questionMapper.findIdsByTypeAndDifficulty(type, null);
            // 排除已选的
            candidateIds.removeAll(selectedIds);

            if (candidateIds.size() < needCount) {
                throw new BusinessException(400,
                        "题型 " + type + " 题目不足，需要 " + needCount + " 道，实际只有 " + candidateIds.size() + " 道");
            }

            // ========== 第二步：按知识点分组，保证覆盖 ==========
            // 查询这些候选题的完整信息（含知识点）
            List<Question> candidates = questionMapper.findByIds(candidateIds);
            Map<String, List<Question>> byKnowledge = candidates.stream()
                    .collect(Collectors.groupingBy(q ->
                            q.getKnowledgePoint() == null ? "未分类" : q.getKnowledgePoint()));

            List<Question> picked = new ArrayList<>();

            // 优先从规则指定的知识点里抽
            if (rules.getKnowledgePoints() != null) {
                for (String kp : rules.getKnowledgePoints()) {
                    List<Question> group = byKnowledge.get(kp);
                    if (group == null || group.isEmpty()) continue;
                    // 组内随机抽 1 道
                    Collections.shuffle(group);
                    Question q = group.get(0);
                    if (!selectedIds.contains(q.getId())) {
                        picked.add(q);
                        selectedIds.add(q.getId());
                        group.remove(0);
                    }
                }
            }

            // 如果还不够，从剩余候选里随机补
            if (picked.size() < needCount) {
                List<Question> remaining = candidates.stream()
                        .filter(q -> !selectedIds.contains(q.getId()))
                        .collect(Collectors.toList());
                Collections.shuffle(remaining);
                int need = needCount - picked.size();
                for (int i = 0; i < need && i < remaining.size(); i++) {
                    picked.add(remaining.get(i));
                    selectedIds.add(remaining.get(i).getId());
                }
            }

            // 如果还是不够，报错
            if (picked.size() < needCount) {
                throw new BusinessException(400, "题型 " + type + " 题目不足，无法组卷");
            }

            // 截取需要的数量
            selected.addAll(picked.subList(0, needCount));
        }

        // ========== 第三步：保存试卷 ==========
        int totalScore = selected.stream().mapToInt(Question::getScore).sum();

        ExamPaper paper = new ExamPaper();
        paper.setName(dto.getName());
        paper.setDurationMinutes(dto.getDurationMinutes());
        paper.setTotalScore(dto.getTotalScore() == null ? totalScore : dto.getTotalScore());
        paper.setStartTime(dto.getStartTime());
        paper.setEndTime(dto.getEndTime());
        paper.setStatus(0);
        examPaperMapper.insert(paper);

        // 保存试卷-题目关联
        List<PaperQuestion> pqs = new ArrayList<>();
        int order = 1;
        for (Question q : selected) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(q.getId());
            pq.setScore(q.getScore());
            pq.setSortOrder(order++);
            pqs.add(pq);
        }
        paperQuestionMapper.batchInsert(pqs);

        // ========== 第四步：组装返回 ==========
        GenerateResultVO vo = new GenerateResultVO();
        vo.setPaperId(paper.getId());
        vo.setName(paper.getName());
        vo.setTotalScore(paper.getTotalScore());
        vo.setQuestionCount(selected.size());

        List<GenerateResultVO.QuestionItem> items = selected.stream().map(q -> {
            GenerateResultVO.QuestionItem item = new GenerateResultVO.QuestionItem();
            item.setQuestionId(q.getId());
            item.setContent(q.getContent());
            item.setType(q.getType());
            item.setDifficulty(q.getDifficulty());
            item.setKnowledgePoint(q.getKnowledgePoint());
            item.setScore(q.getScore());
            return item;
        }).collect(Collectors.toList());
        vo.setQuestions(items);

        return vo;
    }
}