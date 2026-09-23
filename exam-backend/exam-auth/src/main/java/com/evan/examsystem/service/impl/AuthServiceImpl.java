package com.evan.examsystem.service.impl;

import com.evan.examsystem.common.BusinessException;
import com.evan.examsystem.common.ResultCode;
import com.evan.examsystem.dto.LoginDTO;
import com.evan.examsystem.dto.RegisterDTO;
import com.evan.examsystem.entity.User;
import com.evan.examsystem.mapper.UserMapper;
import com.evan.examsystem.service.AuthService;
import com.evan.examsystem.util.JwtUtil;
import com.evan.examsystem.vo.LoginVO;
import com.evan.examsystem.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     *
     * @param loginDTO
     * @return
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null||!user.getPassword().equals(loginDTO.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        String token=jwtUtil.generateToken(user.getId(),user.getUsername(),user.getRole());
        LoginVO vo=new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setToken(token);
        return vo;
    }

    /**
     *
     * @param registerDTO
     */
    @Override
    public void register(RegisterDTO registerDTO) {
        if(userMapper.findByUsername(registerDTO.getUsername())!=null){
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user=new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setRole(registerDTO.getRole()==null?"student":registerDTO.getRole());
        userMapper.insert(user);
    }

    /**
     *
     * @param userId
     * @return
     */
    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if(user==null){
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        UserVO userVO=new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRole(user.getRole());
        return userVO;
    }
}
