package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.EvalRecord;
import com.bishe.teach.entity.Semester;
import com.bishe.teach.entity.SysUser;
import com.bishe.teach.mapper.AnnouncementMapper;
import com.bishe.teach.mapper.EvalRecordMapper;
import com.bishe.teach.mapper.SemesterMapper;
import com.bishe.teach.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "管理员-系统概览")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final SysUserMapper       userMapper;
    private final EvalRecordMapper    evalRecordMapper;
    private final AnnouncementMapper  announcementMapper;
    private final SemesterMapper      semesterMapper;

    @Operation(summary = "按学期获取评分分布")
    @GetMapping("/score-dist")
    public Result<List<Map<String, Object>>> scoreDist(
            @RequestParam(required = false) Long semesterId) {
        List<EvalRecord> records;
        if (semesterId != null) {
            records = evalRecordMapper.selectList(
                    new LambdaQueryWrapper<EvalRecord>()
                            .eq(EvalRecord::getSemesterId, semesterId)
                            .eq(EvalRecord::getStatus, 1)
                            .isNotNull(EvalRecord::getTotalScore));
        } else {
            Semester current = semesterMapper.selectOne(
                    new LambdaQueryWrapper<Semester>().eq(Semester::getIsCurrent, 1));
            if (current == null) return Result.success(Collections.emptyList());
            records = evalRecordMapper.selectList(
                    new LambdaQueryWrapper<EvalRecord>()
                            .eq(EvalRecord::getSemesterId, current.getId())
                            .eq(EvalRecord::getStatus, 1)
                            .isNotNull(EvalRecord::getTotalScore));
        }
        List<Map<String, Object>> dist = new ArrayList<>();
        String[][] ranges = {{"0-60","0","60"},{"60-70","60","70"},{"70-80","70","80"},{"80-90","80","90"},{"90-100","90","100"}};
        for (String[] r : ranges) {
            double lo = Double.parseDouble(r[1]);
            double hi = Double.parseDouble(r[2]);
            long cnt = records.stream()
                    .filter(rec -> {
                        double s = rec.getTotalScore().doubleValue();
                        return s >= lo && (hi == 100 ? s <= hi : s < hi);
                    })
                    .count();
            Map<String, Object> seg = new LinkedHashMap<>();
            seg.put("label", r[0]);
            seg.put("count", cnt);
            dist.add(seg);
        }
        return Result.success(dist);
    }

    @Operation(summary = "获取概览统计（教师数、学生数、本学期评价数、公告数）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long teacherCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleType, 2)
                        .eq(SysUser::getDeleted, 0)
                        .eq(SysUser::getStatus, 1));
        long studentCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleType, 1)
                        .eq(SysUser::getDeleted, 0)
                        .eq(SysUser::getStatus, 1));
        long evalCount = 0;
        Object avgScore = null;
        List<Map<String, Object>> scoreDist = new ArrayList<>();

        Semester current = semesterMapper.selectOne(
                new LambdaQueryWrapper<Semester>().eq(Semester::getIsCurrent, 1));
        if (current != null) {
            List<EvalRecord> records = evalRecordMapper.selectList(
                    new LambdaQueryWrapper<EvalRecord>()
                            .eq(EvalRecord::getSemesterId, current.getId())
                            .eq(EvalRecord::getStatus, 1)
                            .isNotNull(EvalRecord::getTotalScore));
            evalCount = records.size();

            if (!records.isEmpty()) {
                // 本学期均分
                OptionalDouble avg = records.stream()
                        .mapToDouble(r -> r.getTotalScore().doubleValue())
                        .average();
                if (avg.isPresent()) {
                    avgScore = BigDecimal.valueOf(avg.getAsDouble())
                            .setScale(1, RoundingMode.HALF_UP);
                }

                // 评分分布：按分段统计数量
                String[][] ranges = {{"0-60","0","60"},{"60-70","60","70"},{"70-80","70","80"},{"80-90","80","90"},{"90-100","90","100"}};
                for (String[] r : ranges) {
                    double lo = Double.parseDouble(r[1]);
                    double hi = Double.parseDouble(r[2]);
                    long cnt = records.stream()
                            .filter(rec -> {
                                double s = rec.getTotalScore().doubleValue();
                                return s >= lo && (hi == 100 ? s <= hi : s < hi);
                            })
                            .count();
                    Map<String, Object> seg = new LinkedHashMap<>();
                    seg.put("label", r[0]);
                    seg.put("count", cnt);
                    scoreDist.add(seg);
                }
            }
        }
        long announcementCount = announcementMapper.selectCount(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("teacherCount",      teacherCount);
        result.put("studentCount",      studentCount);
        result.put("evalCount",         evalCount);
        result.put("announcementCount", announcementCount);
        result.put("avgScore",          avgScore);
        result.put("scoreDist",         scoreDist);
        return Result.success(result);
    }
}
