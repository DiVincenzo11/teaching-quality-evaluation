package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.Semester;
import com.bishe.teach.mapper.SemesterMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通用接口：所有已登录用户可访问
 */
@Tag(name = "通用接口")
@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final SemesterMapper semesterMapper;

    @Operation(summary = "获取学期列表（所有已登录用户可用）")
    @GetMapping("/semesters")
    public Result<List<Semester>> listSemesters() {
        List<Semester> list = semesterMapper.selectList(
                new LambdaQueryWrapper<Semester>().orderByAsc(Semester::getStartDate));
        return Result.success(list);
    }
}
