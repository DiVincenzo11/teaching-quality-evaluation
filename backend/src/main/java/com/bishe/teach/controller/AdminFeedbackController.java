package com.bishe.teach.controller;

import com.bishe.teach.common.Result;
import com.bishe.teach.mapper.TeacherFeedbackMapper;
import com.bishe.teach.vo.FeedbackListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员-反馈查看")
@RestController
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final TeacherFeedbackMapper feedbackMapper;

    @Operation(summary = "查看教师反馈列表（按学期、教师筛选，含教师名、学期名）")
    @GetMapping
    public Result<List<FeedbackListVO>> list(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long semesterId) {
        return Result.success(feedbackMapper.selectFeedbackListWithNames(teacherId, semesterId));
    }
}
