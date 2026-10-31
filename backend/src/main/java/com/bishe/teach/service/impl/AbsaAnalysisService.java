package com.bishe.teach.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ABSA（方面级情感分析）服务
 * 调用 Python Flask 微服务进行 BERT 情感分析
 * 当微服务不可用时，降级返回 null（前端显示"分析服务暂不可用"）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbsaAnalysisService {

    @Value("${nlp.service.url:http://localhost:5001}")
    private String nlpServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile boolean serviceAvailable = true;
    private volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 30_000;

    /**
     * 分析单条评语
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeComment(String text) {
        if (text == null || text.isBlank()) return null;
        if (!isServiceAvailable()) return null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(Map.of("text", text));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    nlpServiceUrl + "/absa", HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                serviceAvailable = true;
                return formatResult(response.getBody());
            }
        } catch (ResourceAccessException e) {
            serviceAvailable = false;
            lastCheckTime = System.currentTimeMillis();
            log.warn("ABSA 服务连接失败，已降级处理: {}", e.getMessage());
        } catch (Exception e) {
            log.error("ABSA 分析异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 批量分析评语列表，返回综合维度统计
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeCommentsBatch(List<String> comments) {
        if (comments == null || comments.isEmpty()) return buildEmpty();
        if (!isServiceAvailable()) return null;

        try {
            List<Map<String, Object>> items = new ArrayList<>();
            for (int i = 0; i < comments.size(); i++) {
                items.add(Map.of("id", String.valueOf(i), "text", comments.get(i)));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = objectMapper.writeValueAsString(Map.of("items", items));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    nlpServiceUrl + "/batch-absa", HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                serviceAvailable = true;
                return aggregateBatchResults((List<Map<String, Object>>) response.getBody().get("results"));
            }
        } catch (ResourceAccessException e) {
            serviceAvailable = false;
            lastCheckTime = System.currentTimeMillis();
            log.warn("ABSA 批量分析服务不可用，已降级: {}", e.getMessage());
        } catch (Exception e) {
            log.error("ABSA 批量分析异常: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregateBatchResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) return buildEmpty();

        // 聚合各维度均分
        List<String> aspects = List.of("教学态度", "课程难度", "表达清晰度", "互动性", "教学内容");
        Map<String, List<Double>> dimScores = new LinkedHashMap<>();
        for (String a : aspects) dimScores.put(a, new ArrayList<>());

        double overallSum = 0;
        for (Map<String, Object> r : results) {
            overallSum += toDouble(r.get("overallScore"));
            Map<String, Object> dims = (Map<String, Object>) r.get("dimensions");
            if (dims != null) {
                for (String asp : aspects) {
                    Object d = dims.get(asp);
                    if (d instanceof Map dm) {
                        Boolean hasData = (Boolean) dm.get("hasData");
                        if (Boolean.TRUE.equals(hasData)) {
                            dimScores.get(asp).add(toDouble(dm.get("score")));
                        }
                    }
                }
            }
        }

        Map<String, Object> aggregated = new LinkedHashMap<>();
        Map<String, Object> dimAvg = new LinkedHashMap<>();
        for (String asp : aspects) {
            List<Double> scores = dimScores.get(asp);
            double avg = scores.isEmpty() ? 50.0
                    : scores.stream().mapToDouble(Double::doubleValue).average().orElse(50.0);
            dimAvg.put(asp, round(avg));
        }
        aggregated.put("dimensionAvg", dimAvg);
        aggregated.put("overallAvg", round(results.isEmpty() ? 50 : overallSum / results.size()));
        aggregated.put("analyzeCount", results.size());
        aggregated.put("available", true);
        return aggregated;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> formatResult(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimensions", raw.get("dimensions"));
        result.put("overallSentiment", raw.get("overallSentiment"));
        result.put("overallScore", raw.get("overallScore"));
        result.put("keywords", raw.get("keywords"));
        result.put("available", true);
        return result;
    }

    private boolean isServiceAvailable() {
        if (serviceAvailable) return true;
        if (System.currentTimeMillis() - lastCheckTime > CHECK_INTERVAL_MS) {
            try {
                restTemplate.getForObject(nlpServiceUrl + "/health", Map.class);
                serviceAvailable = true;
                log.info("ABSA 服务恢复可用");
            } catch (Exception ignored) {
                lastCheckTime = System.currentTimeMillis();
            }
        }
        return serviceAvailable;
    }

    private Map<String, Object> buildEmpty() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", false);
        return m;
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
