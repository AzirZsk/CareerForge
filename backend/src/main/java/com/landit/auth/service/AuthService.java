package com.landit.auth.service;

import cn.hutool.crypto.digest.BCrypt;
import com.landit.auth.dto.LoginRequest;
import com.landit.auth.dto.LoginResponse;
import com.landit.auth.dto.RegisterRequest;
import com.landit.auth.dto.RegisterResponse;
import com.landit.common.enums.Gender;
import com.landit.common.exception.BusinessException;
import com.landit.common.util.JwtUtil;
import com.landit.common.util.WebUtils;
import com.landit.user.entity.User;
import com.landit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 认证服务实现类
 * 处理用户注册、登录、登出等业务逻辑
 *
 * @author Azir
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @param httpServletRequest HTTP 请求（用于获取注册IP）
     * @return 注册响应
     */
    public RegisterResponse register(RegisterRequest request, HttpServletRequest httpServletRequest) {
        // 检查邮箱是否已存在
        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 加密密码
        String hashedPassword = BCrypt.hashpw(request.getPassword());

        // 创建用户
        User user = new User();
        user.setName(request.getName());
        // 将前端传入的性别值统一转为枚举名存储
        Gender gender = Gender.fromValue(request.getGender());
        user.setGender(gender != null ? gender.name() : null);
        user.setEmail(request.getEmail());
        user.setPassword(hashedPassword);
        user.setStatus("ACTIVE");
        user.setRegisterIp(WebUtils.getClientIp(httpServletRequest));

        userService.save(user);

        log.info("用户注册成功: userId={}, email={}", user.getId(), user.getEmail());

        return RegisterResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含 Token 和用户信息）
     */
    public LoginResponse login(LoginRequest request) {
        // 查找用户（支持邮箱或手机号登录）
        User user = userService.findByAccount(request.getAccount());
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // 检查账号状态
        if ("FROZEN".equals(user.getStatus())) {
            throw new BusinessException("账号已被冻结");
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException("账号已被删除");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        // 兼容老用户：如果 initialized 为 null 但有姓名，自动标记为已初始化
        if (user.getInitialized() == null && user.getName() != null && !user.getName().isEmpty()) {
            user.setInitialized(true);
        }
        userService.updateById(user);

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId());

        log.info("用户登录成功: userId={}, email={}", user.getId(), user.getEmail());

        return LoginResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    /**
     * 用户登出
     * JWT 无状态，客户端删除 Token 即可
     * 此方法仅记录日志
     *
     * @param userId 用户ID
     */
    public void logout(String userId) {
        log.info("用户登出: userId={}", userId);
    }
}
