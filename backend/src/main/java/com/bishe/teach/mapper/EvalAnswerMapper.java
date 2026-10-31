package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.EvalAnswer;
import com.bishe.teach.vo.QuestionAvgScoreVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface EvalAnswerMapper extends BaseMapper<EvalAnswer> {

    List<QuestionAvgScoreVO> selectAvgScoreByTeacherSemester(@Param("teacherId") Long teacherId,
                                                              @Param("semesterId") Long semesterId);

    List<EvalAnswer> selectByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
