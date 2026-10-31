<template>
  <view class="login-page">
    <view class="bg-circle c1" />
    <view class="bg-circle c2" />
    <view class="bg-circle c3" />

    <view class="login-box">
      <view class="logo-area">
        <view class="logo-icon">🎓</view>
        <text class="app-name">教师教学质量评价系统</text>
        <text class="subtitle">智能分析 · 客观评价 · 促进提升</text>
      </view>

      <!-- 角色选择 -->
      <view class="role-tabs">
        <view
          v-for="r in roles"
          :key="r.value"
          class="role-tab"
          :class="{ active: form.roleType === r.value }"
          @click="form.roleType = r.value"
        >
          <text class="role-icon">{{ r.icon }}</text>
          <text>{{ r.label }}</text>
        </view>
      </view>

      <!-- 表单 -->
      <view class="form-area">
        <view class="input-wrap">
          <text class="input-icon">👤</text>
          <input
            v-model="form.username"
            class="input"
            :placeholder="usernamePlaceholder"
            placeholder-class="input-placeholder"
          />
        </view>
        <view class="input-wrap">
          <text class="input-icon">🔒</text>
          <input
            v-model="form.password"
            class="input"
            type="safe-password"
            placeholder="请输入密码"
            placeholder-class="input-placeholder"
            password
          />
        </view>

        <button class="btn-login" :disabled="loading" @click="handleLogin">
          <text v-if="loading">登录中...</text>
          <text v-else>登 录</text>
        </button>

        <view class="register-row">
          <text class="gray-text">还没有账号？</text>
          <text class="link-text" @click="goRegister">免费注册</text>
        </view>
      </view>

      <!-- 演示账号 -->
      <view class="demo-area">
        <view class="demo-title">
          <view class="demo-line" />
          <text class="demo-label">演示账号（密码：123456）</text>
          <view class="demo-line" />
        </view>
        <view class="demo-btns">
          <view class="demo-btn" @click="fillDemo('admin')">管理员</view>
          <view class="demo-btn" @click="fillDemo('teacher')">教师</view>
          <view class="demo-btn" @click="fillDemo('student')">学生</view>
        </view>
      </view>

      <text class="footer-text">华南农业大学软件学院 · 教务系统</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading   = ref(false)

const roles = [
  { label: '学生',   value: 1, icon: '🎒' },
  { label: '教师',   value: 2, icon: '📚' },
  { label: '管理员', value: 3, icon: '⚙️' },
]

const form = reactive({ roleType: 1, username: '', password: '' })

const usernamePlaceholder = computed(() => {
  if (form.roleType === 1) return '请输入学号'
  if (form.roleType === 2) return '请输入工号'
  return '请输入账号'
})

const demoAccounts = {
  admin:   { username: 'admin',        roleType: 3 },
  teacher: { username: 'demo_teacher', roleType: 2 },
  student: { username: 'demo_student', roleType: 1 },
}

function fillDemo(role) {
  Object.assign(form, demoAccounts[role], { password: '123456' })
}

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' })
}

async function handleLogin() {
  if (!form.username.trim()) {
    uni.showToast({ title: '请输入账号', icon: 'none' }); return
  }
  if (!form.password.trim()) {
    uni.showToast({ title: '请输入密码', icon: 'none' }); return
  }
  loading.value = true
  try {
    const data = await userStore.login(form.username, form.password, form.roleType)
    uni.showToast({ title: `欢迎，${data.realName || data.username}！`, icon: 'success', duration: 1500 })
    await new Promise(r => setTimeout(r, 1500))
    if (data.roleType === 3) uni.reLaunch({ url: '/pages/admin/index' })
    else if (data.roleType === 2) uni.reLaunch({ url: '/pages/teacher/index' })
    else uni.reLaunch({ url: '/pages/student/index' })
  } catch {} finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a2a6c 0%, #2d6a9f 50%, #11998e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.bg-circle {
  position: fixed;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
}
.c1 { width: 600rpx; height: 600rpx; top: -150rpx; left: -150rpx; }
.c2 { width: 400rpx; height: 400rpx; bottom: -80rpx; right: -80rpx; }
.c3 { width: 280rpx; height: 280rpx; top: 45%; left: 55%; }

.login-box {
  width: 680rpx;
  background: rgba(255,255,255,0.97);
  border-radius: 32rpx;
  padding: 56rpx 48rpx 40rpx;
  box-shadow: 0 24rpx 80rpx rgba(0,0,0,0.25);
  position: relative;
  z-index: 1;
}
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}
.logo-icon   { font-size: 80rpx; margin-bottom: 12rpx; }
.app-name    { font-size: 36rpx; font-weight: 700; color: #1a2a6c; text-align: center; margin-bottom: 8rpx; }
.subtitle    { font-size: 24rpx; color: #8c9ab0; text-align: center; }

.role-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 36rpx;
  background: #f5f7fa;
  border-radius: 16rpx;
  padding: 8rpx;
}
.role-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #8c9ab0;
  transition: all 0.2s;
}
.role-tab.active {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff;
  box-shadow: 0 4rpx 16rpx rgba(26,42,108,0.3);
}
.role-icon { font-size: 36rpx; margin-bottom: 4rpx; }

.form-area { display: flex; flex-direction: column; gap: 24rpx; }
.input-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #f5f7fa;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  border: 2rpx solid transparent;
}
.input-icon { font-size: 32rpx; flex-shrink: 0; }
.input {
  flex: 1;
  font-size: 28rpx;
  color: #303133;
  background: transparent;
}
.input-placeholder { color: #c0c4cc; font-size: 28rpx; }

.btn-login {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff;
  border-radius: 16rpx;
  height: 96rpx;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
  border: none;
  margin-top: 8rpx;
}
.btn-login:disabled { opacity: 0.7; }

.register-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: -8rpx;
}
.gray-text { font-size: 24rpx; color: #909399; }
.link-text  { font-size: 24rpx; color: #409eff; }

.demo-area { margin: 32rpx 0 24rpx; }
.demo-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.demo-line  { flex: 1; height: 2rpx; background: #ebeef5; }
.demo-label { font-size: 22rpx; color: #c0c4cc; white-space: nowrap; }
.demo-btns  { display: flex; gap: 16rpx; }
.demo-btn {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  background: #ecf5ff;
  color: #409eff;
  font-size: 24rpx;
  border: 2rpx solid #b3d8ff;
}

.footer-text {
  display: block;
  text-align: center;
  font-size: 20rpx;
  color: #c0c4cc;
}
</style>
