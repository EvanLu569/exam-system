package com.evan.examsystem.controller;

import com.evan.examsystem.common.Result;
import com.evan.examsystem.dto.GeneratePaperDTO;
import com.evan.examsystem.dto.PaperDTO;
import com.evan.examsystem.service.PaperGenerateService;
import com.evan.examsystem.service.PaperService;
import com.evan.examsystem.vo.GenerateResultVO;
import com.evan.examsystem.vo.PaperDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "试卷模块")
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperGenerateService paperGenerateService;

    @Operation(summary = "分页查询试卷")
    @GetMapping
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(paperService.page(page, size));
    }

    @Operation(summary = "试卷详情")
    @GetMapping("/{id}")
    public Result<PaperDetailVO> detail(@PathVariable Long id) {
        return Result.success(paperService.detail(id));
    }

    @Operation(summary = "创建试卷")
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody PaperDTO dto) {
        Long id = paperService.create(dto);
        return Result.success(Map.of("id", id));
    }

    @Operation(summary = "修改试卷")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PaperDTO dto) {
        paperService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除试卷")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布试卷")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        paperService.publish(id);
        return Result.success();
    }

    @Operation(summary = "智能组卷")
    @PostMapping("/generate")
    public Result<GenerateResultVO> generate(@RequestBody GeneratePaperDTO dto) {
        return Result.success(paperGenerateService.generate(dto));
    }
}
