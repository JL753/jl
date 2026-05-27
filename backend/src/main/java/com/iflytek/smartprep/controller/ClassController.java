package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.ClassMember;
import com.iflytek.smartprep.domain.User;
import com.iflytek.smartprep.domain.ZhiyuClass;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.ClassMemberMapper;
import com.iflytek.smartprep.mapper.UserMapper;
import com.iflytek.smartprep.mapper.ZhiyuClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ZhiyuClassMapper classMapper;
    private final ClassMemberMapper memberMapper;
    private final UserMapper userMapper;

    @PostMapping
    @RequireRole({"teacher"})
    public ApiResponse<ZhiyuClass> createClass(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ApiResponse.fail("班级名称不能为空");

        ZhiyuClass c = new ZhiyuClass();
        c.setId(System.currentTimeMillis());
        c.setName(name);
        c.setTeacherId(LoginUserHolder.get().getUserId());
        c.setInviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        c.setDescription(body.getOrDefault("description", ""));
        c.setCreatedAt(LocalDateTime.now());
        classMapper.insert(c);
        return ApiResponse.ok(c);
    }

    @GetMapping("/my")
    @RequireRole({"teacher"})
    public ApiResponse<List<ZhiyuClass>> myClasses() {
        return ApiResponse.ok(classMapper.selectList(
                new LambdaQueryWrapper<ZhiyuClass>()
                        .eq(ZhiyuClass::getTeacherId, LoginUserHolder.get().getUserId())));
    }

    @PostMapping("/join")
    public ApiResponse<ZhiyuClass> joinClass(@RequestBody Map<String, String> body) {
        String inviteCode = body.get("inviteCode");
        if (inviteCode == null) return ApiResponse.fail("邀请码不能为空");

        ZhiyuClass c = classMapper.selectOne(
                new LambdaQueryWrapper<ZhiyuClass>().eq(ZhiyuClass::getInviteCode, inviteCode.toUpperCase()));
        if (c == null) return ApiResponse.fail("邀请码无效");

        Long userId = LoginUserHolder.get().getUserId();
        long count = memberMapper.selectCount(
                new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, c.getId())
                        .eq(ClassMember::getStudentId, userId));
        if (count > 0) return ApiResponse.fail("已加入该班级");

        ClassMember cm = new ClassMember();
        cm.setId(System.currentTimeMillis());
        cm.setClassId(c.getId());
        cm.setStudentId(userId);
        cm.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(cm);
        return ApiResponse.ok(c);
    }

    @GetMapping("/{id}/members")
    @RequireRole({"teacher"})
    public ApiResponse<List<User>> getMembers(@PathVariable Long id) {
        List<ClassMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getClassId, id));
        List<Long> userIds = members.stream().map(ClassMember::getStudentId).toList();
        return ApiResponse.ok(userIds.isEmpty() ? List.of()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)));
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @RequireRole({"teacher"})
    public ApiResponse<String> removeMember(@PathVariable Long id, @PathVariable Long studentId) {
        memberMapper.delete(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, id)
                .eq(ClassMember::getStudentId, studentId));
        return ApiResponse.ok("已移除");
    }

    @GetMapping("/my-classes")
    public ApiResponse<List<Map<String, Object>>> myClassList() {
        Long userId = LoginUserHolder.get().getUserId();
        List<ClassMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getStudentId, userId));
        if (members.isEmpty()) return ApiResponse.ok(List.of());

        List<Long> classIds = members.stream().map(ClassMember::getClassId).toList();
        List<ZhiyuClass> classes = classMapper.selectBatchIds(classIds);
        List<Map<String, Object>> result = classes.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("inviteCode", c.getInviteCode());
            return m;
        }).toList();
        return ApiResponse.ok(result);
    }
}
