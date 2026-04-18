package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
