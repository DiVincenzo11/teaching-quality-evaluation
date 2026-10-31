<template>
  <view class="container" style="padding-bottom: 40rpx;">
    <view class="action-bar">
      <button class="btn-add" @click="openCreate">+ 新增学期</button>
    </view>

    <view v-if="loading" class="loading-box"><text>加载中...</text></view>
    <view v-else-if="!list.length" class="empty-box">
      <text class="empty-icon">📅</text>
      <text>暂无学期数据</text>
    </view>
    <view v-for="s in list" :key="s.id" class="sem-card">
      <view class="sc-left">
        <view class="sc-dot" :class="s.isCurrent === 1 ? 'dot-current' : 'dot-normal'" />
      </view>
      <view class="sc-body">
        <view class="sc-name-row">
          <text class="sc-name">{{ s.semesterName }}</text>
          <view v-if="s.isCurrent === 1" class="current-badge">当前</view>
        </view>
        <view class="sc-meta-row">
          <view class="eval-badge" :class="s.allowEvaluation === 1 ? 'allow-badge' : 'deny-badge'">
            {{ s.allowEvaluation === 1 ? '评价开放' : '评价关闭' }}
          </view>
        </view>
        <text class="sc-time">{{ s.startDate }} ~ {{ s.endDate }}</text>
      </view>
      <view class="sc-actions">
        <text v-if="s.isCurrent !== 1" class="action-btn set-btn" @click="setCurrent(s)">设为当前</text>
        <text class="action-btn edit-btn" @click="openEdit(s)">编辑</text>
        <text v-if="s.isCurrent !== 1" class="action-btn del-btn" @click="deleteSem(s)">删除</text>
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view v-if="dialogVisible" class="modal-mask">
      <view class="modal-overlay" @click="dialogVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">{{ editId ? '编辑学期' : '新增学期' }}</text>
          <text class="modal-close" @click="dialogVisible = false">✕</text>
        </view>
        <view style="padding: 24rpx 40rpx;">
          <view class="form-row">
            <text class="form-label">学期名称</text>
            <input v-model="form.semesterName" class="form-input" placeholder="例：2024-2025学年第一学期" />
          </view>
          <view class="form-row">
            <text class="form-label">开始日期</text>
            <picker mode="date" :value="form.startDate" @change="e => form.startDate = e.detail.value">
              <view class="picker-val">
                <text>{{ form.startDate || '请选择' }}</text>
                <text class="arrow">›</text>
              </view>
            </picker>
          </view>
          <view class="form-row">
            <text class="form-label">结束日期</text>
            <picker mode="date" :value="form.endDate" @change="e => form.endDate = e.detail.value">
              <view class="picker-val">
                <text>{{ form.endDate || '请选择' }}</text>
                <text class="arrow">›</text>
              </view>
            </picker>
          </view>
          <view class="form-row">
            <text class="form-label">开放评价</text>
            <switch :checked="form.allowEvaluation === 1" @change="e => form.allowEvaluation = e.detail.value ? 1 : 0" />
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn-cancel-m" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm-m" :disabled="saving" @click="saveSem">
            {{ saving ? '保存中...' : '确认保存' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminSemesterApi } from '@/api/index'

const list    = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId  = ref(null)
const saving  = ref(false)

const form = reactive({ semesterName: '', startDate: '', endDate: '', allowEvaluation: 1 })

async function loadList() {
  loading.value = true
  try {
    const res = await adminSemesterApi.list()
    list.value = res.data || []
  } finally { loading.value = false }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { semesterName: '', startDate: '', endDate: '', allowEvaluation: 1 })
  dialogVisible.value = true
}

function openEdit(s) {
  editId.value = s.id
  Object.assign(form, { semesterName: s.semesterName, startDate: s.startDate || '', endDate: s.endDate || '', allowEvaluation: s.allowEvaluation ?? 1 })
  dialogVisible.value = true
}

