package com.landit.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.landit.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author Azir
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
