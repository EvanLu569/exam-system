package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.PaperQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaperQuestionMapper {

    int batchInsert(@Param("list") List<PaperQuestion> list);

    List<PaperQuestion> findByPaperId(@Param("paperId") Long paperId);

    int deleteByPaperId(@Param("paperId") Long paperId);

    int countByPaperId(@Param("paperId") Long paperId);
}