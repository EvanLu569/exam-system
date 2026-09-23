package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    List<Question> page(@Param("type") Integer type,
                        @Param("difficulty") Integer difficulty,
                        @Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("size") int size);

    long count(@Param("type") Integer type,
               @Param("difficulty") Integer difficulty,
               @Param("keyword") String keyword);

    Question findById(@Param("id") Long id);

    int insert(Question question);

    int update(Question question);

    int deleteById(@Param("id") Long id);

    List<Question> findByIds(@Param("ids") List<Long> ids);

    List<Long> findIdsByTypeAndDifficulty(@Param("type") Integer type,
                                          @Param("difficulty") Integer difficulty);
}