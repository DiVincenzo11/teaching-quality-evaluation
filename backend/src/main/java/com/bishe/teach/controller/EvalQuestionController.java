package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.EvalQuestion;
import com.bishe.teach.mapper.EvalQuestionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评价题目（公共）")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class EvalQuestionController {

    private final EvalQuestionMapper questionMapper;

    @Operation(summary = "获取所有启用题目（用于填写问卷）")
    @GetMapping
    public Result<List<EvalQuestion>> listActive() {
        return Result.success(questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>()
                        .eq(EvalQuestion::getStatus, 1)
                        .orderByAsc(EvalQuestion::getSortOrder)));
    }
}
