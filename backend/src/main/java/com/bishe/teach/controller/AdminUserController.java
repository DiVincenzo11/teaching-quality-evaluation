package com.bishe.teach.controller;

import com.bishe.teach.common.PageResult;
import com.bishe.teach.common.Result;
import com.bishe.teach.dto.UserDTO;
import com.bishe.teach.service.impl.UserServiceImpl;
import com.bishe.teach.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserServiceImpl userService;

    @Operation(summary = "分页查询用户（含院系/专业扩展信息）")
    @GetMapping
    public Result<PageResult<UserListVO>> page(
            @RequestParam(defaultValue = "1")    int     page,
            @RequestParam(defaultValue = "10")   int     size,
            @RequestParam(required = false)      String  keyword,
            @RequestParam(required = false)      Integer roleType,
            @RequestParam(defaultValue = "asc")  String  sortOrder,
            @RequestParam(required = false)      String  className,
            @RequestParam(required = false)      Long    semesterId) {
        return Result.success(PageResult.of(userService.pageUsers(page, size, keyword, roleType, sortOrder, className, semesterId)));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserDTO dto) {
        userService.createUser(dto);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        dto.setId(id);
        userService.updateUser(dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.getOrDefault("password", "123456"));
        return Result.success("密码重置成功", null);
    }
}
