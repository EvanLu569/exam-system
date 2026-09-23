package com.evan.examsystem.config;

import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //放行OPTIONS 预检请求
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if(authorization==null||!authorization.startsWith("Bearer ")){
            response.setStatus(ResultCode.UNAUTHORIZED.getCode());
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 token 失效\",\"data\":null}");
            return false;
        }

        String token = authorization.substring(7);
        if(!jwtUtil.validate(token)) {
            response.setStatus(ResultCode.UNAUTHORIZED.getCode());
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 token 失效\",\"data\":null}");
            return false;
        }

        request.setAttribute("userId",jwtUtil.getUserId(token));
        request.setAttribute("role",jwtUtil.getRole(token));
        request.setAttribute("username",jwtUtil.getUsername(token));
        return true;
    }
}
