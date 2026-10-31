<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左栏：头像卡片 -->
      <el-col :span="8" :xs="24">
        <el-card shadow="never" class="avatar-card">
          <div class="avatar-section">
            <div class="avatar-wrap">
              <el-avatar :size="110" :src="previewAvatar || userInfo.avatar" class="big-avatar">
                <span class="avatar-placeholder">{{ userInfo.realName?.[0] || 'U' }}</span>
              </el-avatar>
              <div class="avatar-overlay" @click="triggerUpload">
                <el-icon size="22" color="#fff"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
            <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp"
                   class="hidden-input" @change="onFileChange" />
            <p class="avatar-hint">支持 JPG / PNG / GIF / WEBP，最大 5MB</p>
            <div class="user-badge">
              <el-tag :type="roleTagType" size="large" effect="dark" round>
                <el-icon style="margin-right:4px"><component :is="roleIcon" /></el-icon>
                {{ roleName }}
              </el-tag>
            </div>
            <div class="basic-info">
              <div class="info-row">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ userInfo.realName || '—' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">账号</span>
                <span class="info-value mono">{{ userInfo.username || '—' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ userInfo.createTime || '—' }}</span>
              </div>
              <template v-if="userInfo.roleType === 2 && (teacherExtra.courses?.length || teacherExtra.classes)">
                <div class="info-row">
                  <span class="info-label">所教科目</span>
                  <span class="info-value">{{ teacherExtra.courses?.join('、') || '—' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">所教班级</span>
                  <span class="info-value">{{ teacherExtra.classes || '—' }}</span>
                </div>
              </template>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右栏：信息编辑卡片 -->
      <el-col :span="16" :xs="24">
        <el-card shadow="never" class="edit-card">
          <template #header>
            <span class="card-title">编辑个人信息</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="edit-form">

            <!-- 邮箱 -->
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" clearable>
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>

            <!-- 手机号 -->
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" clearable
                        :disabled="phoneLimit.limited">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
              <div v-if="phoneLimit.limited" class="phone-tip">
                <el-icon color="#e6a23c"><Warning /></el-icon>
                手机号每月只能修改一次，还需等待
                <strong>{{ phoneLimit.remainDays }}</strong> 天
              </div>
              <div v-else class="phone-tip ok">
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
                本月可修改手机号（修改后 30 天内不可再次修改）
              </div>
            </el-form-item>

            <!-- 头像预览提示 -->
            <el-form-item v-if="pendingFile" label="新头像">
              <div class="pending-avatar">
                <el-avatar :size="56" :src="previewAvatar" />
                <div class="pending-info">
                  <span class="file-name">{{ pendingFile.name }}</span>
                  <el-button text type="danger" size="small" @click="cancelAvatar">取消</el-button>
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave" class="save-btn">
                <el-icon><Check /></el-icon> 保存修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 提示卡 -->
        <el-card shadow="never" class="tips-card">
          <template #header><span class="card-title">修改说明</span></template>
          <ul class="tips-list">
            <li><el-icon color="#4f7ef8"><InfoFilled /></el-icon> 头像支持上传本地图片，大小不超过 5MB。</li>
            <li><el-icon color="#e6a23c"><Warning /></el-icon> 手机号每 <strong>30 天</strong>只能修改一次，请谨慎操作。</li>
            <li><el-icon color="#67c23a"><CircleCheck /></el-icon> 邮箱修改无次数限制，随时可更改。</li>
            <li><el-icon color="#909399"><Lock /></el-icon> 修改密码请通过右上角下拉菜单操作。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { profileApi, teacherApi } from '@/api/index'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef   = ref()
const fileInput = ref()
const saving    = ref(false)

const userInfo = reactive({
  realName: '', username: '', email: '', phone: '',
  avatar: '', roleType: null, createTime: '',
})

const form = reactive({ email: '', phone: '' })

const phoneLimit  = reactive({ limited: false, remainDays: 0 })
const teacherExtra = reactive({ courses: [], classes: '' })

// 待上传的文件和预览
const pendingFile   = ref(null)
const previewAvatar = ref('')

const roleName    = computed(() => ({ 1: '学生', 2: '教师', 3: '管理员' })[userInfo.roleType] || '—')
const roleTagType = computed(() => ({ 1: 'success', 2: 'primary', 3: 'danger' })[userInfo.roleType] || 'info')
const roleIcon    = computed(() => ({ 1: 'User', 2: 'Avatar', 3: 'Setting' })[userInfo.roleType] || 'User')

const rules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

async function loadProfile() {
  const res = await profileApi.getInfo()
  Object.assign(userInfo, res.data)
  form.email = res.data.email || ''
  form.phone = res.data.phone || ''
  if (res.data.roleType === 2) {
    try {
      const tr = await teacherApi.info()
      if (tr?.data) {
        teacherExtra.courses = tr.data.courses || []
        teacherExtra.classes = tr.data.classes || ''
      }
    } catch (_) { /* 教师接口可能不可用 */ }
  }
}

async function loadPhoneLimit() {
  const res = await profileApi.getPhoneLimit()
  phoneLimit.limited    = res.data.limited
  phoneLimit.remainDays = res.data.remainDays
}

function triggerUpload() {
  fileInput.value.click()
}

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }
  pendingFile.value = file
  previewAvatar.value = URL.createObjectURL(file)
  // 清空 input，允许重复选同一文件
  e.target.value = ''
}

