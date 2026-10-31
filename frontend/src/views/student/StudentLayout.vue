<template>
  <BaseLayout title="学生端" :menu-items="menuItems" home-route="/student/dashboard" />
</template>

<script setup>
import BaseLayout from '@/components/BaseLayout.vue'

const menuItems = [
  { path: '/student/dashboard', label: '首页',       icon: 'House' },
  { path: '/student/evaluate',  label: '评价教师',   icon: 'EditPen' },
  { path: '/student/history',   label: '我的评价',   icon: 'DocumentChecked' },
  { path: '/student/feedback',  label: '教师反馈',   icon: 'ChatLineSquare' },
  { path: '/student/announce',  label: '通知公告',   icon: 'Bell' },
]
</script>
