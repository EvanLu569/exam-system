package com.evan.examsystem.controller;

import com.evan.examsystem.common.Result;
import com.evan.examsystem.dto.SubmitExamDTO;
import com.evan.examsystem.service.ExamService;
import com.evan.examsystem.vo.StartExamVO;
import com.evan.examsystem.vo.SubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "考试模块")
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Operation(summary = "开始考试")
    @PostMapping("/{paperId}/start")
    public Result<StartExamVO> start(@PathVariable Long paperId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(examService.start(paperId, userId));
    }

    @Operation(summary = "保存进度")
    @PostMapping("/{recordId}/save")
    public Result<Void> save(@PathVariable Long recordId, @RequestBody SubmitExamDTO dto) {
        examService.save(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "交卷")
    @PostMapping("/{recordId}/submit")
    public Result<SubmitResultVO> submit(@PathVariable Long recordId, @RequestBody SubmitExamDTO dto) {
        return Result.success(examService.submit(recordId, dto));
    }

    @Operation(summary = "我的考试记录")
    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(examService.myRecords(userId, page, size));
    }

    @Operation(summary = "可参加的考试列表")
    @GetMapping("/available")
    public Result<List<Map<String, Object>>> available(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(examService.available(userId));
    }

    @Operation(summary = "考试记录详情")
    @GetMapping("/records/{id}")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return Result.success(examService.recordDetail(id, userId, role));
    }
}
