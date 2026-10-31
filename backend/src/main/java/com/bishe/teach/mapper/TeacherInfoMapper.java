package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.TeacherInfo;
import com.bishe.teach.vo.TeacherVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TeacherInfoMapper extends BaseMapper<TeacherInfo> {

    List<TeacherVO> selectTeacherList(@Param("keyword") String keyword,
                                      @Param("department") String department);

    /** 管理员：按学期筛选（展示该学期所授课程） */
    List<TeacherVO> selectTeacherListBySemester(@Param("semesterId") Long semesterId,
                                                @Param("keyword")    String keyword,
                                                @Param("department") String department);

    /** 学生评价：按学期 + 班级号筛选（仅返回教过该班级的老师） */
    List<TeacherVO> selectTeacherListForStudent(@Param("semesterId") Long semesterId,
                                                @Param("classNum")   int classNum,
                                                @Param("keyword")    String keyword);

    /** 统计某学期某班级学生需评价的教师数量（用于评教进度分母） */
    int countTeachersForStudent(@Param("semesterId") Long semesterId,
                                @Param("classNum")   int classNum);
}
