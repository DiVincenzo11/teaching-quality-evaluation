package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.EvalRecord;
import com.bishe.teach.vo.StudentEvalHistoryVO;
import com.bishe.teach.vo.TeacherEvalStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface EvalRecordMapper extends BaseMapper<EvalRecord> {

    @Select("SELECT COUNT(*) FROM eval_record WHERE student_id=#{studentId} AND teacher_id=#{teacherId} AND semester_id=#{semesterId}")
    int countByStudentTeacherSemester(@Param("studentId") Long studentId,
                                      @Param("teacherId") Long teacherId,
                                      @Param("semesterId") Long semesterId);

    @Select("SELECT SUM(total_score * COALESCE(eval_weight,1)) / NULLIF(SUM(COALESCE(eval_weight,1)),0) " +
            "FROM eval_record WHERE teacher_id=#{teacherId} AND semester_id=#{semesterId} AND status=1 AND (is_excluded=0 OR is_excluded IS NULL)")
    Double avgScoreByTeacherSemester(@Param("teacherId") Long teacherId,
                                     @Param("semesterId") Long semesterId);

    @Select("SELECT COUNT(*) FROM eval_record WHERE teacher_id=#{teacherId} AND semester_id=#{semesterId} AND status=1 AND (is_excluded=0 OR is_excluded IS NULL)")
    int countByTeacherSemester(@Param("teacherId") Long teacherId,
                               @Param("semesterId") Long semesterId);

    List<TeacherEvalStatsVO> selectTeacherStats(@Param("semesterId") Long semesterId);

    @Select("<script>SELECT DISTINCT student_id FROM eval_record " +
            "WHERE semester_id = #{semesterId} AND student_id IN " +
            "<foreach item='id' collection='studentIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> findEvaluatedStudentIds(@Param("semesterId") Long semesterId,
                                       @Param("studentIds") List<Long> studentIds);

    /**
     * 批量统计每位学生在指定学期已评价的教师数量
     * 返回 Map 列表，key: studentId (Long), evalCount (Long)
     */
    @Select("<script>SELECT student_id AS studentId, COUNT(*) AS evalCount FROM eval_record " +
            "WHERE semester_id = #{semesterId} AND student_id IN " +
            "<foreach item='id' collection='studentIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            " GROUP BY student_id</script>")
    List<Map<String, Object>> countGroupByStudent(@Param("semesterId") Long semesterId,
                                                  @Param("studentIds") List<Long> studentIds);

    void batchUpdateWeightAndExcluded(@Param("records") List<EvalRecord> records);

    /** 学生查看某学期已提交的评价历史（含教师信息、学期名称） */
    @Select("SELECT er.teacher_id AS teacherId, ti.teacher_no AS teacherNo, u.real_name AS teacherName, " +
            "ti.department, ti.title, er.semester_id AS semesterId, s.semester_name AS semesterName, " +
            "GROUP_CONCAT(DISTINCT ts.course_name ORDER BY ts.course_name SEPARATOR '、') AS courses, " +
            "er.choice_score AS choiceScore, er.essay_score AS essayScore, er.total_score AS totalScore, " +
            "er.essay_content AS essayContent, er.create_time AS createTime " +
            "FROM eval_record er " +
            "JOIN sys_user u ON u.id = er.teacher_id " +
            "JOIN semester s ON s.id = er.semester_id " +
            "LEFT JOIN teacher_info ti ON ti.user_id = er.teacher_id " +
            "LEFT JOIN teacher_semester ts ON ts.teacher_id = er.teacher_id AND ts.semester_id = er.semester_id " +
            "WHERE er.student_id = #{studentId} AND er.semester_id = #{semesterId} AND er.status = 1 " +
            "GROUP BY er.id, er.teacher_id, ti.teacher_no, u.real_name, ti.department, ti.title, " +
            "er.semester_id, s.semester_name, er.choice_score, er.essay_score, er.total_score, er.essay_content, er.create_time " +
            "ORDER BY er.create_time DESC")
    List<StudentEvalHistoryVO> selectStudentSemesterHistory(@Param("studentId") Long studentId,
                                                            @Param("semesterId") Long semesterId);

    /** 学生已评价过的学期 ID 列表（去重） */
    @Select("SELECT DISTINCT er.semester_id FROM eval_record er " +
            "WHERE er.student_id = #{studentId} AND er.status = 1 ORDER BY er.semester_id")
    List<Long> selectEvaluatedSemesterIds(@Param("studentId") Long studentId);

    /** 学生全部评价历史（含教师、学期信息，用于按学期分组展示） */
    @Select("SELECT er.teacher_id AS teacherId, ti.teacher_no AS teacherNo, u.real_name AS teacherName, " +
            "ti.department, ti.title, er.semester_id AS semesterId, s.semester_name AS semesterName, " +
            "GROUP_CONCAT(DISTINCT ts.course_name ORDER BY ts.course_name SEPARATOR '、') AS courses, " +
            "er.choice_score AS choiceScore, er.essay_score AS essayScore, er.total_score AS totalScore, " +
            "er.essay_content AS essayContent, er.create_time AS createTime " +
            "FROM eval_record er " +
            "JOIN sys_user u ON u.id = er.teacher_id " +
            "JOIN semester s ON s.id = er.semester_id " +
            "LEFT JOIN teacher_info ti ON ti.user_id = er.teacher_id " +
            "LEFT JOIN teacher_semester ts ON ts.teacher_id = er.teacher_id AND ts.semester_id = er.semester_id " +
            "WHERE er.student_id = #{studentId} AND er.status = 1 " +
            "GROUP BY er.id, er.teacher_id, ti.teacher_no, u.real_name, ti.department, ti.title, " +
            "er.semester_id, s.semester_name, er.choice_score, er.essay_score, er.total_score, er.essay_content, er.create_time " +
            "ORDER BY er.semester_id, er.create_time DESC")
    List<StudentEvalHistoryVO> selectStudentAllHistory(@Param("studentId") Long studentId);
}
