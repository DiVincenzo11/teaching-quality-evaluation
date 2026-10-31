package com.bishe.teach.security;

import com.bishe.teach.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil             jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            // 检查Token是否在黑名单（已注销）
            Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                chain.doFilter(request, response);
                return;
            }
            if (jwtUtil.validateToken(token)) {
                try {
                    Long    userId   = jwtUtil.getUserId(token);
                    String  username = jwtUtil.getUsername(token);
                    Integer roleType = jwtUtil.getRoleType(token);
                    String  role     = switch (roleType) {
                        case 2 -> "ROLE_TEACHER";
                        case 3 -> "ROLE_ADMIN";
                        default -> "ROLE_STUDENT";
                    };
                    LoginUser loginUser = new LoginUser(userId, username, roleType);
                    var auth = new UsernamePasswordAuthenticationToken(
                            loginUser, null,
                            Collections.singletonList(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    log.warn("解析Token失败: {}", e.getMessage());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
