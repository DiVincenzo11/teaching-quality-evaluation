<template>
  <BaseLayout title="教师端" :menu-items="menuItems" home-route="/teacher/dashboard" />
</template>

<script setup>
import BaseLayout from '@/components/BaseLayout.vue'

const menuItems = [
  { path: '/teacher/dashboard', label: '工作台',     icon: 'DataBoard' },
  { path: '/teacher/eval',      label: '评价查看',   icon: 'DataAnalysis' },
  { path: '/teacher/report',    label: '能力提升报告', icon: 'DocumentChecked' },
  { path: '/teacher/feedback',  label: '反馈填写',   icon: 'EditPen' },
  { path: '/teacher/announce',  label: '通知公告',   icon: 'Bell' },
]
</script>
