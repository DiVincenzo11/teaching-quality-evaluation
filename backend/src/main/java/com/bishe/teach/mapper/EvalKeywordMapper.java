package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.EvalKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface EvalKeywordMapper extends BaseMapper<EvalKeyword> {

    @Select("SELECT word FROM eval_keyword WHERE word_type = #{wordType}")
    List<String> selectWordsByType(Integer wordType);
}
