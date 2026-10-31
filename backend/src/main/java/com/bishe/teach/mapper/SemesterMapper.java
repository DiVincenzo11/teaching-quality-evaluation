package com.bishe.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bishe.teach.entity.Semester;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SemesterMapper extends BaseMapper<Semester> {

    @Select("SELECT id FROM semester WHERE is_current = 1 LIMIT 1")
    Long findCurrentSemesterId();
}
