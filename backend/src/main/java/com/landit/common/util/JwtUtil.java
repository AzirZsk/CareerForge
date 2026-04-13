package com.landit.common.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTException;
import com.landit.common.config.JwtProperties;
import com.landit.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 * 基于 Hutool JWT 实现 Token 生成和验证
 *
 * @author Azir
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @return Token 字符串
     */
    public String generateToken(String userId) {
        Date expireDate = new Date(System.currentTimeMillis() + jwtProperties.getExpire());
        return JWT.create()
                .setPayload("userId", userId)
                .setKey(jwtProperties.getSecret().getBytes())
                .setExpiresAt(expireDate)
                .sign();
    }

    /**
     * 验证并解析 Token
     *
     * @param token Token 字符串
     * @return 用户ID
     * @throws BusinessException Token 验证失败或已过期
     */
    public String verifyToken(String token) {
        try {
            JWT jwt = JWT.of(token).setKey(jwtProperties.getSecret().getBytes());
            // 验证签名和过期时间
            if (!jwt.verify()) {
                throw new BusinessException("Token签名验证失败");
            }
            // 检查过期时间（Hutool JWT 的 exp 返回秒级时间戳 Number）
            Object expObj = jwt.getPayload("exp");
            if (expObj instanceof Number) {
                long expTimestamp = ((Number) expObj).longValue() * 1000;
                Date expiresAt = new Date(expTimestamp);
                if (expiresAt.before(new Date())) {
                    throw new BusinessException("Token已过期");
                }
            }
            // 提取用户ID并进行空值检查
            Object userIdObj = jwt.getPayload("userId");
            if (userIdObj == null) {
                throw new BusinessException("Token中缺少用户ID");
            }
            return userIdObj.toString();
        } catch (JWTException e) {
            throw new BusinessException("Token验证失败：" + e.getMessage());
        }
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token Token 字符串
     * @return 过期时间戳（毫秒）
     */
    public long getExpireTime(String token) {
        JWT jwt = JWT.of(token);
        Object expObj = jwt.getPayload("exp");
        if (expObj instanceof Number) {
            return ((Number) expObj).longValue() * 1000;
        }
        return 0L;
    }
}
