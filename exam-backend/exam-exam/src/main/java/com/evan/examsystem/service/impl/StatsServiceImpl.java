package com.evan.examsystem.service.impl;

import com.evan.examsystem.entity.*;
import com.evan.examsystem.mapper.*;
import com.evan.examsystem.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private ExamPaperMapper examPaperMapper;
    @Autowired private AnswerRecordMapper answerRecordMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private PaperQuestionMapper paperQuestionMapper;

    @Override
    public List<Map<String, Object>> myScores(Long userId) {
        List<ExamRecord> records = examRecordMapper.findByUserIdAndStatus(userId, 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            ExamPaper p = examPaperMapper.findById(r.getPaperId());
            Map<String, Object> m = new HashMap<>();
            m.put("paperName", p == null ? "已删除试卷" : p.getName());
            m.put("score", r.getTotalScore());
            String time = r.getSubmitTime() != null ? r.getSubmitTime().toString() : "";
            m.put("date", time.length() >= 10 ? time.substring(0, 10) : time);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> knowledgeMastery(Long userId) {
        List<ExamRecord> records = examRecordMapper.findByUserIdAndStatus(userId, 1);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        if (recordIds.isEmpty()) return new ArrayList<>();

        List<AnswerRecord> answers = answerRecordMapper.findByRecordIds(recordIds);
        List<Long> qids = answers.stream().map(AnswerRecord::getQuestionId).distinct().collect(Collectors.toList());
        Map<Long, Question> qMap = qids.isEmpty() ? new HashMap<>() :
                questionMapper.findByIds(qids).stream().collect(Collectors.toMap(Question::getId, q -> q));

        Map<String, int[]> byKp = new LinkedHashMap<>();
        for (AnswerRecord a : answers) {
            Question q = qMap.get(a.getQuestionId());
            if (q == null || q.getKnowledgePoint() == null) continue;
            int[] stat = byKp.computeIfAbsent(q.getKnowledgePoint(), k -> new int[2]);
            stat[0]++;
            if (a.getIsCorrect() != null && a.getIsCorrect() == 1) stat[1]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byKp.entrySet()) {
            int total = e.getValue()[0];
            int correct = e.getValue()[1];
            Map<String, Object> m = new HashMap<>();
            m.put("knowledgePoint", e.getKey());
            m.put("total", total);
            m.put("correct", correct);
            m.put("masteryRate", total == 0 ? 0 : Math.round((double) correct / total * 100) / 100.0);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> paperAccuracy(Long paperId) {
        List<ExamRecord> records = examRecordMapper.findByPaperId(paperId);
        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        List<PaperQuestion> pqs = paperQuestionMapper.findByPaperId(paperId);
        if (pqs.isEmpty()) return new ArrayList<>();

        List<Long> qids = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.findByIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<AnswerRecord> answers = recordIds.isEmpty() ? new ArrayList<>() : answerRecordMapper.findByRecordIds(recordIds);
        Map<Long, int[]> statMap = new HashMap<>();
        for (AnswerRecord a : answers) {
            int[] stat = statMap.computeIfAbsent(a.getQuestionId(), k -> new int[2]);
            stat[0]++;
            if (a.getIsCorrect() != null && a.getIsCorrect() == 1) stat[1]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) continue;
            int[] stat = statMap.getOrDefault(pq.getQuestionId(), new int[2]);
            int total = stat[0];
            int correct = stat[1];
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("content", q.getContent());
            m.put("totalCount", total);
            m.put("correctCount", correct);
            m.put("accuracyRate", total == 0 ? 0 : Math.round((double) correct / total * 100) / 100.0);
            result.add(m);
        }
        return result;
    }
}
