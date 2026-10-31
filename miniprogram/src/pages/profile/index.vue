<template>
  <view class="container" style="padding-bottom: 140rpx;">
    <nav-bar title="个人中心" :back-url="homeTab" />
    <!-- 顶部个人信息 -->
    <view class="profile-header">
      <view class="avatar-big">{{ nameChar }}</view>
      <view class="profile-name-area">
        <text class="profile-name">{{ info.realName || userStore.userInfo?.realName || '用户' }}</text>
        <view class="role-badge" :class="roleBadgeClass">{{ roleLabel }}</view>
      </view>
    </view>

    <!-- 基本信息卡片 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="info-row">
        <text class="info-label">账号</text>
        <text class="info-val">{{ info.username || userStore.userInfo?.username }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">真实姓名</text>
        <text class="info-val">{{ info.realName }}</text>
      </view>
      <view class="info-row" v-if="info.email">
        <text class="info-label">邮箱</text>
        <text class="info-val">{{ info.email }}</text>
      </view>
      <view class="info-row" v-if="info.phone">
        <text class="info-label">手机号</text>
        <text class="info-val">{{ info.phone }}</text>
      </view>
      <view class="info-row" v-if="isStudent && info.className">
        <text class="info-label">班级</text>
        <text class="info-val">{{ info.className }}</text>
      </view>
      <view class="info-row" v-if="isStudent && info.major">
        <text class="info-label">专业</text>
        <text class="info-val">{{ info.major }}</text>
      </view>
      <view class="info-row" v-if="isTeacher && info.department">
        <text class="info-label">院系</text>
        <text class="info-val">{{ info.department }}</text>
      </view>
      <view class="info-row" v-if="isTeacher && info.title">
        <text class="info-label">职称</text>
        <text class="info-val">{{ info.title }}</text>
      </view>
    </view>

    <!-- 编辑资料 -->
    <view class="card">
      <view class="card-title">编辑资料</view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <input v-model="editForm.phone" class="form-input" placeholder="请输入手机号" type="number" />
      </view>
      <view class="form-row">
        <text class="form-label">邮箱</text>
        <input v-model="editForm.email" class="form-input" placeholder="请输入邮箱" />
      </view>
      <button class="btn-save" :disabled="saving" @click="saveProfile" style="margin-top: 24rpx;">
        {{ saving ? '保存中...' : '保存修改' }}
      </button>
    </view>

    <!-- 修改密码 -->
    <view class="card">
      <view class="card-title">修改密码</view>
      <view class="form-row">
        <text class="form-label">原密码</text>
        <input v-model="pwdForm.oldPwd" password class="form-input" placeholder="请输入原密码" />
      </view>
      <view class="form-row">
        <text class="form-label">新密码</text>
        <input v-model="pwdForm.newPwd" password class="form-input" placeholder="请输入新密码（≥6位）" />
      </view>
      <view class="form-row">
        <text class="form-label">确认密码</text>
        <input v-model="pwdForm.confirmPwd" password class="form-input" placeholder="再次输入新密码" />
      </view>
      <button class="btn-primary" style="margin-top: 24rpx;" @click="changePwd">确认修改</button>
    </view>

    <!-- 退出登录 -->
    <view class="card">
      <button class="btn-logout" @click="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { profileApi } from '@/api/index'

const userStore = useUserStore()
const info      = ref({})
const saving    = ref(false)
const editForm  = reactive({ phone: '', email: '' })
const pwdForm   = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' })

const nameChar      = computed(() => (info.value.realName || userStore.userInfo?.realName || '?')[0])
const isStudent     = computed(() => userStore.isStudent)
const homeTab       = computed(() => {
  if (userStore.isAdmin)   return '/pages/admin/index'
  if (userStore.isTeacher) return '/pages/teacher/index'
  return '/pages/student/index'
})
const isTeacher     = computed(() => userStore.isTeacher)
const roleBadgeClass = computed(() => {
  if (userStore.isAdmin)   return 'badge-admin'
  if (userStore.isTeacher) return 'badge-teacher'
  return 'badge-student'
})
const roleLabel = computed(() => {
  if (userStore.isAdmin)   return '管理员'
  if (userStore.isTeacher) return '教师'
  return '学生'
})

onMounted(async () => {
  try {
    const res = await profileApi.getInfo()
    info.value = res.data || {}
    editForm.phone = info.value.phone || ''
    editForm.email = info.value.email || ''
  } catch {}
})

async function saveProfile() {
  saving.value = true
  try {
    const payload = {}
    if (editForm.phone) payload.phone = editForm.phone
    if (editForm.email) payload.email = editForm.email
    await profileApi.update(payload)
    uni.showToast({ title: '保存成功', icon: 'success' })
    info.value.phone = editForm.phone
    info.value.email = editForm.email
  } catch {} finally { saving.value = false }
}

async function changePwd() {
  if (!pwdForm.oldPwd || !pwdForm.newPwd || !pwdForm.confirmPwd) {
    uni.showToast({ title: '请填写完整密码信息', icon: 'none' }); return
  }
  if (pwdForm.newPwd.length < 6) {
    uni.showToast({ title: '新密码不能少于6位', icon: 'none' }); return
  }
  if (pwdForm.newPwd !== pwdForm.confirmPwd) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' }); return
  }
  try {
    await profileApi.changePwd({ oldPassword: pwdForm.oldPwd, newPassword: pwdForm.newPwd })
    uni.showToast({ title: '密码修改成功', icon: 'success' })
    pwdForm.oldPwd = pwdForm.newPwd = pwdForm.confirmPwd = ''
  } catch {}
}

async function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: async ({ confirm }) => {
      if (!confirm) return
      await userStore.logout()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  })
}
</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 28rpx;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;
}
.avatar-big {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  color: #fff;
  font-size: 52rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid rgba(255,255,255,0.4);
  flex-shrink: 0;
}
.profile-name-area { display: flex; flex-direction: column; gap: 10rpx; }
.profile-name { font-size: 40rpx; font-weight: 700; color: #fff; }
.role-badge {
  display: inline-block;
  padding: 4rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
  align-self: flex-start;
}
.badge-student { background: rgba(17,153,142,0.8); color: #fff; }
.badge-teacher { background: rgba(64,158,255,0.8); color: #fff; }
.badge-admin   { background: rgba(230,162,60,0.8); color: #fff; }

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}
.info-row:last-child { border-bottom: none; }
.info-label { font-size: 28rpx; color: #909399; }
.info-val   { font-size: 28rpx; color: #303133; font-weight: 500; }

.form-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
  gap: 16rpx;
}
.form-row:last-child { border-bottom: none; }
.form-label  { font-size: 28rpx; color: #606266; width: 160rpx; flex-shrink: 0; }
.form-input  { flex: 1; font-size: 28rpx; color: #303133; text-align: right; }

.btn-save {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
  border-radius: 16rpx;
  height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  width: 100%;
}
.btn-save:disabled { opacity: 0.7; }

.btn-logout {
  background: linear-gradient(135deg, #f56c6c, #e63946);
  color: #fff;
  border-radius: 16rpx;
  height: 88rpx;
  font-size: 30rpx;
  border: none;
  width: 100%;
}
</style>
