package com.iflytek.smartprep;

import com.iflytek.smartprep.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证密码哈希逻辑是否正确
 */
public class PasswordHashTest {

    @Test
    public void testPasswordHash() {
        String password = "123456";
        String expectedHash = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

        String actualHash = AuthServiceImpl.hashPassword(password);

        System.out.println("原始密码: " + password);
        System.out.println("期望哈希: " + expectedHash);
        System.out.println("实际哈希: " + actualHash);

        assertEquals(expectedHash, actualHash, "密码哈希值不匹配！");
    }
}
