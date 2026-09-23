package com.evan.examsystem.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.StringTokenizer;

@Component
public class JwtUtil {
    private static final String SECRET="exam-system-secret-key-2026-please-change-me";
    private static final long EXPIRE_MS=7*24*60*60*1000;//7天

    /**
     *
     * @return
     */
    private SecretKey getKey(){
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成token
     * @param userId
     * @param username
     * @param role
     * @return
     */
    public String generateToken(Long userId,String username,String role){
        Date now=new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username",username)
                .claim("role",role)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+EXPIRE_MS))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析token
     * @param token
     * @return
     */
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从token中去userId
     * @param token
     * @return
     */
    public Long getUserId(String token){
        return Long.valueOf(parseToken(token).getSubject());
    }

    /**
     * 从token中去role
     * @param token
     * @return
     */
    public String getRole(String token){
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从token中去username
     * @param token
     * @return
     */
    public String  getUsername(String token){
        return parseToken(token).get("username", String.class);
    }

    /**
     * 校验token是否有效
     * @param token
     * @return
     */
    public boolean validate(String token){
        try{
            parseToken(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

}
