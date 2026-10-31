package com.bishe.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.teach.common.PageResult;
import com.bishe.teach.common.Result;
import com.bishe.teach.entity.Announcement;
import com.bishe.teach.mapper.AnnouncementMapper;
import com.bishe.teach.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知公告")
@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    @Operation(summary = "获取公告列表（已登录用户）")
    @GetMapping("/announcements")
    public Result<PageResult<Announcement>> list(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String keyword) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getDeleted, 0)
                .like(keyword != null && !keyword.isBlank(), Announcement::getTitle, keyword)
                .orderByDesc(Announcement::getTopFlag)
                .orderByDesc(Announcement::getCreateTime);
        return Result.success(PageResult.of(announcementMapper.selectPage(new Page<>(page, size), wrapper)));
    }

    @Operation(summary = "获取公告列表（公开，无需登录）")
    @GetMapping("/announcements/public")
    public Result<PageResult<Announcement>> publicList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getDeleted, 0)
                .orderByDesc(Announcement::getTopFlag)
                .orderByDesc(Announcement::getCreateTime);
        return Result.success(PageResult.of(announcementMapper.selectPage(new Page<>(page, size), wrapper)));
    }

    @Operation(summary = "获取公告详情")
    @GetMapping("/announcements/{id}")
    public Result<Announcement> getById(@PathVariable Long id) {
        return Result.success(announcementMapper.selectById(id));
    }

    @Operation(summary = "获取公告列表（管理员，含草稿）")
    @GetMapping("/admin/announcements")
    public Result<PageResult<Announcement>> adminList(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String keyword) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getDeleted, 0)
                .like(keyword != null && !keyword.isBlank(), Announcement::getTitle, keyword)
                .orderByDesc(Announcement::getTopFlag)
                .orderByDesc(Announcement::getCreateTime);
        return Result.success(PageResult.of(announcementMapper.selectPage(new Page<>(page, size), wrapper)));
    }

    @Operation(summary = "新增公告（管理员）")
    @PostMapping("/admin/announcements")
    public Result<Void> create(@RequestBody Announcement announcement,
                               @AuthenticationPrincipal LoginUser loginUser) {
        announcement.setPublisherId(loginUser.getUserId());
        announcementMapper.insert(announcement);
        return Result.success("发布成功", null);
    }

    @Operation(summary = "修改公告（管理员）")
    @PutMapping("/admin/announcements/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement dto) {
        dto.setId(id);
        announcementMapper.updateById(dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除公告（管理员）")
    @DeleteMapping("/admin/announcements/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
