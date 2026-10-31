package com.bishe.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.teach.common.BusinessException;
import com.bishe.teach.dto.RegisterDTO;
import com.bishe.teach.entity.StudentInfo;
import com.bishe.teach.entity.SysUser;
import com.bishe.teach.entity.TeacherInfo;
import com.bishe.teach.mapper.StudentInfoMapper;
import com.bishe.teach.mapper.SysUserMapper;
import com.bishe.teach.mapper.TeacherInfoMapper;
import com.bishe.teach.security.LoginUser;
import com.bishe.teach.util.JwtUtil;
import com.bishe.teach.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final SysUserMapper       userMapper;
    private final TeacherInfoMapper   teacherInfoMapper;
    private final StudentInfoMapper   studentInfoMapper;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private Long expiration;

    public LoginVO login(String username, String password, Integer roleType) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, 0));
        if (user == null) {
            throw new BusinessException(400, "账号不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(400, "账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }
        // 校验登录角色与账号实际角色是否一致
        if (!roleType.equals(user.getRoleType())) {
            String[] roleNames = {"", "学生", "教师", "管理员"};
            String selected = (roleType >= 1 && roleType <= 3) ? roleNames[roleType] : "未知";
            throw new BusinessException(400, "该账号不是" + selected + "身份，请重新选择登录角色");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());

        // 缓存用户信息到Redis
        String cacheKey = "user:info:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, username, 24, TimeUnit.HOURS);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleType(user.getRoleType());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && jwtUtil.validateToken(token)) {
            // 将token加入黑名单，有效期与token一致
            redisTemplate.opsForValue().set(
                    "jwt:blacklist:" + token, "1", expiration, TimeUnit.MILLISECONDS);
        }
    }

    public SysUser getCurrentUser(LoginUser loginUser) {
        return userMapper.selectById(loginUser.getUserId());
    }

    @Transactional
    public LoginVO register(RegisterDTO dto) {
        if (dto.getRoleType() != 1 && dto.getRoleType() != 2) {
            throw new BusinessException(400, "只允许注册学生或教师账号");
        }
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getDeleted, 0));
        if (existing != null) {
            throw new BusinessException(400, "账号已存在，请更换账号");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleType(dto.getRoleType());
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        if (dto.getRoleType() == 2) {
            TeacherInfo info = new TeacherInfo();
            info.setUserId(user.getId());
            info.setTeacherNo(dto.getTeacherNo());
            info.setDepartment(dto.getDepartment());
            info.setTitle(dto.getTitle() != null ? dto.getTitle() : "讲师");
            info.setCourses(dto.getCourses());
            teacherInfoMapper.insert(info);
        } else {
            StudentInfo info = new StudentInfo();
            info.setUserId(user.getId());
            info.setStudentNo(dto.getStudentNo());
            info.setClassName(dto.getClassName());
            info.setMajor(dto.getMajor());
            info.setGrade(dto.getGrade());
            studentInfoMapper.insert(info);
        }

        // ====== 修改的地方在这里 ======
        // 原代码: return login(dto.getUsername(), dto.getPassword());
        // 现代码: 增加了 dto.getRoleType()
        return login(dto.getUsername(), dto.getPassword(), dto.getRoleType());
    }
}