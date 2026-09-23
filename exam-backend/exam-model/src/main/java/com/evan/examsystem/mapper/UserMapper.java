package com.evan.examsystem.mapper;

import com.evan.examsystem.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    int insert(User user);
}