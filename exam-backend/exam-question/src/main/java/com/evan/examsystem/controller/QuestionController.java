package com.evan.examsystem.controller;

import com.evan.examsystem.common.Result;
import com.evan.examsystem.dto.QuestionDTO;
import com.evan.examsystem.service.QuestionService;
import com.evan.examsystem.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "题库模块")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Operation(summary = "分页查询题目")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(questionService.page(type, difficulty, keyword, page, size));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(id));
    }

    @Operation(summary = "新增题目")
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody QuestionDTO dto) {
        Long id = questionService.create(dto);
        return Result.success(Map.of("id", id));
    }

    @Operation(summary = "修改题目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody QuestionDTO dto) {
        questionService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }
}
