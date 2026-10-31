package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.TeacherFeedback;
import com.bishe.teach.vo.FeedbackListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherFeedbackMapper extends BaseMapper<TeacherFeedback> {

    List<FeedbackListVO> selectFeedbackListWithNames(@Param("teacherId") Long teacherId,
                                                     @Param("semesterId") Long semesterId);

    List<FeedbackListVO> selectFeedbackForStudent(@Param("semesterId") Long semesterId,
                                                  @Param("classNum") int classNum,
                                                  @Param("studentId") Long studentId);
}