function cancelAvatar() {
  pendingFile.value   = null
  previewAvatar.value = ''
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    // 1. 先上传头像（如有）
    if (pendingFile.value) {
      const res = await profileApi.uploadAvatar(pendingFile.value)
      userInfo.avatar = res.data
      // 同步更新全局 store（让头像立即生效）
      if (userStore.userInfo) userStore.userInfo.avatar = res.data
      pendingFile.value   = null
      previewAvatar.value = ''
    }

    // 2. 保存邮箱 / 手机号
    const payload = {}
    if (form.email !== userInfo.email) payload.email = form.email
    if (!phoneLimit.limited && form.phone !== userInfo.phone) payload.phone = form.phone

    if (Object.keys(payload).length > 0) {
      await profileApi.update(payload)
      Object.assign(userInfo, payload)
      // 同步 store
      if (userStore.userInfo) Object.assign(userStore.userInfo, payload)
      await loadPhoneLimit()
    }

    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.email = userInfo.email || ''
  form.phone = userInfo.phone || ''
  cancelAvatar()
}

onMounted(async () => {
  await loadProfile()
  await loadPhoneLimit()
})
</script>

<style scoped>
.profile-page { padding: 4px 0; }

/* 头像卡片 */
.avatar-card { border-radius: 12px; }
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0 8px;
  gap: 12px;
}
.avatar-wrap {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  cursor: pointer;
}
.big-avatar {
  width: 110px !important;
  height: 110px !important;
  font-size: 40px;
  border: 3px solid #e4e7ed;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 12px;
}
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.hidden-input { display: none; }
.avatar-hint { font-size: 12px; color: #909399; margin: 0; }
.avatar-placeholder { font-size: 36px; }

.user-badge { margin: 4px 0; }
.basic-info { width: 100%; border-top: 1px solid #f0f0f0; padding-top: 12px; }
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px dashed #f5f5f5;
  font-size: 13px;
}
.info-row:last-child { border-bottom: none; }
.info-label { color: #909399; }
.info-value { color: #303133; font-weight: 500; }
.info-value.mono { font-family: monospace; font-size: 13px; color: #4f7ef8; }

/* 编辑卡片 */
.edit-card { border-radius: 12px; margin-bottom: 16px; }
.card-title { font-size: 15px; font-weight: 600; color: #303133; }
.edit-form { padding-top: 8px; }
.save-btn { min-width: 110px; }

.phone-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #e6a23c;
  margin-top: 6px;
}
.phone-tip.ok { color: #67c23a; }

.pending-avatar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px dashed #d0d7de;
}
.pending-info { display: flex; flex-direction: column; gap: 4px; }
.file-name { font-size: 13px; color: #303133; }

/* 提示卡 */
.tips-card { border-radius: 12px; }
.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tips-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}
</style>
