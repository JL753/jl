package com.iflytek.smartprep.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RoleCheckInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录\"}");
            return false;
        }
        String userRole = loginUser.getRole();
        // 教师+管理员角色合并：teacher 兼 admin 权限
        for (String role : requireRole.value()) {
            if (role.equalsIgnoreCase(userRole) ||
                ("teacher".equalsIgnoreCase(userRole) && "admin".equalsIgnoreCase(role))) {
                return true;
            }
        }
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"无权限\"}");
        return false;
    }
}
