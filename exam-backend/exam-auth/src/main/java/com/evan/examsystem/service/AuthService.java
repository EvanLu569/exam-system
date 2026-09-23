package com.evan.examsystem.service;

import com.evan.examsystem.dto.LoginDTO;
import com.evan.examsystem.dto.RegisterDTO;
import com.evan.examsystem.vo.LoginVO;
import com.evan.examsystem.vo.UserVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    UserVO getCurrentUser(Long userId);

}