async function saveSem() {
  if (!form.semesterName.trim()) { uni.showToast({ title: '请输入学期名称', icon: 'none' }); return }
  saving.value = true
  try {
    const payload = { semesterName: form.semesterName.trim(), startDate: form.startDate || undefined, endDate: form.endDate || undefined, allowEvaluation: form.allowEvaluation }
    if (editId.value) { await adminSemesterApi.update(editId.value, payload) }
    else { await adminSemesterApi.create(payload) }
    uni.showToast({ title: editId.value ? '更新成功' : '创建成功', icon: 'success' })
    dialogVisible.value = false; loadList()
  } finally { saving.value = false }
}

async function setCurrent(s) {
  uni.showModal({ title: '设置当前学期', content: `确认将「${s.semesterName}」设为当前学期？`, success: async ({ confirm }) => {
    if (!confirm) return
    await adminSemesterApi.setCurrent(s.id)
    uni.showToast({ title: '设置成功', icon: 'success' }); loadList()
  }})
}

async function deleteSem(s) {
  uni.showModal({ title: '确认删除', content: `确认删除学期「${s.semesterName}」？`, success: async ({ confirm }) => {
    if (!confirm) return
    await adminSemesterApi.delete(s.id)
    uni.showToast({ title: '删除成功', icon: 'success' }); loadList()
  }})
}

onMounted(loadList)
</script>

<style scoped>
.action-bar { margin-bottom: 24rpx; }
.btn-add { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; border-radius: 12rpx; height: 80rpx; font-size: 28rpx; font-weight: 600; border: none; width: 100%; }

.sem-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; display: flex; gap: 16rpx; align-items: flex-start; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.sc-dot   { width: 16rpx; height: 16rpx; border-radius: 50%; margin-top: 8rpx; flex-shrink: 0; }
.dot-current { background: #11998e; }
.dot-normal  { background: #c0c4cc; }
.sc-body { flex: 1; }
.sc-name-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 10rpx; }
.sc-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.current-badge { background: linear-gradient(135deg, #11998e, #38ef7d); color: #fff; font-size: 20rpx; padding: 3rpx 14rpx; border-radius: 12rpx; }
.sc-meta-row { display: flex; gap: 8rpx; margin-bottom: 8rpx; }
.eval-badge  { font-size: 20rpx; padding: 3rpx 14rpx; border-radius: 4rpx; }
.allow-badge { background: #f0f9eb; color: #67c23a; }
.deny-badge  { background: #fef0f0; color: #f56c6c; }
.sc-time  { font-size: 22rpx; color: #c0c4cc; }
.sc-actions { display: flex; flex-direction: column; gap: 8rpx; align-items: flex-end; }
.action-btn { font-size: 22rpx; padding: 6rpx 14rpx; border-radius: 6rpx; white-space: nowrap; }
.set-btn  { color: #409eff; background: #ecf5ff; }
.edit-btn { color: #e6a23c; background: #fdf6ec; }
.del-btn  { color: #f56c6c; background: #fef0f0; }

.modal-mask   { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box    { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.modal-footer { display: flex; gap: 16rpx; padding: 24rpx 40rpx; border-top: 2rpx solid #f0f0f0; }
.modal-footer button { flex: 1; border-radius: 12rpx; height: 88rpx; font-size: 28rpx; border: none; }
.btn-cancel-m  { background: #f5f7fa; color: #606266; }
.btn-confirm-m { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; }
.btn-confirm-m:disabled { opacity: 0.6; }
.form-row    { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; gap: 16rpx; }
.form-row:last-child { border-bottom: none; }
.form-label  { font-size: 28rpx; color: #606266; width: 160rpx; flex-shrink: 0; }
.form-input  { flex: 1; font-size: 28rpx; color: #303133; text-align: right; }
.picker-val  { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #303133; }
.arrow { color: #c0c4cc; }
</style>

