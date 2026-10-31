<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area">
        <img src="/penguin.png" class="logo-penguin" alt="logo" />
        <transition name="fade">
          <span v-if="!isCollapsed" class="logo-text">{{ title }}</span>
        </transition>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        :collapse="isCollapsed"
        router
        background-color="#1a2a6c"
        text-color="#b8c5e8"
        active-text-color="#ffffff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="isCollapsed = !isCollapsed">
            <el-icon size="20"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: homeRoute }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentMenu">{{ currentMenu }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.realName?.[0] || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="changePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { profileApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  title:     { type: String, default: '教学评价系统' },
  menuItems: { type: Array,  default: () => [] },
  homeRoute: { type: String, default: '/' },
})

const route      = useRoute()
const router     = useRouter()
const userStore  = useUserStore()
const isCollapsed= ref(false)
const pwdVisible = ref(false)
const pwdFormRef = ref()

const activeMenu  = computed(() => route.path)
const currentMenu = computed(() => props.menuItems.find(m => m.path === route.path)?.label)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword:     [{ required: true, message: '请输入原密码' }],
  newPassword:     [{ required: true, min: 6, message: '密码不少于6位' }],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    { validator: (_, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')) }
  ],
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdVisible.value = true
  } else if (cmd === 'profile') {
    const role = userStore.userInfo?.roleType
    if (role === 2) router.push('/teacher/profile')
    else if (role === 1) router.push('/student/profile')
  }
}

async function changePwd() {
  await pwdFormRef.value.validate()
  await profileApi.changePwd({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功，请重新登录')
  pwdVisible.value = false
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; overflow: hidden; }
.layout-aside {
  background: #1a2a6c;
  transition: width 0.25s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #16235c;
  padding: 0 16px;
  overflow: hidden;
  white-space: nowrap;
}
.logo-text { color: white; font-size: 15px; font-weight: 700; letter-spacing: 1px; }
.logo-penguin { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.side-menu { border-right: none; flex: 1; overflow-y: auto; }
:deep(.el-menu-item.is-active) { background: rgba(79,126,248,0.3) !important; }
:deep(.el-menu-item:hover)     { background: rgba(255,255,255,0.08) !important; }
.layout-header {
  height: 64px;
  background: white;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.header-left { display: flex; align-items: center; gap: 16px; }
.header-right { display: flex; align-items: center; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-info:hover { background: #f5f7fa; }
.username { font-size: 14px; color: #303133; }
.layout-main { background: #f5f7fa; overflow-y: auto; padding: 20px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
