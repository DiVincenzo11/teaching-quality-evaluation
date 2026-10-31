<template>
  <view class="page-bg">
    <view class="content">
      <!-- 筛选栏 -->
      <view class="card filter-bar">
        <view class="role-tabs">
          <view v-for="r in roleOpts" :key="r.value" class="role-tab"
                :class="{ active: filterRole === r.value }" @click="changeRole(r.value)">
            {{ r.label }}
          </view>
        </view>
        <view class="search-wrap">
          <text>🔍</text>
          <input v-model="keyword" class="search-input" placeholder="搜索姓名/账号"
                 placeholder-class="ph" @input="onSearch" />
          <text v-if="keyword" class="clear-btn" @click="clearSearch">✕</text>
        </view>
      </view>

      <!-- 新增按钮 -->
      <view class="action-bar">
        <button class="btn-add" @click="openCreate">+ 新增用户</button>
      </view>

      <view v-if="loading" class="loading-box"><text>加载中...</text></view>
      <view v-else-if="!list.length" class="empty-box">
        <text class="empty-icon">👥</text>
        <text>暂无用户数据</text>
      </view>
      <view v-for="u in list" :key="u.id" class="user-card">
        <view class="uc-left">
          <view class="uc-avatar" :class="avatarClass(u.roleType)">{{ u.realName?.[0] || '?' }}</view>
        </view>
        <view class="uc-body">
          <view class="uc-name-row">
            <text class="uc-name">{{ u.realName }}</text>
            <view class="role-badge" :class="roleBadgeClass(u.roleType)">{{ roleName(u.roleType) }}</view>
          </view>
          <text class="uc-account">账号：{{ u.username }}</text>
          <text class="uc-info" v-if="u.className">班级：{{ u.className }}</text>
          <text class="uc-info" v-if="u.department">院系：{{ u.department }}</text>
        </view>
        <view class="uc-actions">
          <view class="status-dot" :class="u.status === 1 ? 'dot-active' : 'dot-disabled'" @click="toggleStatus(u)" />
          <text class="action-btn edit-btn" @click="openEdit(u)">编辑</text>
          <text class="action-btn del-btn" @click="deleteUser(u)">删除</text>
        </view>
      </view>

      <view v-if="total > pageSize" class="pagination">
        <text class="page-info">{{ page }}/{{ Math.ceil(total/pageSize) }} 页，共{{ total }}条</text>
        <view class="page-btns">
          <view class="page-btn" :class="{ disabled: page <= 1 }" @click="prevPage">上一页</view>
          <view class="page-btn" :class="{ disabled: page * pageSize >= total }" @click="nextPage">下一页</view>
        </view>
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view v-if="dialogVisible" class="modal-mask">
      <view class="modal-overlay" @click="dialogVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">{{ editId ? '编辑用户' : '新增用户' }}</text>
          <text class="modal-close" @click="dialogVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 70vh;">
          <view style="padding: 24rpx 40rpx;">
            <view class="form-row">
              <text class="form-label">账号</text>
              <input v-model="form.username" class="form-input" :placeholder="editId ? '不填则不修改' : '请输入账号'" />
            </view>
            <view class="form-row">
              <text class="form-label">真实姓名</text>
              <input v-model="form.realName" class="form-input" placeholder="请输入真实姓名" />
            </view>
            <view v-if="!editId" class="form-row">
              <text class="form-label">角色</text>
              <picker mode="selector" :range="['学生','教师','管理员']" :value="roleIndex" @change="onRoleChange">
                <view class="picker-val">
                  <text>{{ ['学生','教师','管理员'][roleIndex] }}</text>
                  <text class="arrow">›</text>
                </view>
              </picker>
            </view>
            <view v-if="!editId" class="form-row">
              <text class="form-label">密码</text>
              <input v-model="form.password" class="form-input" placeholder="初始密码" password />
            </view>
            <view class="form-row">
              <text class="form-label">手机号</text>
              <input v-model="form.phone" class="form-input" placeholder="选填" type="number" />
            </view>
            <view class="form-row">
              <text class="form-label">邮箱</text>
              <input v-model="form.email" class="form-input" placeholder="选填" />
            </view>
            <!-- 学生字段 -->
            <template v-if="form.roleType === 1">
              <view class="form-row">
                <text class="form-label">班级</text>
                <input v-model="form.className" class="form-input" placeholder="例：软件2022-1班" />
              </view>
              <view class="form-row">
                <text class="form-label">专业</text>
                <input v-model="form.major" class="form-input" placeholder="例：软件工程" />
              </view>
            </template>
            <!-- 教师字段 -->
            <template v-if="form.roleType === 2">
              <view class="form-row">
                <text class="form-label">院系</text>
                <input v-model="form.department" class="form-input" placeholder="例：软件学院" />
              </view>
              <view class="form-row">
                <text class="form-label">职称</text>
                <input v-model="form.title" class="form-input" placeholder="例：副教授" />
              </view>
            </template>
          </view>
        </scroll-view>
        <view class="modal-footer">
          <button class="btn-cancel-m" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm-m" :disabled="saving" @click="saveUser">
            {{ saving ? '保存中...' : '确认保存' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminUserApi } from '@/api/index'

const list      = ref([])
const loading   = ref(false)
const total     = ref(0)
const page      = ref(1)
const pageSize  = 10
const keyword   = ref('')
const filterRole = ref(0)
const dialogVisible = ref(false)
const editId    = ref(null)
const saving    = ref(false)
const roleIndex = ref(0)

const roleOpts = [
  { label: '全部', value: 0 },
  { label: '学生', value: 1 },
  { label: '教师', value: 2 },
  { label: '管理员', value: 3 },
]

const form = reactive({
  username: '', realName: '', password: '', phone: '', email: '',
  roleType: 1, className: '', major: '', department: '', title: ''
})

function roleName(r) { return { 1: '学生', 2: '教师', 3: '管理员' }[r] || '未知' }
function roleBadgeClass(r) { return { 1: 'badge-student', 2: 'badge-teacher', 3: 'badge-admin' }[r] || '' }
function avatarClass(r) { return { 1: 'av-student', 2: 'av-teacher', 3: 'av-admin' }[r] || '' }

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (filterRole.value) params.roleType = filterRole.value
    if (keyword.value) params.keyword = keyword.value
    const res = await adminUserApi.page(params)
    list.value  = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function changeRole(v) { filterRole.value = v; page.value = 1; loadList() }
function onSearch()    { page.value = 1; loadList() }
function clearSearch() { keyword.value = ''; loadList() }
function prevPage()    { if (page.value > 1) { page.value--; loadList() } }
function nextPage()    { if (page.value * pageSize < total.value) { page.value++; loadList() } }

function openCreate() {
  editId.value = null; roleIndex.value = 0
  Object.assign(form, { username:'', realName:'', password:'', phone:'', email:'', roleType:1, className:'', major:'', department:'', title:'' })
  dialogVisible.value = true
}

function openEdit(u) {
  editId.value = u.id
  roleIndex.value = (u.roleType || 1) - 1
  Object.assign(form, {
    username: u.username, realName: u.realName, password: '',
    phone: u.phone || '', email: u.email || '', roleType: u.roleType,
    className: u.className || '', major: u.major || '',
    department: u.department || '', title: u.title || ''
  })
  dialogVisible.value = true
}

function onRoleChange(e) {
  roleIndex.value = e.detail.value
  form.roleType = [1, 2, 3][roleIndex.value]
}

async function saveUser() {
  if (!form.realName.trim()) { uni.showToast({ title: '请输入真实姓名', icon: 'none' }); return }
  if (!editId.value && !form.username.trim()) { uni.showToast({ title: '请输入账号', icon: 'none' }); return }
  saving.value = true
  try {
    const payload = {
      realName: form.realName.trim(),
      phone:    form.phone  || undefined,
      email:    form.email  || undefined,
    }
    if (!editId.value) {
      payload.username = form.username.trim()
      payload.password = form.password || '123456'
      payload.roleType = form.roleType
      if (form.roleType === 1) { payload.className = form.className; payload.major = form.major }
      if (form.roleType === 2) { payload.department = form.department; payload.title = form.title }
      await adminUserApi.create(payload)
    } else {
      if (form.roleType === 1) { payload.className = form.className; payload.major = form.major }
      if (form.roleType === 2) { payload.department = form.department; payload.title = form.title }
      await adminUserApi.update(editId.value, payload)
    }
    uni.showToast({ title: editId.value ? '更新成功' : '创建成功', icon: 'success' })
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    uni.showToast({ title: e?.message || '操作失败，请重试', icon: 'none', duration: 2500 })
  } finally { saving.value = false }
}

async function toggleStatus(u) {
  const newStatus = u.status === 1 ? 0 : 1
  const msg = newStatus === 1 ? '确认启用该用户？' : '确认禁用该用户？'
  uni.showModal({ title: '确认操作', content: msg, success: async ({ confirm }) => {
    if (!confirm) return
    await adminUserApi.updateStatus(u.id, newStatus)
    u.status = newStatus
    uni.showToast({ title: '操作成功', icon: 'success' })
  }})
}

async function deleteUser(u) {
  uni.showModal({ title: '确认删除', content: `确认删除用户 ${u.realName}？此操作不可恢复`, success: async ({ confirm }) => {
    if (!confirm) return
    try {
      await adminUserApi.delete(u.id)
      list.value = list.value.filter(item => item.id !== u.id)
      uni.showToast({ title: '删除成功', icon: 'success' })
      await loadList()
    } catch {
      uni.showToast({ title: '删除失败', icon: 'none' })
    }
  }})
}

onMounted(loadList)
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 40rpx; }
.content { padding: 24rpx; }

