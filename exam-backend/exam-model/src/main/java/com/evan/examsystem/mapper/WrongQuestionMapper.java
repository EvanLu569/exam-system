package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WrongQuestionMapper {
    WrongQuestion findByUserAndQuestion(@Param("userId") Long userId,
                                        @Param("questionId") Long questionId);
    int insert(WrongQuestion wq);
    int update(WrongQuestion wq);
    List<WrongQuestion> findByUserId(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);
    long countByUserId(@Param("userId") Long userId);

    List<WrongQuestion> findReviewToday(@Param("userId") Long userId,
                                        @Param("now") LocalDateTime now);

    int updateMastered(@Param("id") Long id, @Param("mastered") Integer mastered);

    int deleteById(@Param("id") Long id);

    WrongQuestion findById(@Param("id") Long id);
}