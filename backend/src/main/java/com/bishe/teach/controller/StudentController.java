package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.Result;
import com.bishe.teach.dto.EvalSubmitDTO;
import com.bishe.teach.entity.EvalRecord;
import com.bishe.teach.entity.Semester;
import com.bishe.teach.entity.StudentInfo;
import com.bishe.teach.mapper.EvalRecordMapper;
import com.bishe.teach.mapper.SemesterMapper;
import com.bishe.teach.mapper.StudentInfoMapper;
import com.bishe.teach.mapper.TeacherFeedbackMapper;
import com.bishe.teach.security.LoginUser;
import com.bishe.teach.service.impl.EvaluationServiceImpl;
import com.bishe.teach.vo.FeedbackListVO;
import com.bishe.teach.vo.StudentEvalHistoryVO;
import com.bishe.teach.vo.StudentEvalRecordDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学生接口")
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final EvaluationServiceImpl   evalService;
    private final SemesterMapper          semesterMapper;
    private final EvalRecordMapper        evalRecordMapper;
    private final StudentInfoMapper       studentInfoMapper;
    private final TeacherFeedbackMapper   feedbackMapper;

    @Operation(summary = "提交评价")
    @PostMapping("/eval/submit")
    public Result<Void> submitEval(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody EvalSubmitDTO dto) {
        evalService.submitEvaluation(loginUser.getUserId(), dto);
        return Result.success("评价提交成功，感谢您的参与！", null);
    }

    @Operation(summary = "检查是否可以评价某教师")
    @GetMapping("/eval/can-evaluate")
    public Result<Boolean> canEvaluate(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long teacherId,
            @RequestParam Long semesterId) {
        return Result.success(evalService.canEvaluate(loginUser.getUserId(), teacherId, semesterId));
    }

    @Operation(summary = "获取我的评价历史")
    @GetMapping("/eval/history")
    public Result<List<EvalRecord>> myHistory(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(evalService.getStudentHistory(loginUser.getUserId()));
    }

    @Operation(summary = "获取所有学期（用于学生评价页学期切换）")
    @GetMapping("/semesters/current")
    public Result<List<Semester>> currentSemesters() {
        List<Semester> list = semesterMapper.selectList(
                new LambdaQueryWrapper<Semester>()
                        .orderByAsc(Semester::getStartDate));
        return Result.success(list);
    }

    @Operation(summary = "查看指定学期的评价历史（含教师工号、姓名、学期名称）")
    @GetMapping("/eval/history/by-semester")
    public Result<List<StudentEvalHistoryVO>> historyBySemester(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long semesterId) {
        return Result.success(
                evalRecordMapper.selectStudentSemesterHistory(loginUser.getUserId(), semesterId));
    }

    @Operation(summary = "获取学生已评价过的学期列表（含学期名称，用于我的评价页学期筛选）")
    @GetMapping("/eval/history/semesters")
    public Result<List<Semester>> evaluatedSemesters(@AuthenticationPrincipal LoginUser loginUser) {
        List<Long> ids = evalRecordMapper.selectEvaluatedSemesterIds(loginUser.getUserId());
        if (ids.isEmpty()) return Result.success(List.of());
        List<Semester> list = semesterMapper.selectBatchIds(ids);
        list.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return Result.success(list);
    }

    @Operation(summary = "获取全部评价历史（按学期分组，含教师工号、姓名、学期名称）")
    @GetMapping("/eval/history/all")
    public Result<List<StudentEvalHistoryVO>> allHistory(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(evalRecordMapper.selectStudentAllHistory(loginUser.getUserId()));
    }

    @Operation(summary = "查看自己对某教师某学期的评价作答详情")
    @GetMapping("/eval/record/detail")
    public Result<StudentEvalRecordDetailVO> evalRecordDetail(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long teacherId,
            @RequestParam Long semesterId) {
        StudentEvalRecordDetailVO vo = evalService.getStudentEvalRecordDetail(
                loginUser.getUserId(), teacherId, semesterId);
        return Result.success(vo);
    }

    @Operation(summary = "查看教师反馈（教过本班级的教师，按学期筛选）")
    @GetMapping("/feedback/list")
    public Result<List<FeedbackListVO>> feedbackList(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long semesterId) {
        StudentInfo stu = studentInfoMapper.selectOne(
                new LambdaQueryWrapper<StudentInfo>()
                        .eq(StudentInfo::getUserId, loginUser.getUserId()));
        int classNum = stu != null ? extractClassNumber(stu.getClassName()) : 1;
        return Result.success(feedbackMapper.selectFeedbackForStudent(semesterId, classNum, loginUser.getUserId()));
    }

    private int extractClassNumber(String className) {
        if (className == null || className.isEmpty()) return 1;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+)班").matcher(className);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }
}
