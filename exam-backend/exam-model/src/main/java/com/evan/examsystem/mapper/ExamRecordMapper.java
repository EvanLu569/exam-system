package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExamRecordMapper {
    int insert(ExamRecord record);
    ExamRecord findById(@Param("id") Long id);
    int update(ExamRecord record);
    List<ExamRecord> findByUserId(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("size") int size);
    long countByUserId(@Param("userId") Long userId);
    ExamRecord findByPaperAndUser(@Param("paperId") Long paperId,
                                  @Param("userId") Long userId);
}