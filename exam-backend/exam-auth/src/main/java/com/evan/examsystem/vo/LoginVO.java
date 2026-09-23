package com.evan.examsystem.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long id;
    private String username;
    private String role;
    private String token;
}
