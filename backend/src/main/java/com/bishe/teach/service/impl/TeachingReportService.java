package com.bishe.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.entity.EvalRecord;
import com.bishe.teach.entity.SysUser;
import com.bishe.teach.entity.TeacherInfo;
import com.bishe.teach.entity.TeacherSemester;
import com.bishe.teach.mapper.*;
import com.bishe.teach.vo.QuestionAvgScoreVO;
import com.bishe.teach.vo.TeacherEvalStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教学能力提升报告生成服务
 * 结合知识图谱推理，诊断教师短板并推荐改进资源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingReportService {

    private final EvalRecordMapper       recordMapper;
    private final EvalAnswerMapper       answerMapper;
    private final SysUserMapper          userMapper;
    private final TeacherInfoMapper      teacherInfoMapper;
    private final TeacherSemesterMapper  teacherSemesterMapper;
    private final SemesterMapper        semesterMapper;

    // ===== 知识图谱节点定义 =====

    private static final Map<String, Map<String, Object>> PROBLEM_STRATEGIES;
    static {
        PROBLEM_STRATEGIES = new LinkedHashMap<>();

        PROBLEM_STRATEGIES.put("教学内容与方法", Map.of(
                "threshold", 0.70,
                "strategies", List.of("深化课程内容研究", "引入前沿学术成果", "增加实际案例与应用场景", "参加课程建设培训"),
                "resources", List.of(
                        Map.of("title", "B站·大学教学方法与技巧", "url", "https://search.bilibili.com/all?keyword=%E5%A4%A7%E5%AD%A6%E6%95%99%E5%AD%A6%E6%96%B9%E6%B3%95%E4%B8%8E%E6%8A%80%E5%B7%A7", "type", "B站"),
                        Map.of("title", "雨课堂·课程设计与建设", "url", "https://www.yuketang.cn/", "type", "雨课堂"),
                        Map.of("title", "中国大学MOOC·课程内容与方法", "url", "https://www.icourse163.org/search.htm#keyword=%E6%95%99%E5%AD%A6%E5%86%85%E5%AE%B9%E4%B8%8E%E6%96%B9%E6%B3%95", "type", "MOOC"),
                        Map.of("title", "学习通·课程建设资源", "url", "https://i.chaoxing.com/", "type", "学习通"),
                        Map.of("title", "B站·课程内容设计与改革", "url", "https://search.bilibili.com/all?keyword=%E8%AF%BE%E7%A8%8B%E5%86%85%E5%AE%B9%E8%AE%BE%E8%AE%A1%E6%94%B9%E9%9D%A9", "type", "文献"),
                        Map.of("title", "B站·行业前沿技术讲解", "url", "https://search.bilibili.com/all?keyword=%E8%A1%8C%E4%B8%9A%E5%89%8D%E6%B2%BF%E6%8A%80%E6%9C%AF", "type", "案例")
                )
        ));

        PROBLEM_STRATEGIES.put("教学态度与互动", Map.of(
                "threshold", 0.65,
                "strategies", List.of("强化职业素养培训", "参加师德师风专题研讨", "建立学生反馈收集机制", "增加课堂互动"),
                "resources", List.of(
                        Map.of("title", "B站·高校教师课堂互动技巧", "url", "https://search.bilibili.com/all?keyword=%E9%AB%98%E6%A0%A1%E6%95%99%E5%B8%88%E8%AF%BE%E5%A0%82%E4%BA%92%E5%8A%A8%E6%8A%80%E5%B7%A7", "type", "B站"),
                        Map.of("title", "雨课堂·师生互动功能指南", "url", "https://www.yuketang.cn/", "type", "雨课堂"),
                        Map.of("title", "MOOC·教师职业素养提升", "url", "https://www.icourse163.org/search.htm#keyword=%E6%95%99%E5%B8%88%E8%81%8C%E4%B8%9A%E7%B4%A0%E5%85%BB%E6%8F%90%E5%8D%87", "type", "MOOC"),
                        Map.of("title", "学习通·互动教学工具", "url", "https://i.chaoxing.com/", "type", "学习通"),
                        Map.of("title", "B站·师德师风建设专题", "url", "https://search.bilibili.com/all?keyword=%E5%B8%88%E5%BE%B7%E5%B8%88%E9%A3%8E%E5%BB%BA%E8%AE%BE", "type", "文献"),
                        Map.of("title", "B站·优秀教师教学案例", "url", "https://search.bilibili.com/all?keyword=%E4%BC%98%E7%A7%80%E6%95%99%E5%B8%88%E6%95%99%E5%AD%A6%E6%A1%88%E4%BE%8B", "type", "案例")
                )
        ));

        PROBLEM_STRATEGIES.put("教学效果与收获", Map.of(
                "threshold", 0.70,
                "strategies", List.of("强化能力培养导向", "优化学习目标达成", "激发学生课外拓展", "系统梳理教学全流程"),
                "resources", List.of(
                        Map.of("title", "B站·教学质量提升方法", "url", "https://search.bilibili.com/all?keyword=%E6%95%99%E5%AD%A6%E8%B4%A8%E9%87%8F%E6%8F%90%E5%8D%87%E6%96%B9%E6%B3%95", "type", "B站"),
                        Map.of("title", "雨课堂·教学效果分析工具", "url", "https://www.yuketang.cn/", "type", "雨课堂"),
                        Map.of("title", "MOOC·学习效果与能力培养", "url", "https://www.icourse163.org/search.htm#keyword=%E5%AD%A6%E4%B9%A0%E6%95%88%E6%9E%9C%E4%B8%8E%E8%83%BD%E5%8A%9B%E5%9F%B9%E5%85%BB", "type", "MOOC"),
                        Map.of("title", "学习通·学情数据分析", "url", "https://i.chaoxing.com/", "type", "学习通"),
                        Map.of("title", "B站·卓越教学与学生培养", "url", "https://search.bilibili.com/all?keyword=%E5%8D%93%E8%B6%8A%E6%95%99%E5%AD%A6%E4%B8%8E%E5%AD%A6%E7%94%9F%E5%9F%B9%E5%85%BB", "type", "文献"),
                        Map.of("title", "B站·课堂教学观摩与反思", "url", "https://search.bilibili.com/all?keyword=%E8%AF%BE%E5%A0%82%E6%95%99%E5%AD%A6%E8%A7%82%E6%91%A9%E4%B8%8E%E5%8F%8D%E6%80%9D", "type", "案例")
                )
        ));
    }

    public Map<String, Object> generateReport(Long teacherId, Long semesterId) {
        SysUser teacher = userMapper.selectById(teacherId);
        TeacherInfo info = teacherInfoMapper.selectOne(
                new LambdaQueryWrapper<TeacherInfo>().eq(TeacherInfo::getUserId, teacherId));

        // 获取该教师所有单选题平均分
        List<QuestionAvgScoreVO> qavg = answerMapper.selectAvgScoreByTeacherSemester(teacherId, semesterId);

        // 计算各维度得分率
        Map<String, Double> dimensionScores = calcDimensionScores(qavg);

        // 获取简答题评语
        List<EvalRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getTeacherId, teacherId)
                        .eq(EvalRecord::getSemesterId, semesterId)
                        .eq(EvalRecord::getStatus, 1));

        double avgTotal = records.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToDouble(r -> r.getTotalScore().doubleValue())
                .average().orElse(0);

        List<String> comments = records.stream()
                .map(EvalRecord::getEssayContent)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toList());

        // 诊断短板
        List<Map<String, Object>> weaknesses = diagnoseWeaknesses(dimensionScores, qavg);

        // 优势维度
        List<String> strengths = identifyStrengths(dimensionScores, qavg);

        // 知识图谱推荐
        List<Map<String, Object>> graphRecommendations = buildGraphRecommendations(weaknesses);

        // 与全体教师对比（从统计中获取）
        List<TeacherEvalStatsVO> allStats = recordMapper.selectTeacherStats(semesterId);
        double allAvg = allStats.stream()
                .filter(s -> s.getAvgScore() != null)
                .mapToDouble(s -> s.getAvgScore().doubleValue())
                .average().orElse(0);
        int rank = 1;
        for (TeacherEvalStatsVO s : allStats) {
            if (s.getAvgScore() != null && s.getAvgScore().doubleValue() > avgTotal) rank++;
        }
        // 若当前教师不在统计列表中（无评价记录），计入总数并置为末位
        boolean teacherInStats = allStats.stream().anyMatch(s -> teacherId.equals(s.getTeacherId()));
        int totalTeachers = teacherInStats ? allStats.size() : allStats.size() + 1;
        if (!teacherInStats) rank = totalTeachers;
        rank = Math.min(rank, Math.max(totalTeachers, 1));

        // 该学期所教科目与班级
        var tsList = teacherSemesterMapper.selectList(
                new LambdaQueryWrapper<TeacherSemester>()
                        .eq(TeacherSemester::getTeacherId, teacherId)
                        .eq(TeacherSemester::getSemesterId, semesterId));
        var coursesClasses = buildCoursesAndClasses(tsList);

        // 构建报告
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("teacherName", teacher != null ? teacher.getRealName() : "未知");
        report.put("department", info != null ? info.getDepartment() : "");
        report.put("title", info != null ? info.getTitle() : "");
        report.put("courses", coursesClasses.get("courses"));
        report.put("classes", coursesClasses.get("classes"));
        report.put("semesterId", semesterId);
        report.put("avgScore", round(avgTotal));
        report.put("evalCount", records.size());
        report.put("allAvgScore", round(allAvg));
        report.put("rank", rank);
        report.put("totalTeachers", totalTeachers);
        report.put("strengths", strengths);
        report.put("weaknesses", weaknesses);
        report.put("dimensionScores", dimensionScores);
        report.put("questionAvg", qavg);
        report.put("graphRecommendations", graphRecommendations);
        report.put("comments", comments);
        report.put("overallLevel", getOverallLevel(avgTotal));
        report.put("improvementPriority", weaknesses.stream().map(w -> w.get("dimension")).limit(3).collect(Collectors.toList()));

        return report;
    }

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

    private Map<String, Double> calcDimensionScores(List<QuestionAvgScoreVO> qavg) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (qavg == null || qavg.isEmpty()) return scores;

        // 按 questionNo 范围分组：一(1-4)、二(5-8)、三(9-12)
        double[] d1 = getRatioByNo(qavg, 1, 4);
        double[] d2 = getRatioByNo(qavg, 5, 8);
        double[] d3 = getRatioByNo(qavg, 9, 12);

        if (d1[1] > 0) scores.put("教学内容与方法", d1[1] > 0 ? round(d1[0] / d1[1]) : 0);
        if (d2[1] > 0) scores.put("教学态度与互动", d2[1] > 0 ? round(d2[0] / d2[1]) : 0);
        if (d3[1] > 0) scores.put("教学效果与收获", d3[1] > 0 ? round(d3[0] / d3[1]) : 0);
        return scores;
    }

    /** 按 questionNo 范围统计 [sumAvg, sumMax] */
    private double[] getRatioByNo(List<QuestionAvgScoreVO> qavg, int noFrom, int noTo) {
        double sumAvg = 0, sumMax = 0;
        for (QuestionAvgScoreVO q : qavg) {
            int no = q.getQuestionNo() != null ? q.getQuestionNo() : 0;
            if (no >= noFrom && no <= noTo) {
                sumAvg += q.getAvgScore() != null ? q.getAvgScore().doubleValue() : 0;
                sumMax += q.getMaxScore() != null ? q.getMaxScore().doubleValue() : 10;
            }
        }
        return new double[]{sumAvg, sumMax};
    }

    // 统一的提升目标阈值：低于此值均视为有提升空间
    private static final double IMPROVEMENT_THRESHOLD = 0.85;

    private List<Map<String, Object>> diagnoseWeaknesses(
            Map<String, Double> dimensionScores, List<QuestionAvgScoreVO> qavg) {

        List<Map<String, Object>> weaknesses = new ArrayList<>();
        for (Map.Entry<String, Double> e : dimensionScores.entrySet()) {
            String dim = e.getKey();
            double ratio = e.getValue();
            Map<String, Object> graphNode = PROBLEM_STRATEGIES.get(dim);
            if (graphNode == null) continue;

            double originalThreshold = ((Number) graphNode.get("threshold")).doubleValue();
            // 使用更高的统一目标阈值，确保中等水平教师也能看到提升方向
            double targetThreshold = IMPROVEMENT_THRESHOLD;

            if (ratio < targetThreshold) {
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("dimension", dim);
                w.put("scoreRatio", round(ratio * 100));
                w.put("threshold", round(targetThreshold * 100));
                w.put("gap", round((targetThreshold - ratio) * 100));
                String severity;
                if (ratio < 0.50) severity = "严重";
                else if (ratio < 0.65) severity = "中等";
                else if (ratio < originalThreshold) severity = "轻微";
                else severity = "提升空间";
                w.put("severity", severity);
                weaknesses.add(w);
            }
        }

        weaknesses.sort((a, b) -> Double.compare(
                ((Number) b.get("gap")).doubleValue(),
                ((Number) a.get("gap")).doubleValue()));
        return weaknesses;
    }

    private List<String> identifyStrengths(Map<String, Double> dimensionScores, List<QuestionAvgScoreVO> qavg) {
        // 优先展示绝对优势（≥85%），其次展示良好表现（≥75%），最后展示相对最优维度
        List<Map.Entry<String, Double>> sorted = dimensionScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<String> strengths = sorted.stream()
                .filter(e -> e.getValue() >= 0.75)
                .map(e -> {
                    double r = e.getValue();
                    String label = r >= 0.85 ? "表现突出" : r >= 0.80 ? "表现优秀" : "表现良好";
                    return e.getKey() + "（得分率: " + round(r * 100) + "%，" + label + "）";
                })
                .collect(Collectors.toList());

        // 若所有维度均低于75%，取得分最高的维度作为相对优势
        if (strengths.isEmpty() && !sorted.isEmpty()) {
            Map.Entry<String, Double> best = sorted.get(0);
            strengths.add(best.getKey() + "（得分率: " + round(best.getValue() * 100) + "%，相对最优，建议重点保持并提升）");
        }
        return strengths;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildGraphRecommendations(List<Map<String, Object>> weaknesses) {
        List<Map<String, Object>> recs = new ArrayList<>();
        for (Map<String, Object> w : weaknesses) {
            String dim = (String) w.get("dimension");
            Map<String, Object> graphNode = PROBLEM_STRATEGIES.get(dim);
            if (graphNode == null) continue;

            String severity = (String) w.get("severity");
            boolean isImproveOnly = "提升空间".equals(severity);

            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("problem", dim);
            rec.put("severity", severity);
            rec.put("scoreRatio", w.get("scoreRatio"));
            rec.put("strategies", graphNode.get("strategies"));
            rec.put("resources", graphNode.get("resources"));
            String firstStep = isImproveOnly ? dim + "持续优化" : dim + "不足";
            String lastStep  = isImproveOnly ? "精益求精" : "能力提升";
            rec.put("graphPath", List.of(
                    firstStep,
                    "诊断分析",
                    ((List<String>) graphNode.get("strategies")).get(0),
                    lastStep
            ));
            recs.add(rec);
        }
        return recs;
    }

    private String getOverallLevel(double score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 70) return "中等";
        if (score >= 60) return "及格";
        return "待提升";
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
