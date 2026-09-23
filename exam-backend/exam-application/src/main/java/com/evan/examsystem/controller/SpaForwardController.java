package com.evan.examsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 前端路由回退：history 模式下，刷新深层页面时把路径转发到 index.html，
 * 由前端 Vue Router 接管渲染。只覆盖前端路由前缀，不拦截 /api 和 /ws。
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/login", "/exam/**", "/exams", "/records/**", "/wrong-questions", "/stats", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
