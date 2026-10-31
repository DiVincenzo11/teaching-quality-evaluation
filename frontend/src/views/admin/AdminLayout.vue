<template>
  <BaseLayout title="管理员后台" :menu-items="menuItems" home-route="/admin/dashboard" />
</template>

<script setup>
import BaseLayout from '@/components/BaseLayout.vue'

const menuItems = [
  { path: '/admin/dashboard',     label: '系统概览',     icon: 'DataBoard' },
  { path: '/admin/users',         label: '用户管理',     icon: 'User' },
  { path: '/admin/semesters',     label: '学期管理',     icon: 'Calendar' },
  { path: '/admin/questions',     label: '指标体系管理', icon: 'EditPen' },
  { path: '/admin/keywords',      label: '关键词管理',   icon: 'ChatDotSquare' },
  { path: '/admin/announcements', label: '通知公告',     icon: 'Bell' },
  { path: '/admin/analysis',      label: '数据分析',     icon: 'TrendCharts' },
  { path: '/admin/feedback',      label: '反馈查看',     icon: 'ChatLineSquare' },
]
</script>
