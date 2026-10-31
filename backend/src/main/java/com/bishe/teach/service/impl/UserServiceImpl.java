package com.bishe.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.teach.common.BusinessException;
import com.bishe.teach.entity.StudentInfo;
import com.bishe.teach.entity.SysUser;
import com.bishe.teach.entity.TeacherInfo;
import com.bishe.teach.dto.UserDTO;
import com.bishe.teach.mapper.EvalRecordMapper;
import com.bishe.teach.mapper.SemesterMapper;
import com.bishe.teach.mapper.StudentInfoMapper;
import com.bishe.teach.mapper.SysUserMapper;
import com.bishe.teach.mapper.TeacherInfoMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bishe.teach.vo.UserListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final SysUserMapper     userMapper;
    private final TeacherInfoMapper teacherInfoMapper;
    private final StudentInfoMapper studentInfoMapper;
    private static final Pattern CLASS_NUM_PATTERN = Pattern.compile("(\\d+)班");
    private final PasswordEncoder   passwordEncoder;
    private final EvalRecordMapper  evalRecordMapper;
    private final SemesterMapper    semesterMapper;

    public IPage<UserListVO> pageUsers(int page, int size, String keyword, Integer roleType, String sortOrder, String className, Long semesterId) {
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);
        // 班级过滤：通过子查询找到 student_info.class_name 匹配的 user_id
        boolean filterClass = StringUtils.hasText(className) && roleType != null && roleType == 1;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .eq(roleType != null, SysUser::getRoleType, roleType)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getEmail, keyword))
                .inSql(filterClass, SysUser::getId,
                        "SELECT user_id FROM student_info WHERE class_name = '" + className + "'")
                .orderBy(true, asc, SysUser::getUsername);

        IPage<SysUser> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<SysUser> users = userPage.getRecords();

        // 批量查询教师扩展信息
        List<Long> teacherIds = users.stream()
                .filter(u -> u.getRoleType() != null && u.getRoleType() == 2)
                .map(SysUser::getId).collect(Collectors.toList());
        Map<Long, TeacherInfo> teacherMap = teacherIds.isEmpty() ? Map.of()
                : teacherInfoMapper.selectList(
                        new LambdaQueryWrapper<TeacherInfo>().in(TeacherInfo::getUserId, teacherIds))
                  .stream().collect(Collectors.toMap(TeacherInfo::getUserId, t -> t));

        // 批量查询学生扩展信息
        List<Long> studentIds = users.stream()
                .filter(u -> u.getRoleType() != null && u.getRoleType() == 1)
                .map(SysUser::getId).collect(Collectors.toList());
        Map<Long, StudentInfo> studentMap = studentIds.isEmpty() ? Map.of()
                : studentInfoMapper.selectList(
                        new LambdaQueryWrapper<StudentInfo>().in(StudentInfo::getUserId, studentIds))
                  .stream().collect(Collectors.toMap(StudentInfo::getUserId, s -> s));

        // 转换为 VO
        List<UserListVO> vos = users.stream().map(u -> {
            UserListVO vo = new UserListVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setEmail(u.getEmail());
            vo.setPhone(u.getPhone());
            vo.setRoleType(u.getRoleType());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());

            if (u.getRoleType() != null && u.getRoleType() == 2) {
                TeacherInfo ti = teacherMap.get(u.getId());
                if (ti != null) {
                    vo.setTeacherNo(ti.getTeacherNo());
                    vo.setDepartment(ti.getDepartment());
                    vo.setTitle(ti.getTitle());
                    vo.setCourses(ti.getCourses());
                }
            } else if (u.getRoleType() != null && u.getRoleType() == 1) {
                StudentInfo si = studentMap.get(u.getId());
                if (si != null) {
                    vo.setStudentNo(si.getStudentNo());
                    vo.setClassName(si.getClassName());
                    vo.setMajor(si.getMajor());
                    vo.setGrade(si.getGrade());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        // 评教状态：统计每位学生已评教师数 vs 该学生该学期需评教师数（按班级课程）
        if (!studentIds.isEmpty()) {
            Long targetSemesterId = semesterId != null ? semesterId : semesterMapper.findCurrentSemesterId();
            Map<Long, Integer> evalCountMap = new java.util.HashMap<>();
            if (targetSemesterId != null) {
                List<Map<String, Object>> countList =
                        evalRecordMapper.countGroupByStudent(targetSemesterId, studentIds);
                for (Map<String, Object> row : countList) {
                    Long sid = ((Number) row.get("studentId")).longValue();
                    int cnt  = ((Number) row.get("evalCount")).intValue();
                    evalCountMap.put(sid, cnt);
                }
            }

            // 按班级号缓存需评教师数，避免重复查询
            Map<Integer, Integer> teacherTotalByClass = new java.util.HashMap<>();
            vos.forEach(vo -> {
                if (vo.getRoleType() != null && vo.getRoleType() == 1) {
                    int count = evalCountMap.getOrDefault(vo.getId(), 0);
                    int classNum = extractClassNumber(vo.getClassName());
                    int teacherTotal = teacherTotalByClass.computeIfAbsent(classNum,
                            cn -> targetSemesterId != null
                                    ? teacherInfoMapper.countTeachersForStudent(targetSemesterId, cn)
                                    : 0);
                    vo.setEvalCount(count);
                    vo.setTeacherTotal(teacherTotal);
                    vo.setEvalCompleted(teacherTotal > 0 && count >= teacherTotal);
                }
            });
        }

        Page<UserListVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Transactional
    public void createUser(UserDTO dto) {
        long exist = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exist > 0) throw new BusinessException(400, "账号已存在");

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456"));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleType(dto.getRoleType());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userMapper.insert(user);

        saveExtInfo(user.getId(), dto);
    }

    @Transactional
    public void updateUser(UserDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) throw new BusinessException(400, "用户不存在");

        if (StringUtils.hasText(dto.getRealName())) user.setRealName(dto.getRealName());
        if (StringUtils.hasText(dto.getEmail()))    user.setEmail(dto.getEmail());
        if (StringUtils.hasText(dto.getPhone()))    user.setPhone(dto.getPhone());
        if (dto.getStatus() != null)                user.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        updateExtInfo(user.getId(), user.getRoleType(), dto);
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(400, "用户不存在");
        // 软删除前把 username 改名，释放唯一键，防止日后重复创建同名账号时报冲突
        user.setUsername(user.getUsername() + "_del_" + id);
        userMapper.updateById(user);
        userMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(400, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(400, "用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private void saveExtInfo(Long userId, UserDTO dto) {
        if (dto.getRoleType() == 2) {
            TeacherInfo ti = new TeacherInfo();
            ti.setUserId(userId);
            ti.setTeacherNo(dto.getTeacherNo());
            ti.setDepartment(dto.getDepartment());
            ti.setTitle(dto.getTitle());
            ti.setCourses(dto.getCourses());
            teacherInfoMapper.insert(ti);
        } else if (dto.getRoleType() == 1) {
            StudentInfo si = new StudentInfo();
            si.setUserId(userId);
            si.setStudentNo(dto.getStudentNo());
            si.setClassName(dto.getClassName());
            si.setMajor(dto.getMajor());
            si.setGrade(dto.getGrade());
            studentInfoMapper.insert(si);
        }
    }

    /** 从班级名称提取班号，如 "软件工程2班"、"22软工1班" → 2 或 1 */
    private int extractClassNumber(String className) {
        if (!StringUtils.hasText(className)) return 1;
        Matcher m = CLASS_NUM_PATTERN.matcher(className);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    private void updateExtInfo(Long userId, Integer roleType, UserDTO dto) {
        if (roleType == 2) {
            TeacherInfo ti = teacherInfoMapper.selectOne(
                    new LambdaQueryWrapper<TeacherInfo>().eq(TeacherInfo::getUserId, userId));
            if (ti != null) {
                if (StringUtils.hasText(dto.getDepartment())) ti.setDepartment(dto.getDepartment());
                if (StringUtils.hasText(dto.getTitle()))      ti.setTitle(dto.getTitle());
                if (StringUtils.hasText(dto.getCourses()))    ti.setCourses(dto.getCourses());
                teacherInfoMapper.updateById(ti);
            }
        } else if (roleType == 1) {
            StudentInfo si = studentInfoMapper.selectOne(
                    new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getUserId, userId));
            if (si != null) {
                if (StringUtils.hasText(dto.getClassName())) si.setClassName(dto.getClassName());
                if (StringUtils.hasText(dto.getMajor()))     si.setMajor(dto.getMajor());
                if (StringUtils.hasText(dto.getGrade()))     si.setGrade(dto.getGrade());
                studentInfoMapper.updateById(si);
            }
        }
    }
}
