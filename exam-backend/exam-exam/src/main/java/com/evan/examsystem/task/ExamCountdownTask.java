package com.evan.examsystem.task;

import com.evan.examsystem.config.ExamWebSocketHandler;
import com.evan.examsystem.entity.ExamPaper;
import com.evan.examsystem.entity.ExamRecord;
import com.evan.examsystem.mapper.ExamPaperMapper;
import com.evan.examsystem.mapper.ExamRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class ExamCountdownTask {

    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private ExamPaperMapper examPaperMapper;

    @Scheduled(fixedRate = 3000)
    public void pushCountdown() {
        for (Long recordId : ExamWebSocketHandler.getActiveRecordIds()) {
            ExamRecord record = examRecordMapper.findById(recordId);
            if (record == null || record.getStatus() == 1) {
                // 已交卷，关闭连接
                ExamWebSocketHandler.closeAll(recordId);
                continue;
            }

            ExamPaper paper = examPaperMapper.findById(record.getPaperId());
            if (paper == null) continue;

            long elapsed = Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            long total = paper.getDurationMinutes() * 60L;
            long remaining = total - elapsed;

            if (remaining <= 0) {
                // 时间到，广播强制交卷
                ExamWebSocketHandler.broadcast(recordId, "{\"type\":\"force_submit\",\"reason\":\"时间到\"}");
                ExamWebSocketHandler.closeAll(recordId);
                continue;
            }

            String msg = String.format("{\"type\":\"countdown\",\"remainingSeconds\":%d}", remaining);
            ExamWebSocketHandler.broadcast(recordId, msg);
        }
    }
}