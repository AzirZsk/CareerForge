package com.careerforge.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.careerforge.common.exception.BusinessException;
import com.careerforge.common.util.SecurityUtils;
import com.careerforge.user.dto.UserCreateRequest;
import com.careerforge.user.dto.UserStatusResponse;
import com.careerforge.user.dto.UserInitResponse;
import com.careerforge.user.dto.UserUpdateRequest;
import com.careerforge.user.entity.User;
import com.careerforge.user.mapper.UserMapper;
import com.careerforge.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * 仅负责用户表的 CRUD 操作，聚合操作由 UserHandler 处理
 *
 * @author Azir
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    /**
     * 获取用户状态
     */
    public UserStatusResponse getUserStatus() {
        User user = getById(SecurityUtils.getCurrentUserId());
        // 用户不存在或未初始化
        if (!isUserProfileComplete(user)) {
            return UserStatusResponse.notExists();
        }
        // 用户存在，返回用户信息
        return UserStatusResponse.exists(user.getId(), user.getName(), user.getGender(), user.getAvatar());
    }

    /**
     * 检查用户是否已初始化
     *
     * @return true 如果用户已初始化
     */
    public boolean isUserInitialized() {
        User user = getById(SecurityUtils.getCurrentUserId());
        return isUserProfileComplete(user);
    }

    /**
     * 检查用户资料是否完整（是否已上传简历初始化）
     *
     * @param user 用户对象
     * @return true 如果用户已完成简历初始化
     */
    private boolean isUserProfileComplete(User user) {
        return user != null && Boolean.TRUE.equals(user.getInitialized());
    }

    /**
     * 初始化用户（上传简历解析后更新用户信息）
     * 注册后用户记录已存在，此处更新姓名并标记为已初始化
     */
    public UserInitResponse createUser(UserCreateRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        // 已初始化的用户不允许重复初始化
        if (Boolean.TRUE.equals(user.getInitialized())) {
            throw new BusinessException("用户已初始化，无法重复操作");
        }
        // 更新用户信息（简历解析出的姓名覆盖注册时的姓名）
        user.setName(request.getName());
        user.setGender(request.getGender());
        user.setInitialized(true);
        updateById(user);
        return UserInitResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .gender(user.getGender())
                .build();
    }

    /**
     * 获取当前用户信息
     */
    public User getUserProfile() {
        return getById(SecurityUtils.getCurrentUserId());
    }

    /**
     * 更新用户信息
     */
    public User updateUserProfile(UserUpdateRequest request) {
        User user = getById(SecurityUtils.getCurrentUserId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setName(request.getName());
        user.setGender(request.getGender());
        updateById(user);
        return user;
    }

    /**
     * 更新用户头像URL
     *
     * @param avatarUrl 头像URL
     */
    public void updateUserAvatar(String avatarUrl) {
        User user = getById(SecurityUtils.getCurrentUserId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setAvatar(avatarUrl);
        updateById(user);
    }

    // ========== 以下是认证相关方法 ==========

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return true 如果邮箱已存在
     */
    public boolean existsByEmail(String email) {
        return count(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
    }

    /**
     * 根据账号查找用户（支持邮箱或手机号登录）
     *
     * @param account 账号（邮箱或手机号）
     * @return 用户对象，不存在则返回 null
     */
    public User findByAccount(String account) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, account)
                .or()
                .eq(User::getPhone, account)
                .last("LIMIT 1"));
    }

}
