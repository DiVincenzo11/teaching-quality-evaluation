<template>
  <view class="page">
    <view class="header">
      <view class="back-btn" @click="goBack">‹ 返回登录</view>
      <text class="header-title">创建账号</text>
    </view>

    <scroll-view scroll-y class="scroll">
      <view class="content">
        <!-- 角色选择 -->
        <view class="card">
          <view class="section-title">选择角色</view>
          <view class="role-tabs">
            <view
              v-for="r in roles"
              :key="r.value"
              class="role-tab"
              :class="{ active: form.roleType === r.value }"
              @click="form.roleType = r.value"
            >
              <text class="ri">{{ r.icon }}</text>
              <text class="rl">{{ r.label }}</text>
            </view>
          </view>
        </view>

        <!-- 基本信息 -->
        <view class="card">
          <view class="section-title">基本信息</view>
          <view class="form-row">
            <text class="form-label">{{ accountLabel }}</text>
            <input v-model="form.username" class="form-input" :placeholder="accountPlaceholder" />
          </view>
          <view class="form-row">
            <text class="form-label">真实姓名</text>
            <input v-model="form.realName" class="form-input" placeholder="请输入真实姓名" />
          </view>
          <view class="form-row">
            <text class="form-label">手机号</text>
            <input v-model="form.phone" class="form-input" placeholder="选填" type="number" />
          </view>
          <view class="form-row">
            <text class="form-label">邮箱</text>
            <input v-model="form.email" class="form-input" placeholder="选填" />
          </view>
        </view>

        <!-- 学生专属 -->
        <view v-if="form.roleType === 1" class="card">
          <view class="section-title">学生信息</view>
          <view class="form-row">
            <text class="form-label">班级</text>
            <input v-model="form.className" class="form-input" placeholder="例：软件2022-1班" />
          </view>
          <view class="form-row">
            <text class="form-label">专业</text>
            <input v-model="form.major" class="form-input" placeholder="例：软件工程" />
          </view>
          <view class="form-row">
            <text class="form-label">年级</text>
            <input v-model="form.grade" class="form-input" placeholder="例：2022" type="number" />
          </view>
        </view>

        <!-- 教师专属 -->
        <view v-if="form.roleType === 2" class="card">
          <view class="section-title">教师信息</view>
          <view class="form-row">
            <text class="form-label">院系</text>
            <input v-model="form.department" class="form-input" placeholder="例：软件学院" />
          </view>
          <view class="form-row">
            <text class="form-label">职称</text>
            <picker mode="selector" :range="titleOptions" :value="titleIndex" @change="onTitleChange">
              <view class="picker-val">
                <text>{{ titleOptions[titleIndex] }}</text>
                <text class="arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 密码 -->
        <view class="card">
          <view class="section-title">设置密码</view>
          <view class="form-row">
            <text class="form-label">登录密码</text>
            <input v-model="form.password" class="form-input" placeholder="至少6位" password />
          </view>
          <view class="form-row">
            <text class="form-label">确认密码</text>
            <input v-model="form.confirmPwd" class="form-input" placeholder="再次输入密码" password />
          </view>
        </view>

        <view class="submit-area">
          <button class="btn-submit" :disabled="loading" @click="handleRegister">
            {{ loading ? '注册中...' : '立即注册' }}
          </button>
          <view class="login-hint">
            <text class="gray-text">已有账号？</text>
            <text class="link-text" @click="goBack">立即登录</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, computed, ref } from 'vue'
import { authApi } from '@/api/index'

const loading = ref(false)
const titleIndex = ref(0)
const titleOptions = ['助教', '讲师', '副教授', '教授', '研究员']

const roles = [
  { label: '学生',   value: 1, icon: '🎒' },
  { label: '教师',   value: 2, icon: '📚' },
]

const form = reactive({
  roleType:    1,
  username:    '',
  realName:    '',
  phone:       '',
  email:       '',
  password:    '',
  confirmPwd:  '',
  className:   '',
  major:       '',
  grade:       '',
  department:  '',
  title:       '讲师',
})

const accountLabel = computed(() => form.roleType === 1 ? '学号' : '工号')
const accountPlaceholder = computed(() => form.roleType === 1 ? '请输入学号' : '请输入工号')

function onTitleChange(e) {
  titleIndex.value = e.detail.value
  form.title = titleOptions[titleIndex.value]
}

function goBack() { uni.navigateBack() }

function validate() {
  if (!form.username.trim()) { uni.showToast({ title: '请输入账号', icon: 'none' }); return false }
  if (!form.realName.trim()) { uni.showToast({ title: '请输入真实姓名', icon: 'none' }); return false }
  if (!form.password) { uni.showToast({ title: '请输入密码', icon: 'none' }); return false }
  if (form.password.length < 6) { uni.showToast({ title: '密码至少6位', icon: 'none' }); return false }
  if (form.password !== form.confirmPwd) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); return false }
  if (form.roleType === 1 && !form.className.trim()) { uni.showToast({ title: '请输入班级', icon: 'none' }); return false }
  if (form.roleType === 2 && !form.department.trim()) { uni.showToast({ title: '请输入院系', icon: 'none' }); return false }
  return true
}

async function handleRegister() {
  if (!validate()) return
  loading.value = true
  try {
    const payload = {
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      phone:    form.phone  || undefined,
      email:    form.email  || undefined,
      roleType: form.roleType,
    }
    if (form.roleType === 1) {
      payload.className = form.className.trim()
      payload.major     = form.major    || undefined
      payload.grade     = form.grade    ? Number(form.grade) : undefined
    }
    if (form.roleType === 2) {
      payload.department = form.department.trim()
      payload.title      = form.title
    }
    await authApi.register(payload)
    uni.showToast({ title: '注册成功，请登录', icon: 'success', duration: 1800 })
    await new Promise(r => setTimeout(r, 1800))
    uni.navigateBack()
  } catch {} finally { loading.value = false }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f6fa; display: flex; flex-direction: column; }
.header {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  padding: 80rpx 40rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.back-btn  { color: rgba(255,255,255,0.85); font-size: 28rpx; }
.header-title { flex: 1; font-size: 36rpx; font-weight: 700; color: #fff; }

.scroll { flex: 1; }
.content { padding: 24rpx 24rpx 40rpx; }

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1a2a6c;
  margin-bottom: 20rpx;
  padding-bottom: 12rpx;
  border-bottom: 3rpx solid #1a2a6c;
}

.role-tabs {
  display: flex;
  gap: 20rpx;
}
.role-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 2rpx solid #ebeef5;
  background: #f5f7fa;
  transition: all 0.2s;
}
.role-tab.active {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  border-color: #1a2a6c;
}
.ri { font-size: 48rpx; margin-bottom: 8rpx; }
.rl { font-size: 26rpx; color: #606266; }
.role-tab.active .rl { color: #fff; }

.form-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
  gap: 16rpx;
}
.form-row:last-child { border-bottom: none; }
.form-label {
  font-size: 28rpx;
  color: #606266;
  width: 160rpx;
  flex-shrink: 0;
}
.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #303133;
  text-align: right;
}
.picker-val {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: #303133;
}
.arrow { color: #c0c4cc; }

.submit-area { padding-top: 8rpx; }
.btn-submit {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff;
  border-radius: 16rpx;
  height: 96rpx;
  font-size: 32rpx;
  font-weight: 700;
  border: none;
  width: 100%;
  margin-bottom: 24rpx;
}
.btn-submit:disabled { opacity: 0.7; }
.login-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}
.gray-text { font-size: 24rpx; color: #909399; }
.link-text  { font-size: 24rpx; color: #409eff; }
</style>
