package com.bishe.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.entity.EvalAnswer;
import com.bishe.teach.entity.EvalQuestion;
import com.bishe.teach.mapper.EvalAnswerMapper;
import com.bishe.teach.mapper.EvalQuestionMapper;
import com.bishe.teach.mapper.EvalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于信息熵的动态权重生成服务
 * 熵权法原理：指标值差异越大（熵值越小），携带的信息量越多，权重越大。
 * 支持按课程类型（理论课/实验课）调整权重分布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicWeightService {

    private final EvalQuestionMapper             questionMapper;
    private final EvalAnswerMapper               answerMapper;
    private final EvalRecordMapper               recordMapper;
    private final RedisTemplate<String, Object>  redisTemplate;

    /**
     * @param semesterId 学期ID，null 表示全部
     * @param courseType 课程类型 1=理论课 2=实验课 null=通用
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> calcDynamicWeights(Long semesterId, Integer courseType) {
        // Redis 缓存，30分钟内同参数直接返回
        String cacheKey = "dynamic:weights:v2:" + semesterId + ":" + courseType;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            return (List<Map<String, Object>>) cached;
        }

        List<EvalQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>()
                        .eq(EvalQuestion::getQuestionType, 1)
                        .eq(EvalQuestion::getStatus, 1)
                        .orderByAsc(EvalQuestion::getSortOrder));

        if (questions.isEmpty()) return Collections.emptyList();

        // 一次性批量查出所有题目的答案，避免 N+1 查询
        List<Long> questionIds = questions.stream().map(EvalQuestion::getId).collect(Collectors.toList());
        List<EvalAnswer> allAnswers = answerMapper.selectByQuestionIds(questionIds);

        // 按 questionId 分组
        Map<Long, List<EvalAnswer>> answerGroupMap = allAnswers.stream()
                .collect(Collectors.groupingBy(EvalAnswer::getQuestionId));

        // 收集每道题的所有得分（归一化到 [0,1]）
        Map<Long, List<Double>> scoreMap = new LinkedHashMap<>();
        for (EvalQuestion q : questions) {
            List<EvalAnswer> answers = answerGroupMap.getOrDefault(q.getId(), Collections.emptyList());
            if (answers.isEmpty()) {
                scoreMap.put(q.getId(), Collections.emptyList());
                continue;
            }
            double maxS = q.getMaxScore().doubleValue();
            List<Double> normalized = answers.stream()
                    .map(a -> a.getScore() != null ? a.getScore().doubleValue() / maxS : 0.0)
                    .collect(Collectors.toList());
            scoreMap.put(q.getId(), normalized);
        }

        List<Map<String, Object>> result = buildResult(questions, scoreMap, courseType);

        // 写入缓存，30 分钟过期
        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 直接传入分数矩阵计算权重（不读数据库，专用于测试场景）
     * @param questionScores 每道题所有教师的得分列表，索引0=Q1，共12项
     * @param maxScore       每题满分（如5.0）
     * @param courseType     课程类型
     */
    public List<Map<String, Object>> calcFromInputData(
            List<List<Double>> questionScores, double maxScore, Integer courseType) {

        if (questionScores == null || questionScores.isEmpty()) return Collections.emptyList();

        List<EvalQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<EvalQuestion>()
                        .eq(EvalQuestion::getQuestionType, 1)
                        .eq(EvalQuestion::getStatus, 1)
                        .orderByAsc(EvalQuestion::getSortOrder));

        int count = Math.min(questions.size(), questionScores.size());

        Map<Long, List<Double>> scoreMap = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            EvalQuestion q = questions.get(i);
            double ms = maxScore > 0 ? maxScore : q.getMaxScore().doubleValue();
            List<Double> normalized = questionScores.get(i).stream()
                    .map(v -> v / ms)
                    .collect(Collectors.toList());
            scoreMap.put(q.getId(), normalized);
        }

        return buildResult(questions.subList(0, count), scoreMap, courseType);
    }

    private List<Map<String, Object>> buildResult(
            List<EvalQuestion> questions,
            Map<Long, List<Double>> scoreMap,
            Integer courseType) {

        Map<Long, Double> entropyMap = new HashMap<>();
        for (EvalQuestion q : questions) {
            entropyMap.put(q.getId(), calcEntropy(scoreMap.get(q.getId())));
        }

        Map<Long, Double> diffMap = new HashMap<>();
        for (Long id : entropyMap.keySet()) {
            diffMap.put(id, 1.0 - entropyMap.get(id));
        }

        double sumDiff = diffMap.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sumDiff == 0) sumDiff = 1.0;

        Map<Integer, Double> typeFactors = getCourseTypeFactors(courseType);

        List<Map<String, Object>> result = new ArrayList<>();
        for (EvalQuestion q : questions) {
            double rawWeight = diffMap.get(q.getId()) / sumDiff;
            double factor = getFactorForQuestion(q.getQuestionNo(), typeFactors);
            double adjustedWeight = rawWeight * factor;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q.getId());
            item.put("questionNo", q.getQuestionNo());
            item.put("questionContent", q.getQuestionContent());
            item.put("entropy", round(entropyMap.get(q.getId())));
            item.put("rawWeight", round(rawWeight * 100));
            item.put("adjustedWeight", round(adjustedWeight * 100));
            item.put("maxScore", q.getMaxScore());
            result.add(item);
        }

        double sumAdj = result.stream()
                .mapToDouble(m -> ((Number) m.get("adjustedWeight")).doubleValue()).sum();
        if (sumAdj > 0) {
            for (Map<String, Object> item : result) {
                double w = ((Number) item.get("adjustedWeight")).doubleValue() / sumAdj * 100;
                item.put("adjustedWeight", round(w));
            }
        }
        return result;
    }

    /**
     * 标准熵权法熵值计算：E_j = -1/ln(n) * Σ p_ij * ln(p_ij)
     * 其中 p_ij = x_ij / Σ x_ij（各评价对象在该指标上的比例）
     * 所有对象得分相同 → p_ij = 1/n → E_j = 1（最大熵，权重趋近0）
     * 得分分散       → E_j < 1   → d_j = 1 - E_j 较大 → 权重较大
     */
    private double calcEntropy(List<Double> values) {
        if (values == null || values.isEmpty()) return 1.0;
        int n = values.size();
        if (n == 1) return 1.0;

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return 1.0;

        double entropy = 0.0;
        for (double v : values) {
            double p = v / sum;
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        double logN = Math.log(n);
        return logN > 0 ? Math.min(entropy / logN, 1.0) : 1.0;
    }

    /**
     * 理论课偏重"教学内容"和"逻辑表达"；实验课偏重"互动性"和"实践指导"
     */
    private Map<Integer, Double> getCourseTypeFactors(Integer courseType) {
        Map<Integer, Double> factors = new HashMap<>();
        if (courseType == null) {
            for (int i = 1; i <= 10; i++) factors.put(i, 1.0);
            return factors;
        }
        if (courseType == 1) { // 理论课
            factors.put(1, 1.0);  // 教学态度
            factors.put(2, 1.2);  // 教学内容丰富性 +20%
            factors.put(3, 1.1);  // 教学重难点把握
            factors.put(4, 1.2);  // 逻辑表达清晰
            factors.put(5, 0.9);  // 互动提问
            factors.put(6, 0.8);  // 实验实践
            factors.put(7, 1.1);  // 作业布置
            factors.put(8, 1.0);  // 答疑辅导
            factors.put(9, 1.0);  // 整体满意度
        } else { // 实验课
            factors.put(1, 1.0);
            factors.put(2, 0.9);
            factors.put(3, 1.0);
            factors.put(4, 0.9);
            factors.put(5, 1.2);  // 互动提问 +20%
            factors.put(6, 1.3);  // 实验实践 +30%
            factors.put(7, 1.0);
            factors.put(8, 1.1);  // 答疑辅导 +10%
            factors.put(9, 1.0);
        }
        for (int i = 10; i <= 20; i++) factors.put(i, 1.0);
        return factors;
    }

    private double getFactorForQuestion(Integer questionNo, Map<Integer, Double> factors) {
        return factors.getOrDefault(questionNo, 1.0);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
