package com.careerforge.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类
 * 从 application.yml 读取 JWT 相关配置
 *
 * @author Azir
 */
@Data
@Component
@ConfigurationProperties(prefix = "careerforge.jwt")
public class JwtProperties {

    /**
     * JWT 密钥（用于签名和验证）
     * 长度建议至少 256 位
     * 生产环境必须通过环境变量配置，禁止使用默认值
     */
    private String secret;

    /**
     * Token 过期时间（毫秒）
     * 默认 2 小时（7200000 ms）
     */
    private Long expire = 7200000L;
}
