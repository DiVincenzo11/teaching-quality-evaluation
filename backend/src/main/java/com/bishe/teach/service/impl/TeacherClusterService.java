package com.bishe.teach.service.impl;

import com.bishe.teach.vo.TeacherEvalStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师画像聚类服务 —— K-Means (K=3)
 * 特征向量: [综合均分, 单选均分, 简答均分, 评价人数(归一化)]
 * 聚类结果: "严谨学术型" / "风趣互动型" / "均衡发展型"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherClusterService {

    private static final int K = 3;
    private static final int MAX_ITER = 100;

    /** 聚类标签与描述 */
    private static final String[] CLUSTER_LABELS = {
            "严谨学术型", "风趣互动型", "均衡发展型"
    };
    private static final String[] CLUSTER_COLORS = {
            "#5470c6", "#91cc75", "#fac858"
    };
    private static final String[] CLUSTER_DESCRIPTIONS = {
            "教学内容严谨深入，重视知识体系的系统性，单选题得分较高，互动性相对较弱",
            "课堂气氛活跃，善于激发学生兴趣，简答题评价突出，学生参与度高",
            "综合表现均衡，教学各维度表现稳定，整体评价良好"
    };

    public Map<String, Object> clusterTeachers(List<TeacherEvalStatsVO> teachers) {
        if (teachers == null || teachers.size() < K) {
            return buildEmptyResult();
        }

        // 构造特征矩阵
        double[][] features = extractFeatures(teachers);

        // 归一化
        double[] min = new double[4];
        double[] max = new double[4];
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, -Double.MAX_VALUE);
        for (double[] row : features) {
            for (int j = 0; j < 4; j++) {
                min[j] = Math.min(min[j], row[j]);
                max[j] = Math.max(max[j], row[j]);
            }
        }
        double[][] normalized = normalize(features, min, max);

        // K-Means
        double[][] centroids = initCentroids(normalized);
        int[] assignments = new int[teachers.size()];

        for (int iter = 0; iter < MAX_ITER; iter++) {
            int[] newAssignments = assign(normalized, centroids);
            double[][] newCentroids = updateCentroids(normalized, newAssignments);
            if (Arrays.equals(assignments, newAssignments)) break;
            assignments = newAssignments;
            centroids = newCentroids;
        }

        // 根据聚类中心特征映射到语义标签
        int[] labelMapping = mapCentroidsToLabels(centroids);

        // 构建返回数据
        List<Map<String, Object>> teacherResults = new ArrayList<>();
        for (int i = 0; i < teachers.size(); i++) {
            TeacherEvalStatsVO t = teachers.get(i);
            int clusterIdx = assignments[i];
            int labelIdx = labelMapping[clusterIdx];

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("teacherId", t.getTeacherId());
            item.put("realName", t.getRealName());
            item.put("department", t.getDepartment());
            item.put("avgScore", t.getAvgScore());
            item.put("avgChoiceScore", t.getAvgChoiceScore());
            item.put("avgEssayScore", t.getAvgEssayScore());
            item.put("evalCount", t.getEvalCount());
            item.put("cluster", labelIdx);
            item.put("clusterLabel", CLUSTER_LABELS[labelIdx]);
            item.put("clusterColor", CLUSTER_COLORS[labelIdx]);
            teacherResults.add(item);
        }

        List<Map<String, Object>> clusterSummary = new ArrayList<>();
        for (int c = 0; c < K; c++) {
            final int fc = c;
            final int[] la = labelMapping;
            int labelIdx = -1;
            for (int i = 0; i < K; i++) {
                if (la[i] == c) { labelIdx = i; break; }
            }
            // reverse: find which label maps to cluster fc
            int memberCount = 0;
            for (int a : assignments) if (labelMapping[a] == c) memberCount++;

            Map<String, Object> s = new LinkedHashMap<>();
            s.put("cluster", c);
            s.put("label", CLUSTER_LABELS[c]);
            s.put("description", CLUSTER_DESCRIPTIONS[c]);
            s.put("color", CLUSTER_COLORS[c]);
            s.put("count", memberCount);
            clusterSummary.add(s);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("teachers", teacherResults);
        result.put("clusterSummary", clusterSummary);
        result.put("k", K);
        return result;
    }

    private double[][] extractFeatures(List<TeacherEvalStatsVO> teachers) {
        double[][] f = new double[teachers.size()][4];
        for (int i = 0; i < teachers.size(); i++) {
            TeacherEvalStatsVO t = teachers.get(i);
            f[i][0] = t.getAvgScore() != null ? t.getAvgScore().doubleValue() : 0;
            f[i][1] = t.getAvgChoiceScore() != null ? t.getAvgChoiceScore().doubleValue() : 0;
            f[i][2] = t.getAvgEssayScore() != null ? t.getAvgEssayScore().doubleValue() : 0;
            f[i][3] = t.getEvalCount();
        }
        return f;
    }

    private double[][] normalize(double[][] data, double[] min, double[] max) {
        double[][] out = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                double range = max[j] - min[j];
                out[i][j] = range > 0 ? (data[i][j] - min[j]) / range : 0.5;
            }
        }
        return out;
    }

    private double[][] initCentroids(double[][] data) {
        // K-Means++ 初始化
        Random rand = new Random(42);
        double[][] centroids = new double[K][data[0].length];
        centroids[0] = data[rand.nextInt(data.length)].clone();

        for (int k = 1; k < K; k++) {
            double[] dists = new double[data.length];
            double sum = 0;
            for (int i = 0; i < data.length; i++) {
                double minDist = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    minDist = Math.min(minDist, euclidean(data[i], centroids[c]));
                }
                dists[i] = minDist * minDist;
                sum += dists[i];
            }
            double r = rand.nextDouble() * sum;
            double cum = 0;
            for (int i = 0; i < data.length; i++) {
                cum += dists[i];
                if (cum >= r) { centroids[k] = data[i].clone(); break; }
            }
        }
        return centroids;
    }

    private int[] assign(double[][] data, double[][] centroids) {
        int[] a = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            double minD = Double.MAX_VALUE;
            for (int c = 0; c < K; c++) {
                double d = euclidean(data[i], centroids[c]);
                if (d < minD) { minD = d; a[i] = c; }
            }
        }
        return a;
    }

    private double[][] updateCentroids(double[][] data, int[] assignments) {
        double[][] sums = new double[K][data[0].length];
        int[] counts = new int[K];
        for (int i = 0; i < data.length; i++) {
            int c = assignments[i];
            counts[c]++;
            for (int j = 0; j < data[i].length; j++) sums[c][j] += data[i][j];
        }
        double[][] centroids = new double[K][data[0].length];
        for (int c = 0; c < K; c++) {
            if (counts[c] > 0) {
                for (int j = 0; j < sums[c].length; j++) centroids[c][j] = sums[c][j] / counts[c];
            }
        }
        return centroids;
    }

    /**
     * 根据质心特征语义映射到标签索引：
     * 严谨学术型: 单选高、简答相对低
     * 风趣互动型: 简答高、互动评分突出
     * 均衡发展型: 整体均衡
     */
    private int[] mapCentroidsToLabels(double[][] centroids) {
        // feature: [avgScore, choiceScore, essayScore, evalCount]
        // 严谨学术型 -> 单选/简答比值最高
        // 风趣互动型 -> 简答最高
        // 均衡发展型 -> 整体均衡（choice/essay 差值最小）
        int[] mapping = new int[K];
        boolean[] used = new boolean[3];

        // 简答得分最高的为风趣互动型 (index 1)
        int essayHighIdx = 0;
        for (int c = 1; c < K; c++) {
            if (centroids[c][2] > centroids[essayHighIdx][2]) essayHighIdx = c;
        }
        mapping[essayHighIdx] = 1;
        used[1] = true;

        // 单选/简答差最大为严谨学术型 (index 0)
        int academicIdx = -1;
        double maxRatio = -1;
        for (int c = 0; c < K; c++) {
            if (c == essayHighIdx) continue;
            double ratio = centroids[c][1] - centroids[c][2];
            if (ratio > maxRatio) { maxRatio = ratio; academicIdx = c; }
        }
        if (academicIdx == -1) academicIdx = (essayHighIdx + 1) % K;
        mapping[academicIdx] = 0;

        // 剩余为均衡发展型 (index 2)
        for (int c = 0; c < K; c++) {
            if (c != essayHighIdx && c != academicIdx) {
                mapping[c] = 2;
            }
        }
        return mapping;
    }

    private double euclidean(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += (a[i]-b[i])*(a[i]-b[i]);
        return Math.sqrt(sum);
    }

    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("teachers", Collections.emptyList());
        r.put("clusterSummary", Collections.emptyList());
        r.put("message", "数据量不足，至少需要 " + K + " 位教师的评价数据");
        return r;
    }
}
