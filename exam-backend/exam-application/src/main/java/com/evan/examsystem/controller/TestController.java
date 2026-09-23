package com.evan.examsystem.controller;

import com.evan.examsystem.entity.User;
import com.evan.examsystem.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/user/{username}")
    public User getUser(@PathVariable String username) {
        return userMapper.findByUsername(username);
    }
}