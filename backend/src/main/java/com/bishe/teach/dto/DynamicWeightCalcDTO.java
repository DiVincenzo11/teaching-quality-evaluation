package com.bishe.teach.dto;

import lombok.Data;
import java.util.List;

/**
 * 直接传入分数矩阵计算动态权重（用于测试场景，不读数据库）
 * scores: 每个子列表为该题所有教师的得分，共12个子列表对应12道题
 */
@Data
public class DynamicWeightCalcDTO {

    /** 每道题的得分列表，索引0=Q1，索引1=Q2，依此类推 */
    private List<List<Double>> scores;

    /** 每题满分，默认5分 */
    private double maxScore = 5.0;

    /** 课程类型 1=理论课 2=实验课 null=通用 */
    private Integer courseType;
}
