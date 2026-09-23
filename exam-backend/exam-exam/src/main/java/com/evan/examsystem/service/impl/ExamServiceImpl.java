package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.dto.SubmitExamDTO;
import com.evan.examsystem.entity.*;
import com.evan.examsystem.mapper.*;
import com.evan.examsystem.service.ExamService;
import com.evan.examsystem.vo.ExamPaperVO;
import com.evan.examsystem.vo.StartExamVO;
import com.evan.examsystem.vo.SubmitResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired private ExamPaperMapper examPaperMapper;
    @Autowired private PaperQuestionMapper paperQuestionMapper;
    @Autowired private QuestionMapper questionMapper;
    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private AnswerRecordMapper answerRecordMapper;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public StartExamVO start(Long paperId, Long userId) {
        ExamPaper paper = examPaperMapper.findById(paperId);
        if (paper == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (paper.getStatus() != 1) throw new BusinessException(ResultCode.EXAM_NOT_STARTED);

        // 检查是否已交卷
        ExamRecord exist = examRecordMapper.findByPaperAndUser(paperId, userId);
        if (exist != null && exist.getStatus() == 1) {
            throw new BusinessException(ResultCode.EXAM_ALREADY_SUBMITTED);
        }

        // 如果已有进行中的记录，直接复用
        ExamRecord record = exist;
        if (record == null || record.getStatus() == 1) {
            record = new ExamRecord();
            record.setPaperId(paperId);
            record.setUserId(userId);
            record.setTotalScore(0);
            record.setStartTime(LocalDateTime.now());
            record.setStatus(0);
            examRecordMapper.insert(record);
        }

        // 组装试卷（不含答案）
        List<PaperQuestion> pqs = paperQuestionMapper.findByPaperId(paperId);
        List<Long> qids = pqs.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = qids.isEmpty() ? new ArrayList<>() : questionMapper.findByIds(qids);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        ExamPaperVO paperVO = new ExamPaperVO();
        paperVO.setPaperId(paper.getId());
        paperVO.setName(paper.getName());
        paperVO.setDurationMinutes(paper.getDurationMinutes());
        paperVO.setTotalScore(paper.getTotalScore());

        List<ExamPaperVO.QuestionItem> items = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = qMap.get(pq.getQuestionId());
            if (q == null) continue;
            ExamPaperVO.QuestionItem item = new ExamPaperVO.QuestionItem();
            item.setId(q.getId());
            item.setContent(q.getContent());
            item.setScore(pq.getScore());
            item.setType(q.getType());
            item.setSortOrder(pq.getSortOrder());
            try {
                if (q.getOptions() != null) {
                    item.setOptions(objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {}));
                }
            } catch (Exception e) { item.setOptions(new ArrayList<>()); }
            items.add(item);
        }
        paperVO.setQuestions(items);

        // 计算剩余秒数
        long elapsed = Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
        long total = paper.getDurationMinutes() * 60L;
        int remaining = (int) Math.max(0, total - elapsed);

        StartExamVO vo = new StartExamVO();
        vo.setRecordId(record.getId());
        vo.setPaper(paperVO);
        vo.setRemainingSeconds(remaining);
        return vo;
    }

    @Override
    public void save(Long recordId, SubmitExamDTO dto) {
        ExamRecord record = examRecordMapper.findById(recordId);
        if (record == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (record.getStatus() == 1) throw new BusinessException(ResultCode.EXAM_ALREADY_SUBMITTED);

        answerRecordMapper.deleteByRecordId(recordId);
        if (dto.getAnswers() == null) return;

        List<AnswerRecord> list = new ArrayList<>();
        for (SubmitExamDTO.AnswerItem item : dto.getAnswers()) {
            AnswerRecord ar = new AnswerRecord();
            ar.setRecordId(recordId);
            ar.setQuestionId(item.getQuestionId());
            ar.setUserAnswer(item.getUserAnswer());
            ar.setIsCorrect(0);
            ar.setScore(0);
            list.add(ar);
        }
        if (!list.isEmpty()) answerRecordMapper.batchInsert(list);
    }

    @Override
    @Transactional
    public SubmitResultVO submit(Long recordId, SubmitExamDTO dto) {
        ExamRecord record = examRecordMapper.findById(recordId);
        if (record == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (record.getStatus() == 1) throw new BusinessException(ResultCode.EXAM_ALREADY_SUBMITTED);

        // 试卷题目
        List<PaperQuestion> pqs = paperQuestionMapper.findByPaperId(record.getPaperId());
        Map<Long, Integer> scoreMap = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        // 学生答案
        Map<Long, String> answerMap = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (SubmitExamDTO.AnswerItem item : dto.getAnswers()) {
                answerMap.put(item.getQuestionId(), item.getUserAnswer());
            }
        }

        // 判卷
        List<Long> qids = new ArrayList<>(scoreMap.keySet());
        List<Question> questions = questionMapper.findByIds(qids);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        int correctCount = 0;
        List<SubmitResultVO.Detail> details = new ArrayList<>();
        List<AnswerRecord> answerRecords = new ArrayList<>();

        for (Long qid : qids) {
            Question q = qMap.get(qid);
            if (q == null) continue;
            String userAnswer = answerMap.getOrDefault(qid, "");
            int score = scoreMap.get(qid);

            boolean correct = isCorrect(q.getType(), userAnswer, q.getCorrectAnswer());
            int gotScore = correct ? score : 0;
            totalScore += gotScore;
            if (correct) correctCount++;

            // 答题明细
            AnswerRecord ar = new AnswerRecord();
            ar.setRecordId(recordId);
            ar.setQuestionId(qid);
            ar.setUserAnswer(userAnswer);
            ar.setIsCorrect(correct ? 1 : 0);
            ar.setScore(gotScore);
            answerRecords.add(ar);

            // 错题本
            if (!correct) {
                saveWrongQuestion(record.getUserId(), qid);
            }

            SubmitResultVO.Detail d = new SubmitResultVO.Detail();
            d.setQuestionId(qid);
            d.setUserAnswer(userAnswer);
            d.setCorrectAnswer(q.getCorrectAnswer());
            d.setIsCorrect(correct ? 1 : 0);
            d.setScore(gotScore);
            details.add(d);
        }

        // 保存答题明细
        answerRecordMapper.deleteByRecordId(recordId);
        if (!answerRecords.isEmpty()) answerRecordMapper.batchInsert(answerRecords);

        // 更新考试记录
        record.setTotalScore(totalScore);
        record.setSubmitTime(LocalDateTime.now());
        record.setStatus(1);
        examRecordMapper.update(record);

        SubmitResultVO vo = new SubmitResultVO();
        vo.setRecordId(recordId);
        vo.setTotalScore(totalScore);
        vo.setCorrectCount(correctCount);
        vo.setTotalCount(qids.size());
        vo.setDetails(details);
        return vo;
    }

    @Override
    public Map<String, Object> myRecords(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<ExamRecord> list = examRecordMapper.findByUserId(userId, offset, size);
        long total = examRecordMapper.countByUserId(userId);

        List<Map<String, Object>> voList = new ArrayList<>();
        for (ExamRecord r : list) {
            ExamPaper p = examPaperMapper.findById(r.getPaperId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("paperId", r.getPaperId());
            m.put("paperName", p == null ? "" : p.getName());
            m.put("totalScore", r.getTotalScore());
            m.put("startTime", r.getStartTime());
            m.put("submitTime", r.getSubmitTime());
            m.put("status", r.getStatus());
            voList.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", voList);
        return result;
    }

    @Override
    public List<Map<String, Object>> available(Long userId) {
        List<ExamPaper> papers = examPaperMapper.findByStatus(1);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamPaper p : papers) {
            if (p.getStartTime() != null && p.getStartTime().isAfter(now)) continue;
            if (p.getEndTime() != null && p.getEndTime().isBefore(now)) continue;
            ExamRecord record = examRecordMapper.findByPaperAndUser(p.getId(), userId);
            boolean submitted = record != null && record.getStatus() == 1;
            boolean inProgress = record != null && record.getStatus() == 0;
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("durationMinutes", p.getDurationMinutes());
            m.put("totalScore", p.getTotalScore());
            m.put("startTime", p.getStartTime());
            m.put("endTime", p.getEndTime());
            m.put("status", p.getStatus());
            m.put("joined", submitted);
            m.put("submitted", submitted);
            m.put("inProgress", inProgress);
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> recordDetail(Long recordId, Long userId, String role) {
        ExamRecord record = examRecordMapper.findById(recordId);
        if (record == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!"admin".equals(role) && !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        ExamPaper paper = examPaperMapper.findById(record.getPaperId());
        List<AnswerRecord> answers = answerRecordMapper.findByRecordId(recordId);
        Map<Long, Question> qMap = new HashMap<>();
        if (!answers.isEmpty()) {
            List<Long> qids = answers.stream().map(AnswerRecord::getQuestionId).collect(Collectors.toList());
            qMap = questionMapper.findByIds(qids).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        for (AnswerRecord a : answers) {
            Question q = qMap.get(a.getQuestionId());
            if (q == null) continue;
            Map<String, Object> qm = new HashMap<>();
            qm.put("questionId", q.getId());
            qm.put("content", q.getContent());
            qm.put("options", parseOptions(q.getOptions()));
            qm.put("type", q.getType());
            qm.put("userAnswer", a.getUserAnswer());
            qm.put("correctAnswer", q.getCorrectAnswer());
            qm.put("isCorrect", a.getIsCorrect() != null && a.getIsCorrect() == 1);
            qm.put("score", a.getScore());
            questions.add(qm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("paperId", record.getPaperId());
        result.put("paperName", paper == null ? "已删除试卷" : paper.getName());
        result.put("paperTotalScore", paper == null ? 0 : paper.getTotalScore());
        result.put("totalScore", record.getTotalScore());
        result.put("correctCount", questions.stream().filter(q -> Boolean.TRUE.equals(q.get("isCorrect"))).count());
        result.put("totalCount", questions.size());
        result.put("startTime", record.getStartTime());
        result.put("submitTime", record.getSubmitTime());
        result.put("status", record.getStatus());
        result.put("questions", questions);
        return result;
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 判卷：单选/判断全对，多选少选得半分、错选不得分 */
    private boolean isCorrect(Integer type, String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) return false;
        if (type == 2) {
            // 多选：错选不得分，少选得半分（这里简化：全对才算对）
            return userAnswer.equalsIgnoreCase(correctAnswer);
        }
        return userAnswer.equalsIgnoreCase(correctAnswer);
    }

    /** 错题写入错题本，按艾宾浩斯安排下次复习 */
    private void saveWrongQuestion(Long userId, Long questionId) {
        WrongQuestion exist = wrongQuestionMapper.findByUserAndQuestion(userId, questionId);
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongTime(now);
            wq.setNextReviewTime(now.plusDays(1));
            wq.setMastered(0);
            wrongQuestionMapper.insert(wq);
        } else {
            int count = exist.getWrongCount() + 1;
            int[] intervals = {1, 2, 4, 7, 15};
            int days = intervals[Math.min(count - 1, intervals.length - 1)];
            exist.setWrongCount(count);
            exist.setLastWrongTime(now);
            exist.setNextReviewTime(now.plusDays(days));
            exist.setMastered(0);
            wrongQuestionMapper.update(exist);
        }
    }
}