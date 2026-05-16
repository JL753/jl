package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.JwtTokenProvider;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.domain.User;
import com.iflytek.smartprep.dto.LoginRequest;
import com.iflytek.smartprep.dto.LoginResponse;
import com.iflytek.smartprep.dto.ProfileUpdateRequest;
import com.iflytek.smartprep.dto.RegisterRequest;
import com.iflytek.smartprep.mapper.UserMapper;
import com.iflytek.smartprep.service.AuthService;
import com.iflytek.smartprep.service.ProfileService;
import com.iflytek.smartprep.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final TutorService tutorService;
    private final ProfileService profileService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(hashPassword(request.getPassword()))) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return toLoginResponse(user);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String role = normalizeRole(request.getRole());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(hashPassword(request.getPassword()));
        user.setRole(role);
        user.setDisplayName(request.getDisplayName());
        userMapper.insert(user);
        initializeUserData(user);
        return toLoginResponse(user);
    }

    @Override
    public User currentUser() {
        Long uid = LoginUserHolder.get().getUserId();
        return userMapper.selectById(uid);
    }

    @Override
    public User updateCurrentUser(ProfileUpdateRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(hashPassword(request.getPassword()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(user);

        StudentProfile profile = profileService.getByUserId(uid);
        if (profile == null) {
            profile = new StudentProfile();
        }
        if (request.getMajor() != null) profile.setMajor(request.getMajor());
        if (request.getCourse() != null) profile.setCourse(request.getCourse());
        if (request.getKnowledgeBase() != null) profile.setKnowledgeBase(request.getKnowledgeBase());
        if (request.getCognitiveStyle() != null) profile.setCognitiveStyle(request.getCognitiveStyle());
        if (request.getWeakPoints() != null) profile.setWeakPoints(request.getWeakPoints());
        if (request.getInterestPreference() != null) profile.setInterestPreference(request.getInterestPreference());
        if (request.getPacePreference() != null) profile.setPacePreference(request.getPacePreference());
        if (request.getExamGoal() != null) profile.setExamGoal(request.getExamGoal());
        profileService.saveOrUpdateProfile(uid, profile);
        return userMapper.selectById(uid);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "student";
        }
        String lower = role.toLowerCase();
        if (!"teacher".equals(lower) && !"student".equals(lower) && !"admin".equals(lower)) {
            return "student";
        }
        return lower;
    }

    private void initializeUserData(User user) {
        tutorService.initializeUserData(user.getId(), user.getDisplayName(), user.getRole());
    }

    static String hashPassword(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private LoginResponse toLoginResponse(User user) {
        return LoginResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole()))
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .displayName(user.getDisplayName())
                .build();
    }
}