.filter-bar { padding: 20rpx 24rpx; }
.role-tabs  { display: flex; gap: 8rpx; margin-bottom: 16rpx; overflow-x: auto; }
.role-tab   { padding: 10rpx 24rpx; border-radius: 20rpx; font-size: 24rpx; color: #606266; background: #f5f7fa; border: 2rpx solid #ebeef5; white-space: nowrap; flex-shrink: 0; }
.role-tab.active { background: #1a2a6c; color: #fff; border-color: #1a2a6c; }
.search-wrap { display: flex; align-items: center; gap: 12rpx; background: #f5f7fa; border-radius: 40rpx; padding: 14rpx 20rpx; }
.search-input { flex: 1; font-size: 26rpx; color: #303133; background: transparent; }
.ph { color: #c0c4cc; font-size: 26rpx; }
.clear-btn { font-size: 24rpx; color: #c0c4cc; }

.action-bar { margin-bottom: 16rpx; }
.btn-add {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff; border-radius: 12rpx; height: 80rpx;
  font-size: 28rpx; font-weight: 600; border: none; width: 100%;
}

.user-card {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 16rpx; display: flex; gap: 16rpx; align-items: flex-start;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
}
.uc-avatar {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  color: #fff; font-size: 28rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.av-student { background: linear-gradient(135deg, #11998e, #38ef7d); }
.av-teacher { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); }
.av-admin   { background: linear-gradient(135deg, #e6a23c, #f5a623); }
.uc-body { flex: 1; overflow: hidden; }
.uc-name-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.uc-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.role-badge { font-size: 20rpx; padding: 3rpx 12rpx; border-radius: 4rpx; }
.badge-student { background: #ecf5ff; color: #409eff; }
.badge-teacher { background: #f0f9eb; color: #67c23a; }
.badge-admin   { background: #fdf6ec; color: #e6a23c; }
.uc-account { display: block; font-size: 24rpx; color: #909399; margin-bottom: 4rpx; }
.uc-info    { display: block; font-size: 22rpx; color: #c0c4cc; }
.uc-actions { display: flex; flex-direction: column; align-items: center; gap: 10rpx; flex-shrink: 0; }
.status-dot { width: 24rpx; height: 24rpx; border-radius: 50%; }
.dot-active   { background: #67c23a; }
.dot-disabled { background: #c0c4cc; }
.action-btn { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 8rpx; }
.edit-btn { color: #409eff; background: #ecf5ff; }
.del-btn  { color: #f56c6c; background: #fef0f0; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; }
.page-info  { font-size: 24rpx; color: #909399; }
.page-btns  { display: flex; gap: 12rpx; }
.page-btn   { padding: 12rpx 24rpx; border-radius: 8rpx; background: #1a2a6c; color: #fff; font-size: 24rpx; }
.page-btn.disabled { background: #ebeef5; color: #c0c4cc; }

.modal-mask   { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box    { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; max-height: 85vh; display: flex; flex-direction: column; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; flex-shrink: 0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.modal-footer { display: flex; gap: 16rpx; padding: 24rpx 40rpx; border-top: 2rpx solid #f0f0f0; flex-shrink: 0; }
.modal-footer button { flex: 1; border-radius: 12rpx; height: 88rpx; font-size: 28rpx; border: none; }
.btn-cancel-m  { background: #f5f7fa; color: #606266; border: 2rpx solid #dcdfe6 !important; }
.btn-confirm-m { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; }
.btn-confirm-m:disabled { opacity: 0.6; }
.form-row    { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; gap: 16rpx; }
.form-row:last-child { border-bottom: none; }
.form-label  { font-size: 28rpx; color: #606266; width: 160rpx; flex-shrink: 0; }
.form-input  { flex: 1; font-size: 28rpx; color: #303133; text-align: right; }
.picker-val  { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #303133; }
.arrow { color: #c0c4cc; }
</style>

