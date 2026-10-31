package com.bishe.teach.controller;

import com.bishe.teach.common.Result;
import com.bishe.teach.dto.LoginDTO;
import com.bishe.teach.dto.RegisterDTO;
import com.bishe.teach.security.LoginUser;
import com.bishe.teach.service.impl.AuthServiceImpl;
import com.bishe.teach.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto.getUsername(), dto.getPassword(), dto.getRoleType()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "用户注册（教师/学生）")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<Object> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(authService.getCurrentUser(loginUser));
    }
}
