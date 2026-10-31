package com.bishe.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.BusinessException;
import com.bishe.teach.dto.EvalSubmitDTO;
import com.bishe.teach.entity.*;
import com.bishe.teach.entity.PreQuestionConfig;
import com.bishe.teach.mapper.*;
import com.bishe.teach.entity.TeacherSemester;
import com.bishe.teach.util.SemanticAnalysisUtil;
import com.bishe.teach.vo.EvalDetailVO;
import com.bishe.teach.vo.QuestionAvgScoreVO;
import com.bishe.teach.vo.StudentEvalRecordDetailVO;
import com.bishe.teach.vo.TeacherEvalStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl {

    private final EvalRecordMapper       recordMapper;
    private final EvalAnswerMapper       answerMapper;
    private final EvalQuestionMapper     questionMapper;
    private final EvalKeywordMapper      keywordMapper;
    private final SemesterMapper         semesterMapper;
    private final SysUserMapper          userMapper;
    private final PreQuestionConfigMapper preQConfigMapper;
    private final TeacherSemesterMapper  teacherSemesterMapper;
    private final SemanticAnalysisUtil   semanticUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Lazy
    @Autowired
    private AbsaAnalysisService absaService;

    @Transactional
    public void submitEvaluation(Long studentId, EvalSubmitDTO dto) {
        // 只有当前学期（is_current=1）才允许提交评价
        Semester semester = semesterMapper.selectById(dto.getSemesterId());
        if (semester == null || semester.getIsCurrent() == null || semester.getIsCurrent() != 1) {
            throw new BusinessException(400, "只能对当前学期的教师进行评价");
        }

        // 防重复评价
        int count = recordMapper.countByStudentTeacherSemester(
                studentId, dto.getTeacherId(), dto.getSemesterId());
        if (count > 0) {
            throw new BusinessException(400, "您已对该教师完成本学期评价，不能重复提交");
        }

        // 加载所有题目
        List<EvalQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>()
                        .eq(EvalQuestion::getStatus, 1)
                        .orderByAsc(EvalQuestion::getSortOrder));
        Map<Long, EvalQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(EvalQuestion::getId, q -> q));

        // 加载关键词（优先从Redis缓存）
        List<String> positiveWords = getKeywordsFromCache(1);
        List<String> negativeWords = getKeywordsFromCache(3);

        // 计算单选题得分
        BigDecimal choiceScore = BigDecimal.ZERO;
        List<EvalAnswer> answers = new ArrayList<>();

        for (EvalSubmitDTO.AnswerItem item : dto.getAnswers()) {
            EvalQuestion q = questionMap.get(item.getQuestionId());
            if (q == null || q.getQuestionType() != 1) continue;
            BigDecimal score = semanticUtil.calcChoiceScore(item.getAnswerValue(), q.getMaxScore());
            choiceScore = choiceScore.add(score);

            EvalAnswer ans = new EvalAnswer();
            ans.setQuestionId(item.getQuestionId());
            ans.setAnswerValue(item.getAnswerValue());
            ans.setScore(score);
            answers.add(ans);
        }

        // 计算简答题得分
        EvalQuestion essayQ = questions.stream()
                .filter(q -> q.getQuestionType() == 2).findFirst().orElse(null);
        BigDecimal essayScore = BigDecimal.ZERO;
        if (essayQ != null && dto.getEssayContent() != null) {
            essayScore = semanticUtil.calcEssayScore(
                    dto.getEssayContent(), positiveWords, negativeWords, essayQ.getMaxScore());
        }

        BigDecimal totalScore = choiceScore.add(essayScore)
                .setScale(2, RoundingMode.HALF_UP);

        // 计算参与度权重
        BigDecimal engagementWeight = calcEngagementWeight(dto.getAttendanceRate(), dto.getStudyTime());

        // 保存评价记录
        EvalRecord record = new EvalRecord();
        record.setStudentId(studentId);
        record.setTeacherId(dto.getTeacherId());
        record.setSemesterId(dto.getSemesterId());
        record.setTotalScore(totalScore);
        record.setChoiceScore(choiceScore.setScale(2, RoundingMode.HALF_UP));
        record.setEssayScore(essayScore.setScale(2, RoundingMode.HALF_UP));
        record.setEssayContent(dto.getEssayContent());
        record.setAttendanceRate(dto.getAttendanceRate());
        record.setStudyTime(dto.getStudyTime());
        record.setEvalWeight(engagementWeight);
        record.setIsExcluded(0);
        record.setStatus(1);
        recordMapper.insert(record);

        // 保存答案
        for (EvalAnswer ans : answers) {
            ans.setRecordId(record.getId());
            answerMapper.insert(ans);
        }

        // 清除教师统计缓存
        redisTemplate.delete("teacher:stats:v2:" + dto.getTeacherId() + ":" + dto.getSemesterId());
    }

    public boolean canEvaluate(Long studentId, Long teacherId, Long semesterId) {
        Semester semester = semesterMapper.selectById(semesterId);
        // 只有当前学期（is_current=1）才允许评价
        if (semester == null || semester.getIsCurrent() == null || semester.getIsCurrent() != 1) return false;
        int count = recordMapper.countByStudentTeacherSemester(studentId, teacherId, semesterId);
        return count == 0;
    }

    public List<EvalRecord> getStudentHistory(Long studentId) {
        return recordMapper.selectList(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getStudentId, studentId)
                        .eq(EvalRecord::getStatus, 1)
                        .orderByDesc(EvalRecord::getCreateTime));
    }

    /**
     * 学生查看自己某次评价的作答详情（含各题选项与得分）
     */
    public StudentEvalRecordDetailVO getStudentEvalRecordDetail(Long studentId, Long teacherId, Long semesterId) {
        EvalRecord record = recordMapper.selectOne(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getStudentId, studentId)
                        .eq(EvalRecord::getTeacherId, teacherId)
                        .eq(EvalRecord::getSemesterId, semesterId)
                        .eq(EvalRecord::getStatus, 1));
        if (record == null) return null;

        SysUser teacher = userMapper.selectById(teacherId);
        Semester semester = semesterMapper.selectById(semesterId);
        List<EvalAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<EvalAnswer>().eq(EvalAnswer::getRecordId, record.getId()));

        List<StudentEvalRecordDetailVO.AnswerItem> items = new ArrayList<>();
        for (EvalAnswer a : answers) {
            EvalQuestion q = questionMapper.selectById(a.getQuestionId());
            if (q == null) continue;
            StudentEvalRecordDetailVO.AnswerItem item = new StudentEvalRecordDetailVO.AnswerItem();
            item.setQuestionNo(q.getQuestionNo());
            item.setQuestionContent(q.getQuestionContent());
            item.setAnswerValue(a.getAnswerValue());
            item.setAnswerLabel(semanticUtil.getAnswerLabel(a.getAnswerValue() != null ? a.getAnswerValue() : 0));
            item.setScore(a.getScore());
            items.add(item);
        }
        items.sort((x, y) -> Integer.compare(x.getQuestionNo() != null ? x.getQuestionNo() : 0,
                y.getQuestionNo() != null ? y.getQuestionNo() : 0));

        StudentEvalRecordDetailVO vo = new StudentEvalRecordDetailVO();
        vo.setRecordId(record.getId());
        vo.setTeacherId(teacherId);
        vo.setTeacherName(teacher != null ? teacher.getRealName() : "");
        vo.setSemesterId(semesterId);
        vo.setSemesterName(semester != null ? semester.getSemesterName() : "");
        vo.setTotalScore(record.getTotalScore());
        vo.setChoiceScore(record.getChoiceScore());
        vo.setEssayScore(record.getEssayScore());
        vo.setEssayContent(record.getEssayContent());
        vo.setCreateTime(record.getCreateTime());
        vo.setAnswers(items);
        return vo;
    }

    /**
     * 获取教师评价详情（供教师查看）
     * 匿名：只返回评语和综合统计，不返回学生身份
     */
    public Map<String, Object> getTeacherEvalView(Long teacherId, Long semesterId) {
        String cacheKey = "teacher:stats:v2:" + teacherId + ":" + semesterId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return (Map<String, Object>) cached;

        Double avgScore = recordMapper.avgScoreByTeacherSemester(teacherId, semesterId);
        int evalCount = recordMapper.countByTeacherSemester(teacherId, semesterId);

        List<EvalRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getTeacherId, teacherId)
                        .eq(EvalRecord::getSemesterId, semesterId)
                        .eq(EvalRecord::getStatus, 1)
                        .orderByDesc(EvalRecord::getCreateTime));

        List<String> comments = records.stream()
                .map(EvalRecord::getEssayContent)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toList());

        // 构建带时间戳的评语列表（供前端展示）
        List<Map<String, Object>> essayList = records.stream()
                .filter(r -> r.getEssayContent() != null && !r.getEssayContent().isBlank())
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("essayContent", r.getEssayContent());
                    m.put("submitTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());

        // 按好评/中评/差评分类（用于工作台展示）
        List<String> positiveWords = getKeywordsFromCache(1);
        List<String> neutralWords = getKeywordsFromCache(2);
        List<String> negativeWords = getKeywordsFromCache(3);
        List<String> commentsPositive = new ArrayList<>();
        List<String> commentsNeutral = new ArrayList<>();
        List<String> commentsNegative = new ArrayList<>();
        for (String c : comments) {
            boolean hasNeg = negativeWords.stream().anyMatch(c::contains);
            boolean hasPos = positiveWords.stream().anyMatch(c::contains);
            boolean hasNeutral = neutralWords.stream().anyMatch(c::contains);
            if (hasNeg) {
                commentsNegative.add(c);
            } else if (hasNeutral && !hasPos) {
                commentsNeutral.add(c);
            } else {
                commentsPositive.add(c);
            }
        }

        List<QuestionAvgScoreVO> questionData = answerMapper.selectAvgScoreByTeacherSemester(teacherId, semesterId);

        // 计算院系排名（按当前学期所有教师均分排序）
        int rankInDept = 0;
        try {
            BigDecimal myAvg = avgScore != null
                    ? BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            List<TeacherEvalStatsVO> allStats = recordMapper.selectTeacherStats(semesterId);
            if (allStats != null && !allStats.isEmpty()) {
                long higherCount = allStats.stream()
                        .filter(s -> s.getTeacherId() != null && !s.getTeacherId().equals(teacherId)
                                && s.getAvgScore() != null && s.getAvgScore().compareTo(myAvg) > 0)
                        .count();
                rankInDept = (int) higherCount + 1;
            } else {
                rankInDept = evalCount > 0 ? 1 : 0;
            }
        } catch (Exception e) {
            log.warn("计算院系排名失败: {}", e.getMessage());
        }

        // ABSA 情感分析（调用 Python 微服务，失败时降级）
        Map<String, Object> absaResult = null;
        try {
            absaResult = absaService.analyzeCommentsBatch(comments);
        } catch (Exception e) {
            log.warn("ABSA 分析调用异常，已跳过: {}", e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgScore",    avgScore != null ? BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("evalCount",   evalCount);
        result.put("rankInDept",  rankInDept);
        result.put("essayList",   essayList);
        result.put("comments",    comments);
        result.put("commentsPositive", commentsPositive);
        result.put("commentsNeutral",  commentsNeutral);
        result.put("commentsNegative", commentsNegative);
        result.put("questionAvg", questionData);
        result.put("absa",        absaResult);

        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);
        return result;
    }

    public List<TeacherEvalStatsVO> getTeacherStatsList(Long semesterId) {
        List<TeacherEvalStatsVO> stats = recordMapper.selectTeacherStats(semesterId);
        if (stats == null || stats.isEmpty()) return stats;

        // 批量查询科目/班级信息并填充
        List<Long> teacherIds = stats.stream().map(TeacherEvalStatsVO::getTeacherId).collect(Collectors.toList());
        LambdaQueryWrapper<TeacherSemester> wrapper = new LambdaQueryWrapper<TeacherSemester>()
                .in(TeacherSemester::getTeacherId, teacherIds);
        if (semesterId != null) wrapper.eq(TeacherSemester::getSemesterId, semesterId);
        List<TeacherSemester> tsList = teacherSemesterMapper.selectList(wrapper);

        Map<Long, List<TeacherSemester>> tsMap = tsList.stream()
                .collect(Collectors.groupingBy(TeacherSemester::getTeacherId));

        for (TeacherEvalStatsVO vo : stats) {
            List<TeacherSemester> rows = tsMap.getOrDefault(vo.getTeacherId(), Collections.emptyList());
            Set<String> courses = new LinkedHashSet<>();
            Set<Integer> classNums = new TreeSet<>();
            for (TeacherSemester ts : rows) {
                if (ts.getCourseName() != null && !ts.getCourseName().isBlank())
                    courses.add(ts.getCourseName());
                int s = ts.getClassStart() != null ? ts.getClassStart() : 1;
                int e = ts.getClassEnd()   != null ? ts.getClassEnd()   : s;
                for (int n = s; n <= e; n++) classNums.add(n);
            }
            vo.setCourses(new ArrayList<>(courses));
            vo.setClasses(classNums.stream().map(n -> "22软工" + n + "班").collect(Collectors.joining("、")));
        }
        return stats;
    }

    @SuppressWarnings("unchecked")
    private List<String> getKeywordsFromCache(int wordType) {
        String cacheKey = "keywords:type:" + wordType;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) return (List<String>) cached;

        List<String> words = keywordMapper.selectWordsByType(wordType);
        redisTemplate.opsForValue().set(cacheKey, words, 1, TimeUnit.HOURS);
        return words;
    }

    // 刷新关键词缓存（关键词修改后调用）
    public void refreshKeywordCache() {
        redisTemplate.delete("keywords:type:1");
        redisTemplate.delete("keywords:type:2");
        redisTemplate.delete("keywords:type:3");
    }

    private static final String CACHE_PRE_WEIGHTS = "pre_question:weights";

    /**
     * 计算参与度基础权重（从 DB/缓存读取配置，支持管理员动态修改）
     */
    private BigDecimal calcEngagementWeight(Integer attendanceRate, Integer studyTime) {
        Map<String, Map<Integer, Double>> wm = getPreWeightMap();
        int ar = (attendanceRate != null && attendanceRate >= 1 && attendanceRate <= 5) ? attendanceRate : 5;
        int st = (studyTime      != null && studyTime      >= 1 && studyTime      <= 5) ? studyTime      : 5;
        double aw = wm.getOrDefault("attendance", Map.of()).getOrDefault(ar, 1.0);
        double sw = wm.getOrDefault("study_time", Map.of()).getOrDefault(st, 1.0);
        return BigDecimal.valueOf((aw + sw) / 2.0).setScale(4, RoundingMode.HALF_UP);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Map<Integer, Double>> getPreWeightMap() {
        Object cached = redisTemplate.opsForValue().get(CACHE_PRE_WEIGHTS);
        if (cached instanceof Map) {
            // GenericJackson2JsonRedisSerializer 反序列化后内层 Map 的 key 是 String 类型
            // 需要手动转换为 Integer，否则 getOrDefault(int, 1.0) 始终命中不到
            Map<String, Map<Integer, Double>> result = new java.util.HashMap<>();
            for (Map.Entry<?, ?> outer : ((Map<?, ?>) cached).entrySet()) {
                String outerKey = String.valueOf(outer.getKey());
                Map<Integer, Double> inner = new java.util.HashMap<>();
                if (outer.getValue() instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) outer.getValue()).entrySet()) {
                        inner.put(Integer.parseInt(String.valueOf(e.getKey())),
                                  Double.parseDouble(String.valueOf(e.getValue())));
                    }
                }
                result.put(outerKey, inner);
            }
            return result;
        }

        List<PreQuestionConfig> cfgs = preQConfigMapper.selectList(null);
        Map<String, Map<Integer, Double>> result = new java.util.HashMap<>();
        for (PreQuestionConfig c : cfgs) {
            result.computeIfAbsent(c.getQuestionKey(), k -> new java.util.HashMap<>())
                  .put(c.getOptionValue(), c.getWeight().doubleValue());
        }
        redisTemplate.opsForValue().set(CACHE_PRE_WEIGHTS, result, 1, TimeUnit.HOURS);
        return result;
    }

    public void refreshPreWeightCache() {
        redisTemplate.delete(CACHE_PRE_WEIGHTS);
    }

    /**
     * 异常数据智能剔除与权重降级（管理员手动或定时触发）
     * 规则：
     *   1. 对同一教师+学期的所有正常记录计算均值和标准差
     *   2. 参与度权重 < 0.65 且 z-score < -1.5 → 剔除（is_excluded=1）
     *   3. 参与度权重 < 0.75 且 z-score < -1.0 → 权重再降级 × 0.8
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> recalculateWeights(Long semesterId) {
        // Redis 缓存：数据未变时直接返回，避免重复计算和写库
        String cacheKey = "recalc:weights:" + semesterId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }

        List<EvalRecord> all = recordMapper.selectList(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getStatus, 1)
                        .eq(semesterId != null, EvalRecord::getSemesterId, semesterId));

        if (all.isEmpty()) {
            Map<String, Object> empty = new java.util.LinkedHashMap<>();
            empty.put("totalRecords", 0);
            empty.put("excluded",     0);
            empty.put("downgraded",   0);
            empty.put("normal",       0);
            redisTemplate.opsForValue().set(cacheKey, empty, 10, TimeUnit.MINUTES);
            return empty;
        }

        // 记录处理前的原始值，用于后续只更新真正变化的记录
        Map<Long, BigDecimal> origWeights  = new HashMap<>();
        Map<Long, Integer>    origExcluded = new HashMap<>();
        for (EvalRecord r : all) {
            origWeights .put(r.getId(), r.getEvalWeight()  != null ? r.getEvalWeight()  : BigDecimal.ONE);
            origExcluded.put(r.getId(), r.getIsExcluded()  != null ? r.getIsExcluded()  : 0);
        }

        // 按教师+学期分组
        Map<String, List<EvalRecord>> grouped = all.stream()
                .collect(Collectors.groupingBy(r -> r.getTeacherId() + "_" + r.getSemesterId()));

        int totalExcluded = 0;
        int totalDowngraded = 0;

        for (List<EvalRecord> group : grouped.values()) {
            for (EvalRecord r : group) {
                BigDecimal baseW = calcEngagementWeight(r.getAttendanceRate(), r.getStudyTime());
                r.setEvalWeight(baseW);
                r.setIsExcluded(0);
            }

            double mean = group.stream()
                    .mapToDouble(r -> r.getTotalScore().doubleValue())
                    .average().orElse(0);
            double variance = group.stream()
                    .mapToDouble(r -> Math.pow(r.getTotalScore().doubleValue() - mean, 2))
                    .average().orElse(0);
            double stdDev = Math.sqrt(variance);

            for (EvalRecord r : group) {
                double score = r.getTotalScore().doubleValue();
                double w     = r.getEvalWeight().doubleValue();
                double z     = stdDev > 0 ? (score - mean) / stdDev : 0;

                if (w < 0.65 && z < -1.5) {
                    r.setIsExcluded(1);
                    totalExcluded++;
                } else if (w < 0.75 && z < -1.0) {
                    r.setEvalWeight(BigDecimal.valueOf(w * 0.8).setScale(4, RoundingMode.HALF_UP));
                    totalDowngraded++;
                }
            }
        }

        // 只批量更新真正发生变化的记录，跳过未变化的记录
        List<EvalRecord> changed = all.stream()
                .filter(r -> {
                    BigDecimal origW = origWeights.get(r.getId());
                    int origE        = origExcluded.get(r.getId());
                    int newE         = r.getIsExcluded() != null ? r.getIsExcluded() : 0;
                    return r.getEvalWeight().compareTo(origW) != 0 || origE != newE;
                })
                .collect(Collectors.toList());

        if (!changed.isEmpty()) {
            recordMapper.batchUpdateWeightAndExcluded(changed);
        }

        // 清理教师统计缓存
        if (semesterId != null) {
            Set<String> cacheKeys = redisTemplate.keys("teacher:stats:*:" + semesterId);
            if (cacheKeys != null && !cacheKeys.isEmpty()) {
                cacheKeys.forEach(key -> redisTemplate.delete(key));
            }
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalRecords", all.size());
        result.put("excluded",     totalExcluded);
        result.put("downgraded",   totalDowngraded);
        result.put("normal",       all.size() - totalExcluded - totalDowngraded);

        // 缓存结果 10 分钟，期间重复调用直接返回
        redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 获取评价记录中参与度分析数据（管理员查看）
     */
    public List<Map<String, Object>> getEngagementStats(Long semesterId) {
        List<EvalRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<EvalRecord>()
                        .eq(EvalRecord::getStatus, 1)
                        .eq(semesterId != null, EvalRecord::getSemesterId, semesterId));

        long total      = records.size();
        long excluded   = records.stream().filter(r -> r.getIsExcluded() != null && r.getIsExcluded() == 1).count();
        long lowAttend  = records.stream().filter(r -> r.getAttendanceRate() != null && r.getAttendanceRate() <= 2).count();
        long lowStudy   = records.stream().filter(r -> r.getStudyTime()      != null && r.getStudyTime()      <= 2).count();
        long bothLow    = records.stream().filter(r ->
                r.getAttendanceRate() != null && r.getAttendanceRate() <= 2 &&
                r.getStudyTime()      != null && r.getStudyTime()      <= 2).count();

        // 出勤率分布
        Map<Integer, Long> attendanceDist = records.stream()
                .filter(r -> r.getAttendanceRate() != null)
                .collect(Collectors.groupingBy(EvalRecord::getAttendanceRate, Collectors.counting()));

        // 课外投入分布
        Map<Integer, Long> studyDist = records.stream()
                .filter(r -> r.getStudyTime() != null)
                .collect(Collectors.groupingBy(EvalRecord::getStudyTime, Collectors.counting()));

        List<Map<String, Object>> stats = new ArrayList<>();
        Map<String, Object> overview = new java.util.LinkedHashMap<>();
        overview.put("type",           "overview");
        overview.put("total",          total);
        overview.put("excluded",       excluded);
        overview.put("lowAttendance",  lowAttend);
        overview.put("lowStudy",       lowStudy);
        overview.put("bothLow",        bothLow);
        overview.put("attendanceDist", attendanceDist);
        overview.put("studyDist",      studyDist);
        stats.add(overview);
        return stats;
    }
}
