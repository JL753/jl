package com.iflytek.smartprep.config;

import com.iflytek.smartprep.dto.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private static final Set<String> OPEN_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/common/health",
            "/api/common/portal",
            "/api/common/datacenter",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui/index.html",
            "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (OPEN_PATHS.stream().anyMatch(uri::startsWith)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"缺少Token\"}");
            return false;
        }
        try {
            Claims claims = jwtTokenProvider.parse(auth.substring(7));
            LoginUserHolder.set(LoginUser.builder()
                    .userId(((Number) claims.get("uid")).longValue())
                    .username((String) claims.get("username"))
                    .role((String) claims.get("role"))
                    .build());
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Token无效或已过期\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserHolder.clear();
    }
}
