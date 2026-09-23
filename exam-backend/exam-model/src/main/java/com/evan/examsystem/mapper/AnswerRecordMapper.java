package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.AnswerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerRecordMapper {
    int batchInsert(@Param("list") List<AnswerRecord> list);
    List<AnswerRecord> findByRecordId(@Param("recordId") Long recordId);
    int deleteByRecordId(@Param("recordId") Long recordId);
}