package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.Semester;
import com.bishe.teach.entity.StudentInfo;
import com.bishe.teach.entity.TeacherFeedback;
import com.bishe.teach.entity.TeacherInfo;
import com.bishe.teach.entity.TeacherSemester;
import com.bishe.teach.mapper.SemesterMapper;
import com.bishe.teach.mapper.StudentInfoMapper;
import com.bishe.teach.mapper.SysUserMapper;
import com.bishe.teach.mapper.TeacherFeedbackMapper;
import com.bishe.teach.mapper.TeacherInfoMapper;
import com.bishe.teach.mapper.TeacherSemesterMapper;
import com.bishe.teach.security.LoginUser;
import com.bishe.teach.service.impl.EvaluationServiceImpl;
import com.bishe.teach.service.impl.TeachingReportService;
import com.bishe.teach.vo.TeacherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "教师接口")
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final EvaluationServiceImpl  evalService;
    private final TeachingReportService  reportService;
    private final TeacherFeedbackMapper  feedbackMapper;
    private final SemesterMapper         semesterMapper;
    private final TeacherInfoMapper      teacherInfoMapper;
    private final TeacherSemesterMapper  teacherSemesterMapper;
    private final StudentInfoMapper      studentInfoMapper;
    private final SysUserMapper          userMapper;

    @Operation(summary = "获取我的教师信息（含所教科目、所教班级）")
    @GetMapping("/info")
    public Result<Map<String, Object>> myInfo(@AuthenticationPrincipal LoginUser loginUser,
                                              @RequestParam(required = false) Long semesterId) {
        var teacher = userMapper.selectById(loginUser.getUserId());
        var info = teacherInfoMapper.selectOne(
                new LambdaQueryWrapper<TeacherInfo>().eq(TeacherInfo::getUserId, loginUser.getUserId()));
        var tsList = teacherSemesterMapper.selectList(
                new LambdaQueryWrapper<TeacherSemester>()
                        .eq(TeacherSemester::getTeacherId, loginUser.getUserId())
                        .and(semesterId != null, w -> w.eq(TeacherSemester::getSemesterId, semesterId)));
        var coursesClasses = buildCoursesAndClasses(tsList);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("realName", teacher != null ? teacher.getRealName() : "");
        result.put("department", info != null ? info.getDepartment() : "");
        result.put("title", info != null ? info.getTitle() : "");
        result.put("courses", coursesClasses.get("courses"));
        result.put("classes", coursesClasses.get("classes"));
        return Result.success(result);
    }

    /** 从 teacher_semester 构建所教科目列表和班级字符串（如 22软工1班、22软工2班） */
    private Map<String, Object> buildCoursesAndClasses(List<TeacherSemester> tsList) {
        Set<String> courses = new LinkedHashSet<>();
        Set<Integer> classNums = new TreeSet<>();
        if (tsList != null) {
            for (TeacherSemester ts : tsList) {
                if (ts.getCourseName() != null && !ts.getCourseName().isBlank()) {
                    courses.add(ts.getCourseName());
                }
                int start = ts.getClassStart() != null ? ts.getClassStart() : 1;
                int end = ts.getClassEnd() != null ? ts.getClassEnd() : start;
                for (int n = start; n <= end; n++) classNums.add(n);
            }
        }
        String classesStr = classNums.stream()
                .map(n -> "22软工" + n + "班")
                .collect(Collectors.joining("、"));
        return Map.of("courses", new ArrayList<>(courses), "classes", classesStr);
    }

    @Operation(summary = "查看我的评价统计（匿名）")
    @GetMapping("/eval/stats")
    public Result<Map<String, Object>> myEvalStats(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) Long semesterId) {
        Long sid = semesterId != null ? semesterId : getCurrentSemesterId();
        return Result.success(evalService.getTeacherEvalView(loginUser.getUserId(), sid));
    }

    @Operation(summary = "获取我的授课学期列表（仅展示该教师实际授课的学期）")
    @GetMapping("/eval/semesters")
    public Result<List<Semester>> evalSemesters(@AuthenticationPrincipal LoginUser loginUser) {
        // 从 teacher_semester 获取该教师授课的学期 ID，只返回这些学期
        var tsList = teacherSemesterMapper.selectList(
                new LambdaQueryWrapper<TeacherSemester>()
                        .eq(TeacherSemester::getTeacherId, loginUser.getUserId())
                        .select(TeacherSemester::getSemesterId));
        if (tsList == null || tsList.isEmpty()) {
            return Result.success(List.of());
        }
        var semesterIds = tsList.stream()
                .map(TeacherSemester::getSemesterId)
                .distinct()
                .toList();
        List<Semester> list = semesterMapper.selectBatchIds(semesterIds);
        list.sort((a, b) -> (a.getStartDate() != null && b.getStartDate() != null)
                ? a.getStartDate().compareTo(b.getStartDate()) : 0);
        return Result.success(list);
    }

    @Operation(summary = "提交/更新反馈")
    @PostMapping("/feedback")
    public Result<Void> saveFeedback(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody TeacherFeedback dto) {
        dto.setTeacherId(loginUser.getUserId());
        TeacherFeedback existing = feedbackMapper.selectOne(
                new LambdaQueryWrapper<TeacherFeedback>()
                        .eq(TeacherFeedback::getTeacherId, loginUser.getUserId())
                        .eq(TeacherFeedback::getSemesterId, dto.getSemesterId()));
        if (existing != null) {
            existing.setReflection(dto.getReflection());
            existing.setImprovementPlan(dto.getImprovementPlan());
            feedbackMapper.updateById(existing);
        } else {
            feedbackMapper.insert(dto);
        }
        return Result.success("提交成功", null);
    }

    @Operation(summary = "获取我的反馈列表")
    @GetMapping("/feedback")
    public Result<List<TeacherFeedback>> myFeedback(@AuthenticationPrincipal LoginUser loginUser) {
        List<TeacherFeedback> list = feedbackMapper.selectList(
                new LambdaQueryWrapper<TeacherFeedback>()
                        .eq(TeacherFeedback::getTeacherId, loginUser.getUserId())
                        .orderByDesc(TeacherFeedback::getCreateTime));
        return Result.success(list);
    }

    @Operation(summary = "生成教学能力提升报告")
    @GetMapping("/report")
    public Result<Map<String, Object>> teachingReport(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) Long semesterId) {
        Long sid = semesterId != null ? semesterId : getCurrentSemesterId();
        return Result.success(reportService.generateReport(loginUser.getUserId(), sid));
    }

    @Operation(summary = "获取教师列表（管理员使用，可按学期筛选）")
    @GetMapping("/list")
    public Result<List<TeacherVO>> teacherList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long   semesterId) {
        if (semesterId != null) {
            return Result.success(teacherInfoMapper.selectTeacherListBySemester(semesterId, keyword, department));
        }
        return Result.success(teacherInfoMapper.selectTeacherList(keyword, department));
    }

    @Operation(summary = "获取学生可评教师列表（按学期+所在班级筛选）")
    @GetMapping("/list/for-student")
    public Result<List<TeacherVO>> teacherListForStudent(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam Long semesterId,
            @RequestParam(required = false) String keyword) {
        StudentInfo stu = studentInfoMapper.selectOne(
                new LambdaQueryWrapper<StudentInfo>()
                        .eq(StudentInfo::getUserId, loginUser.getUserId()));
        int classNum = stu != null ? extractClassNumber(stu.getClassName()) : 1;
        return Result.success(teacherInfoMapper.selectTeacherListForStudent(semesterId, classNum, keyword));
    }

    /** 从班级名称中提取班号，如 "软件工程2班" → 2 */
    private int extractClassNumber(String className) {
        if (className == null || className.isEmpty()) return 1;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+)班").matcher(className);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    private Long getCurrentSemesterId() {
        Semester current = semesterMapper.selectOne(
                new LambdaQueryWrapper<Semester>().eq(Semester::getIsCurrent, 1));
        return current != null ? current.getId() : 1L;
    }
}
