package com.evan.examsystem.controller;

import com.evan.examsystem.common.Result;
import com.evan.examsystem.service.WrongQuestionService;
import com.evan.examsystem.vo.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "错题本模块")
@RestController
@RequestMapping("/api/wrong-questions")
public class WrongQuestionController {

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Operation(summary = "错题列表")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(wrongQuestionService.page(userId, page, size));
    }

    @Operation(summary = "今日复习")
    @GetMapping("/review-today")
    public Result<List<WrongQuestionVO>> reviewToday(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(wrongQuestionService.reviewToday(userId));
    }

    @Operation(summary = "标记掌握")
    @PostMapping("/{id}/master")
    public Result<Void> master(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        wrongQuestionService.master(id, userId);
        return Result.success();
    }

    @Operation(summary = "删除错题")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        wrongQuestionService.delete(id, userId);
        return Result.success();
    }
}
