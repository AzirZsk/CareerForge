package com.landit.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.landit.common.enums.Gender;
import com.landit.common.exception.BusinessException;
import com.landit.user.dto.UserCreateRequest;
import com.landit.user.dto.UserStatusResponse;
import com.landit.user.dto.UserInitResponse;
import com.landit.user.dto.UserUpdateRequest;
import com.landit.user.entity.User;
import com.landit.user.mapper.UserMapper;
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

    private static final String SINGLE_USER_ID = "1";

    /**
     * 获取用户状态
     */
    public UserStatusResponse getUserStatus() {
        User user = getById(SINGLE_USER_ID);
        // 用户不存在或未初始化
        if (user == null || user.getName() == null || user.getName().isEmpty()) {
            return UserStatusResponse.notExists();
        }
        // 用户存在，返回用户信息
        String genderValue = user.getGender() != null ? user.getGender().name() : null;
        return UserStatusResponse.exists(user.getId(), user.getName(), genderValue, user.getAvatar());
    }

    /**
     * 检查用户是否已初始化
     *
     * @return true 如果用户已初始化
     */
    public boolean isUserInitialized() {
        User user = getById(SINGLE_USER_ID);
        return user != null && user.getName() != null && !user.getName().isEmpty();
    }

    /**
     * 创建用户
     * 在 AI 解析简历获取用户信息后调用此方法创建用户
     */
    public UserInitResponse createUser(UserCreateRequest request) {
        // 检查用户是否已存在
        if (isUserInitialized()) {
            throw new BusinessException("用户已存在，无法重复初始化");
        }
        // 创建用户
        User user = new User();
        user.setId(SINGLE_USER_ID);
        user.setName(request.getName());
        user.setGender(request.getGender());
        save(user);
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
        return getById(SINGLE_USER_ID);
    }

    /**
     * 更新用户信息
     */
    public User updateUserProfile(UserUpdateRequest request) {
        User user = getById(SINGLE_USER_ID);
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
        User user = getById(SINGLE_USER_ID);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setAvatar(avatarUrl);
        updateById(user);
    }

}
