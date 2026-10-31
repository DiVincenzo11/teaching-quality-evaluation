package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.teach.common.PageResult;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.EvalQuestion;
import com.bishe.teach.entity.EvalKeyword;
import com.bishe.teach.entity.PreQuestionConfig;
import com.bishe.teach.entity.Semester;
import com.bishe.teach.mapper.EvalQuestionMapper;
import com.bishe.teach.mapper.EvalKeywordMapper;
import com.bishe.teach.mapper.PreQuestionConfigMapper;
import com.bishe.teach.mapper.SemesterMapper;
import com.bishe.teach.dto.DynamicWeightCalcDTO;
import com.bishe.teach.service.impl.DynamicWeightService;
import com.bishe.teach.service.impl.EvaluationServiceImpl;
import com.bishe.teach.service.impl.TeacherClusterService;
import com.bishe.teach.service.impl.TeachingReportService;
import com.bishe.teach.vo.TeacherEvalStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员-评价管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminEvalController {

    private final EvalQuestionMapper     questionMapper;
    private final EvalKeywordMapper      keywordMapper;
    private final SemesterMapper         semesterMapper;
    private final PreQuestionConfigMapper preQConfigMapper;
    private final EvaluationServiceImpl  evalService;
    private final DynamicWeightService   weightService;
    private final TeacherClusterService  clusterService;
    private final TeachingReportService reportService;

    // ===== 题目管理 =====

    @Operation(summary = "获取所有题目")
    @GetMapping("/questions")
    public Result<List<EvalQuestion>> listQuestions() {
        return Result.success(questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>().orderByAsc(EvalQuestion::getSortOrder)));
    }

    @Operation(summary = "新增单选题")
    @PostMapping("/questions")
    public Result<Void> addQuestion(@RequestBody EvalQuestion dto) {
        dto.setQuestionType(1);
        dto.setStatus(1);
        // 确定排序号和题目编号
        List<EvalQuestion> all = questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>().orderByAsc(EvalQuestion::getSortOrder));
        int choiceCount = (int) all.stream().filter(q -> q.getQuestionType() == 1).count();
        int maxSort = all.stream().mapToInt(q -> q.getSortOrder() != null ? q.getSortOrder() : 0).max().orElse(0);
        dto.setQuestionNo(choiceCount + 1);
        dto.setSortOrder(maxSort + 1);
        if (dto.getMaxScore() == null) dto.setMaxScore(BigDecimal.TEN);
        questionMapper.insert(dto);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改题目内容/分值/状态")
    @PutMapping("/questions/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody EvalQuestion dto) {
        EvalQuestion q = questionMapper.selectById(id);
        if (q == null) return Result.error("题目不存在");
        if (dto.getMaxScore() != null) q.setMaxScore(dto.getMaxScore());
        if (dto.getQuestionContent() != null) q.setQuestionContent(dto.getQuestionContent());
        if (dto.getStatus() != null) q.setStatus(dto.getStatus());
        questionMapper.updateById(q);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除单选题")
    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        EvalQuestion q = questionMapper.selectById(id);
        if (q == null) return Result.error("题目不存在");
        if (q.getQuestionType() == 2) return Result.error("简答题不可删除");
        questionMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "自动均衡分配单选题分数（总分100，简答题可设定）")
    @PutMapping("/questions/rebalance")
    public Result<Void> rebalanceScores(@RequestBody Map<String, Integer> params) {
        int essayScore = params.getOrDefault("essayScore", 20);
        if (essayScore < 0 || essayScore > 90) return Result.error("简答题分值范围 0-90");

        List<EvalQuestion> all = questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>()
                        .eq(EvalQuestion::getStatus, 1)
                        .orderByAsc(EvalQuestion::getSortOrder));

        List<EvalQuestion> choices = all.stream().filter(q -> q.getQuestionType() == 1).toList();
        List<EvalQuestion> essays  = all.stream().filter(q -> q.getQuestionType() == 2).toList();

        if (choices.isEmpty()) return Result.error("没有启用的单选题");

        BigDecimal choiceTotal = BigDecimal.valueOf(100 - essayScore);
        BigDecimal perChoice = choiceTotal.divide(
                BigDecimal.valueOf(choices.size()), 2, RoundingMode.HALF_UP);

        for (EvalQuestion q : choices) {
            q.setMaxScore(perChoice);
            questionMapper.updateById(q);
        }
        for (EvalQuestion q : essays) {
            q.setMaxScore(BigDecimal.valueOf(essayScore));
            questionMapper.updateById(q);
        }
        return Result.success("分数重新分配成功", null);
    }

    // ===== 前置问题权重配置 =====

    @Operation(summary = "获取前置问题权重配置")
    @GetMapping("/pre-question-config")
    public Result<List<PreQuestionConfig>> getPreQuestionConfig() {
        return Result.success(preQConfigMapper.selectList(
                new LambdaQueryWrapper<PreQuestionConfig>()
                        .orderByAsc(PreQuestionConfig::getQuestionKey)
                        .orderByAsc(PreQuestionConfig::getOptionValue)));
    }

    @Operation(summary = "批量更新前置问题权重配置")
    @PutMapping("/pre-question-config")
    public Result<Void> updatePreQuestionConfig(@RequestBody List<PreQuestionConfig> configs) {
        for (PreQuestionConfig c : configs) {
            if (c.getId() == null) continue;
            BigDecimal w = c.getWeight();
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                return Result.error("权重值必须在 0.00 ~ 1.00 之间");
            }
            preQConfigMapper.updateById(c);
        }
        evalService.refreshPreWeightCache();
        return Result.success("权重配置已更新", null);
    }

    // ===== 关键词管理 =====

    @Operation(summary = "获取关键词列表")
    @GetMapping("/keywords")
    public Result<List<EvalKeyword>> listKeywords(@RequestParam(required = false) Integer wordType) {
        LambdaQueryWrapper<EvalKeyword> wrapper = new LambdaQueryWrapper<EvalKeyword>()
                .eq(wordType != null, EvalKeyword::getWordType, wordType)
                .orderByAsc(EvalKeyword::getWordType)
                .orderByAsc(EvalKeyword::getId);
        return Result.success(keywordMapper.selectList(wrapper));
    }

    @Operation(summary = "新增关键词")
    @PostMapping("/keywords")
    public Result<Void> addKeyword(@RequestBody EvalKeyword keyword) {
        keywordMapper.insert(keyword);
        evalService.refreshKeywordCache();
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改关键词")
    @PutMapping("/keywords/{id}")
    public Result<Void> updateKeyword(@PathVariable Long id, @RequestBody EvalKeyword dto) {
        dto.setId(id);
        keywordMapper.updateById(dto);
        evalService.refreshKeywordCache();
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除关键词")
    @DeleteMapping("/keywords/{id}")
    public Result<Void> deleteKeyword(@PathVariable Long id) {
        keywordMapper.deleteById(id);
        evalService.refreshKeywordCache();
        return Result.success("删除成功", null);
    }

    // ===== 学期管理 =====

    @Operation(summary = "获取学期列表")
    @GetMapping("/semesters")
    public Result<List<Semester>> listSemesters() {
        return Result.success(semesterMapper.selectList(
                new LambdaQueryWrapper<Semester>().orderByAsc(Semester::getStartDate)));
    }

    @Operation(summary = "新增学期")
    @PostMapping("/semesters")
    public Result<Void> addSemester(@RequestBody Semester semester) {
        semesterMapper.insert(semester);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改学期")
    @PutMapping("/semesters/{id}")
    public Result<Void> updateSemester(@PathVariable Long id, @RequestBody Semester dto) {
        dto.setId(id);
        semesterMapper.updateById(dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "设置当前学期")
    @PutMapping("/semesters/{id}/set-current")
    public Result<Void> setCurrentSemester(@PathVariable Long id) {
        // 清除所有is_current
        Semester clear = new Semester();
        clear.setIsCurrent(0);
        semesterMapper.update(clear, new LambdaQueryWrapper<Semester>().eq(Semester::getIsCurrent, 1));
        // 设置新的当前学期
        Semester current = semesterMapper.selectById(id);
        if (current != null) {
            current.setIsCurrent(1);
            semesterMapper.updateById(current);
        }
        return Result.success("设置成功", null);
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/semesters/{id}")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        semesterMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ===== 数据分析 =====

    @Operation(summary = "获取教师评分排行（按学期）")
    @GetMapping("/analysis/teacher-stats")
    public Result<List<TeacherEvalStatsVO>> teacherStats(
            @RequestParam(required = false) Long semesterId) {
        return Result.success(evalService.getTeacherStatsList(semesterId));
    }

    @Operation(summary = "获取某教师某学期详细评价")
    @GetMapping("/analysis/teacher/{teacherId}")
    public Result<Map<String, Object>> teacherDetail(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long semesterId) {
        Long sid = semesterId != null ? semesterId : getCurrentSemesterId();
        return Result.success(evalService.getTeacherEvalView(teacherId, sid));
    }

    @Operation(summary = "获取某教师教学质量分析报告（含排名、优劣势、改进建议）")
    @GetMapping("/analysis/teacher/{teacherId}/report")
    public Result<Map<String, Object>> teacherReport(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long semesterId) {
        Long sid = semesterId != null ? semesterId : getCurrentSemesterId();
        return Result.success(reportService.generateReport(teacherId, sid));
    }

    @Operation(summary = "动态权重计算（熵权法）—— 从数据库读取")
    @GetMapping("/analysis/dynamic-weights")
    public Result<List<Map<String, Object>>> dynamicWeights(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer courseType) {
        return Result.success(weightService.calcDynamicWeights(semesterId, courseType));
    }

    @Operation(summary = "动态权重计算（熵权法）—— 直接传入分数矩阵（测试用）")
    @PostMapping("/analysis/dynamic-weights")
    public Result<List<Map<String, Object>>> dynamicWeightsFromData(
            @RequestBody DynamicWeightCalcDTO dto) {
        return Result.success(weightService.calcFromInputData(
                dto.getScores(), dto.getMaxScore(), dto.getCourseType()));
    }

    @Operation(summary = "教师画像聚类分析（K-Means）")
    @GetMapping("/analysis/teacher-clusters")
    public Result<Map<String, Object>> teacherClusters(
            @RequestParam(required = false) Long semesterId) {
        List<TeacherEvalStatsVO> stats = evalService.getTeacherStatsList(semesterId);
        return Result.success(clusterService.clusterTeachers(stats));
    }

    // ===== 异常数据智能处理 =====

    @Operation(summary = "重新计算评价权重并剔除异常数据")
    @PostMapping("/analysis/recalculate-weights")
    public Result<Map<String, Object>> recalculateWeights(
            @RequestParam(required = false) Long semesterId) {
        return Result.success(evalService.recalculateWeights(semesterId));
    }

    @Operation(summary = "获取参与度分析统计")
    @GetMapping("/analysis/engagement-stats")
    public Result<List<Map<String, Object>>> engagementStats(
            @RequestParam(required = false) Long semesterId) {
        return Result.success(evalService.getEngagementStats(semesterId));
    }

    private Long getCurrentSemesterId() {
        Semester current = semesterMapper.selectOne(
                new LambdaQueryWrapper<Semester>().eq(Semester::getIsCurrent, 1));
        return current != null ? current.getId() : 1L;
    }
}
