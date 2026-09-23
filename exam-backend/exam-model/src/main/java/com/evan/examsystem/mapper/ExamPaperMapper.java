package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.ExamPaper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExamPaperMapper {

    List<ExamPaper> page(@Param("offset") int offset, @Param("size") int size);

    long count();

    ExamPaper findById(@Param("id") Long id);

    int insert(ExamPaper paper);

    int update(ExamPaper paper);

    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    List<ExamPaper> findByStatus(@Param("status") Integer status);
}