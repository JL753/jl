package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String role;
    private String displayName;
    private String avatarUrl;
}
