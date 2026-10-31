package com.bishe.teach.controller;

import com.bishe.teach.common.BusinessException;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.SysUser;
import com.bishe.teach.mapper.SysUserMapper;
import com.bishe.teach.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserMapper       userMapper;
    private final PasswordEncoder     passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${file.upload-path}")
    private String uploadPath;

    // 手机号修改限制 Redis key 前缀
    private static final String PHONE_LIMIT_KEY = "profile:phone:limited:";
    // 30天 (单位：秒)
    private static final long PHONE_LIMIT_SECONDS = 30L * 24 * 60 * 60;

    @Operation(summary = "获取个人信息")
    @GetMapping
    public Result<SysUser> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) return Result.error("用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BusinessException(400, "请选择文件");

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";
        // 只允许图片格式
        if (!ext.matches("(?i)\\.(jpg|jpeg|png|gif|webp)")) {
            throw new BusinessException(400, "仅支持 JPG / PNG / GIF / WEBP 格式");
        }

        String filename = loginUser.getUserId() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadPath, "avatars");
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(filename));

        // 构造前端可直接访问的 URL（经由 Vite proxy → /api/uploads/...）
        String url = "/api/uploads/avatars/" + filename;

        // 同步更新数据库
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user != null) {
            user.setAvatar(url);
            userMapper.updateById(user);
        }
        return Result.success("上传成功", url);
    }

    @Operation(summary = "修改个人信息（头像/手机号）")
    @PutMapping
    public Result<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody Map<String, String> dto) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) return Result.error("用户不存在");

        String newPhone = dto.get("phone");
        String newEmail = dto.get("email");

        // ── 手机号修改：每 30 天只能改一次 ──────────────────────
        if (StringUtils.hasText(newPhone) && !newPhone.equals(user.getPhone())) {
            String key = PHONE_LIMIT_KEY + loginUser.getUserId();
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                long days = (ttl + 86399) / 86400;
                throw new BusinessException(400, "手机号每月只能修改一次，还需等待 " + days + " 天后才能再次修改");
            }
            user.setPhone(newPhone);
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                    PHONE_LIMIT_SECONDS, TimeUnit.SECONDS);
        }

        if (StringUtils.hasText(newEmail)) user.setEmail(newEmail);

        userMapper.updateById(user);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "查询手机号修改剩余等待天数")
    @GetMapping("/phone-limit")
    public Result<Map<String, Object>> getPhoneLimit(@AuthenticationPrincipal LoginUser loginUser) {
        String key = PHONE_LIMIT_KEY + loginUser.getUserId();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return Result.success(Map.of("limited", false, "remainDays", 0));
        }
        long days = (ttl + 86399) / 86400;
        return Result.success(Map.of("limited", true, "remainDays", days));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestBody Map<String, String> body) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (!passwordEncoder.matches(body.get("oldPassword"), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(body.get("newPassword")));
        userMapper.updateById(user);
        return Result.success("密码修改成功", null);
    }
}
