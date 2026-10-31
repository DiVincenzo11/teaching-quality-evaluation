<template>
  <div class="reg-page">
    <div class="reg-bg">
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
      <div class="bg-circle c3"></div>
    </div>
    <div class="reg-container">
      <div class="reg-header">
        <img src="/penguin.png" class="logo-penguin" alt="logo" />
        <h1>注册账号</h1>
        <p class="subtitle">教师教学质量评价系统</p>
      </div>

      <!-- 角色选择 -->
      <div class="role-tabs">
        <div
          class="role-tab"
          :class="{ active: form.roleType === 1 }"
          @click="form.roleType = 1"
        >
          <el-icon><User /></el-icon>
          <span>学生注册</span>
        </div>
        <div
          class="role-tab"
          :class="{ active: form.roleType === 2 }"
          @click="form.roleType = 2"
        >
          <el-icon><Avatar /></el-icon>
          <span>教师注册</span>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="reg-form"
      >
        <!-- 基本信息 -->
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" size="large" clearable>
            <template #prefix><el-icon><UserFilled /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="form.roleType === 1 ? '学号（登录账号）' : '工号（登录账号）'" size="large" clearable>
            <template #prefix><el-icon><Postcard /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="设置密码（6-20位）" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱（选填）" size="large" clearable>
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="phone">
              <el-input v-model="form.phone" placeholder="手机号（选填）" size="large" clearable>
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 教师额外信息 -->
        <template v-if="form.roleType === 2">
          <el-divider content-position="left"><span class="divider-text">教师信息</span></el-divider>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item>
                <el-input v-model="form.department" placeholder="所属院系" size="large" clearable>
                  <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item>
                <el-select v-model="form.title" placeholder="职称" size="large" style="width:100%">
                  <el-option label="助教" value="助教" />
                  <el-option label="讲师" value="讲师" />
                  <el-option label="副教授" value="副教授" />
                  <el-option label="教授" value="教授" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-input v-model="form.courses" placeholder="所授课程（逗号分隔，如：数据结构,算法导论）" size="large" clearable>
              <template #prefix><el-icon><Reading /></el-icon></template>
            </el-input>
          </el-form-item>
        </template>

        <!-- 学生额外信息 -->
        <template v-if="form.roleType === 1">
          <el-divider content-position="left"><span class="divider-text">学生信息</span></el-divider>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item>
                <el-input v-model="form.major" placeholder="专业" size="large" clearable>
                  <template #prefix><el-icon><Notebook /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item>
                <el-input v-model="form.grade" placeholder="年级（如 2022）" size="large" clearable>
                  <template #prefix><el-icon><Calendar /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item>
            <el-input v-model="form.className" placeholder="班级（如 软件工程2班）" size="large" clearable>
              <template #prefix><el-icon><School /></el-icon></template>
            </el-input>
          </el-form-item>
        </template>

        <el-form-item style="margin-top:8px">
          <el-button
            type="primary"
            size="large"
            class="reg-btn"
            :loading="loading"
            @click="handleRegister"
          >
            立即注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="reg-footer">
        已有账号？
        <el-button text type="primary" @click="router.push('/login')">立即登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/index'
import { ElMessage } from 'element-plus'

const router    = useRouter()
const userStore = useUserStore()
const formRef   = ref()
const loading   = ref(false)

const form = reactive({
  roleType: 1,
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  // teacher
  teacherNo: '',
  department: '',
  title: '讲师',
  courses: '',
  // student
  studentNo: '',
  className: '',
  major: '',
  grade: '',
})

const validateConfirm = (rule, value, cb) => {
  if (value !== form.password) cb(new Error('两次密码不一致'))
  else cb()
}

const rules = {
  realName:        [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  username:        [{ required: true, message: '请输入账号', trigger: 'blur' },
                    { min: 4, max: 30, message: '账号长度 4-30 位', trigger: 'blur' }],
  password:        [{ required: true, message: '请设置密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' },
                    { validator: validateConfirm, trigger: 'blur' }],
}

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = { ...form }
      if (form.roleType === 1) payload.studentNo = form.username
      else payload.teacherNo = form.username
      delete payload.confirmPassword

      const res = await authApi.register(payload)
      const data = res.data
      // 注册成功后自动登录
      userStore.token    = data.token
      userStore.userInfo = { userId: data.userId, username: data.username, realName: data.realName, roleType: data.roleType, avatar: data.avatar }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

      ElMessage.success(`注册成功，欢迎 ${data.realName}！`)
      if (data.roleType === 2) router.push('/teacher')
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
.reg-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a2a6c 0%, #2d6a9f 50%, #11998e 100%);
  position: relative;
  overflow: hidden;
  padding: 24px 0;
}
.reg-bg { position: absolute; inset: 0; overflow: hidden; }
.bg-circle {
  position: absolute; border-radius: 50%; opacity: 0.1;
  background: white; animation: float 8s ease-in-out infinite;
}
.c1 { width: 400px; height: 400px; top: -100px; left: -100px; }
.c2 { width: 300px; height: 300px; bottom: -50px; right: -50px; animation-delay: 2s; }
.c3 { width: 200px; height: 200px; top: 50%; left: 60%; animation-delay: 4s; }
@keyframes float {
  0%,100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-20px) rotate(10deg); }
}
.reg-container {
  background: rgba(255,255,255,0.97);
  border-radius: 20px;
  padding: 40px 40px 32px;
  width: 520px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  position: relative;
  z-index: 1;
}
.reg-header { text-align: center; margin-bottom: 20px; }
.logo-penguin { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; }
.reg-header h1 { font-size: 22px; font-weight: 700; color: #1a2a6c; margin: 10px 0 6px; }
.subtitle { color: #8c9ab0; font-size: 13px; }

.role-tabs {
  display: flex; gap: 12px; margin-bottom: 20px;
}
.role-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px; border: 2px solid #e4e7ed; border-radius: 10px;
  cursor: pointer; color: #606266; font-size: 14px; transition: all .2s;
}
.role-tab.active {
  border-color: #4f7ef8; color: #4f7ef8;
  background: rgba(79,126,248,.06);
}
.role-tab:hover { border-color: #4f7ef8; color: #4f7ef8; }

.reg-form :deep(.el-input__wrapper) { border-radius: 10px; padding: 4px 12px; }
.reg-btn {
  width: 100%; height: 46px; border-radius: 10px;
  font-size: 15px; font-weight: 600;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  border: none; letter-spacing: 2px;
}
.reg-btn:hover { opacity: 0.9; transform: translateY(-1px); }
.divider-text { font-size: 12px; color: #909399; }
.reg-footer { text-align: center; margin-top: 12px; font-size: 13px; color: #8c9ab0; }
</style>
