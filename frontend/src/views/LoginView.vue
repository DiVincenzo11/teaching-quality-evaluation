<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
      <div class="bg-circle c3"></div>
    </div>
    <div class="login-container">
      <div class="login-banner">
        <img src="/login-banner.png" alt="banner" class="banner-img" />
        <div class="banner-text">
          <h2>教师教学质量评价系统</h2>
          <p>智能分析 · 客观评价 · 促进提升</p>
        </div>
      </div>
      <div class="login-right">
      <div class="login-header">
        <h1>欢迎登录</h1>
        <p class="subtitle">请选择角色并输入账号信息</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="roleType">
          <el-select
            v-model="form.roleType"
            placeholder="请选择登录角色"
            size="large"
            style="width:100%"
          >
            <el-option :value="1" label="学生">
              <el-icon style="margin-right:6px"><User /></el-icon>学生
            </el-option>
            <el-option :value="2" label="教师">
              <el-icon style="margin-right:6px"><Avatar /></el-icon>教师
            </el-option>
            <el-option :value="3" label="管理员">
              <el-icon style="margin-right:6px"><Setting /></el-icon>管理员
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="form.roleType === 1 ? '请输入学号' : form.roleType === 2 ? '请输入工号' : '请输入账号'"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-link">
        还没有账号？
        <el-button text type="primary" size="small" @click="router.push('/register')">免费注册</el-button>
      </div>
      <div class="login-tips">
        <el-tag type="info" size="small">默认密码：123456</el-tag>
        <div class="demo-accounts">
          <el-button text size="small" @click="fillDemo('admin')">管理员</el-button>
          <el-button text size="small" @click="fillDemo('teacher')">教师</el-button>
          <el-button text size="small" @click="fillDemo('student')">学生</el-button>
        </div>
      </div>
      <div class="login-footer">
        <span>华南农业大学软件学院 · 教务系统</span>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router   = useRouter()
const userStore= useUserStore()
const formRef  = ref()
const loading  = ref(false)

const form = reactive({ roleType: null, username: '', password: '' })
const rules = {
  roleType: [{ required: true, message: '请选择登录角色', trigger: 'change' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demoAccounts = {
  admin:   { username: 'admin',        roleType: 3 },
  teacher: { username: 'demo_teacher', roleType: 2 },
  student: { username: 'demo_student', roleType: 1 },
}

function fillDemo(role) {
  form.username = demoAccounts[role].username
  form.password = '123456'
  form.roleType = demoAccounts[role].roleType
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await userStore.login(form.username, form.password, form.roleType)
      ElMessage.success(`欢迎回来，${data.realName || data.username}！`)
      if (data.roleType === 3) router.push('/admin')
      else if (data.roleType === 2) router.push('/teacher')
      else router.push('/student')
    } catch {
      // 错误由 request.js 统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a2a6c 0%, #2d6a9f 50%, #11998e 100%);
  position: relative;
  overflow: hidden;
}
.login-bg { position: absolute; inset: 0; overflow: hidden; }
.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  background: white;
  animation: float 8s ease-in-out infinite;
}
.c1 { width: 400px; height: 400px; top: -100px; left: -100px; animation-delay: 0s; }
.c2 { width: 300px; height: 300px; bottom: -50px; right: -50px; animation-delay: 2s; }
.c3 { width: 200px; height: 200px; top: 50%; left: 60%; animation-delay: 4s; }
@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}
.login-container {
  display: flex;
  background: rgba(255,255,255,0.97);
  border-radius: 20px;
  overflow: hidden;
  width: 860px;
  min-height: 520px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  position: relative;
  z-index: 1;
}

/* 左侧插图区 */
.login-banner {
  flex: 1;
  background: linear-gradient(160deg, #e8f0fe 0%, #dbeafe 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  gap: 24px;
}
.banner-img {
  width: 100%;
  max-width: 340px;
  object-fit: contain;
  border-radius: 12px;
}
.banner-text {
  text-align: center;
}
.banner-text h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a2a6c;
  margin-bottom: 8px;
}
.banner-text p {
  color: #5a7ab0;
  font-size: 13px;
}

/* 右侧表单区 */
.login-right {
  width: 380px;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-header { text-align: center; margin-bottom: 32px; }
.login-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a2a6c;
  margin-bottom: 8px;
}
.subtitle { color: #8c9ab0; font-size: 13px; }
.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  border: none;
  letter-spacing: 4px;
}
.login-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(45,106,159,0.4);
}
.register-link {
  text-align: center;
  margin: -4px 0 8px;
  font-size: 13px;
  color: #8c9ab0;
}
.login-tips {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.demo-accounts { display: flex; gap: 4px; }
.login-footer {
  text-align: center;
  margin-top: 24px;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
