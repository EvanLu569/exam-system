package com.evan.examsystem.controller;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.Result;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "成绩统计模块")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Operation(summary = "个人成绩趋势")
    @GetMapping("/my-scores")
    public Result<List<Map<String, Object>>> myScores(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(statsService.myScores(userId));
    }

    @Operation(summary = "知识点掌握度")
    @GetMapping("/knowledge-mastery")
    public Result<List<Map<String, Object>>> knowledgeMastery(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(statsService.knowledgeMastery(userId));
    }

    @Operation(summary = "试卷题目正确率")
    @GetMapping("/paper/{paperId}/accuracy")
    public Result<List<Map<String, Object>>> paperAccuracy(@PathVariable Long paperId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) throw new BusinessException(ResultCode.FORBIDDEN);
        return Result.success(statsService.paperAccuracy(paperId));
    }
}
