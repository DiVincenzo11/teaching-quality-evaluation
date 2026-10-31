<template>
  <view class="page-bg">
    <view class="content">
      <view class="action-bar">
        <button class="btn-add" @click="openCreate">+ 新增公告</button>
      </view>

      <view class="search-card">
        <view class="search-wrap">
          <text>🔍</text>
          <input v-model="keyword" class="search-input" placeholder="搜索公告标题"
                 placeholder-class="ph" @input="onSearch" />
        </view>
      </view>

      <view v-if="loading" class="loading-box"><text>加载中...</text></view>
      <view v-else-if="!list.length" class="empty-box">
        <text class="empty-icon">📢</text>
        <text>暂无公告</text>
      </view>

      <view v-for="item in list" :key="item.id" class="announce-card">
        <view class="ac-header">
          <view v-if="item.topFlag" class="top-tag">置顶</view>
          <text class="ac-title">{{ item.title }}</text>
          <view class="status-tag" :class="item.status === 1 ? 'tag-pub' : 'tag-draft'">
            {{ item.status === 1 ? '已发布' : '草稿' }}
          </view>
        </view>
        <text class="ac-preview">{{ truncate(item.content) }}</text>
        <view class="ac-footer">
          <text class="ac-time">{{ item.createTime?.slice(0, 10) }}</text>
          <view class="ac-actions">
            <text class="action-btn edit-btn" @click="openEdit(item)">编辑</text>
            <text class="action-btn del-btn" @click="deleteAnn(item)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view v-if="dialogVisible" class="modal-mask">
      <view class="modal-overlay" @click="dialogVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">{{ editId ? '编辑公告' : '新增公告' }}</text>
          <text class="modal-close" @click="dialogVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 55vh;">
          <view style="padding: 20rpx 40rpx;">
            <view class="form-row">
              <text class="form-label">标题</text>
              <input v-model="form.title" class="form-input"
                     placeholder="请输入公告标题" placeholder-class="ph" />
            </view>
            <view class="form-row">
              <text class="form-label">置顶</text>
              <switch :checked="form.topFlag === 1"
                      @change="e => form.topFlag = e.detail.value ? 1 : 0" />
            </view>
            <view class="form-row">
              <text class="form-label">状态</text>
              <picker mode="selector" :range="statusOptions" :value="statusIndex"
                      @change="onStatusChange">
                <view class="picker-val">
                  <text>{{ statusOptions[statusIndex] }}</text>
                  <text class="arrow">›</text>
                </view>
              </picker>
            </view>
            <view class="form-row-block">
              <text class="form-label-block">公告内容</text>
              <textarea v-model="form.content" class="form-textarea"
                        placeholder="请输入公告详细内容..." />
            </view>
          </view>
        </scroll-view>
        <view class="modal-footer">
          <button class="btn-cancel-m" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm-m" :disabled="saving" @click="saveAnn">
            {{ saving ? '保存中...' : '确认保存' }}
          </button>
        </view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/admin/announce" />
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { announcementApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const list         = ref([])
const loading      = ref(false)
const keyword      = ref('')
const page         = ref(1)
const dialogVisible = ref(false)
const editId       = ref(null)
const saving       = ref(false)
const statusIndex  = ref(1)
const statusOptions = ['草稿', '已发布']

// status: 0=草稿, 1=已发布 (整数)
const form = reactive({ title: '', content: '', topFlag: 0, status: 1 })

function truncate(s) {
  if (!s) return ''
  return s.length > 60 ? s.slice(0, 60) + '...' : s
}

async function loadList() {
  loading.value = true
  try {
    const res = await announcementApi.adminList(page.value, 20, keyword.value || undefined)
    list.value = res.data?.records || []
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onSearch() { page.value = 1; loadList() }

function openCreate() {
  editId.value = null
  statusIndex.value = 1
  Object.assign(form, { title: '', content: '', topFlag: 0, status: 1 })
  dialogVisible.value = true
}

function openEdit(item) {
  editId.value = item.id
  statusIndex.value = item.status === 1 ? 1 : 0
  Object.assign(form, {
    title:   item.title   || '',
    content: item.content || '',
    topFlag: item.topFlag ? 1 : 0,
    status:  item.status === 1 ? 1 : 0,
  })
  dialogVisible.value = true
}

function onStatusChange(e) {
  statusIndex.value = Number(e.detail.value)
  form.status = statusIndex.value === 1 ? 1 : 0
}

async function saveAnn() {
  if (!form.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload = {
      title:   form.title.trim(),
      content: form.content.trim(),
      topFlag: form.topFlag,
      status:  form.status,
    }
    if (editId.value) {
      await announcementApi.update(editId.value, payload)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await announcementApi.create(payload)
      uni.showToast({ title: '发布成功', icon: 'success' })
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    uni.showToast({ title: String(e?.message || '保存失败，请重试'), icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function deleteAnn(item) {
  uni.showModal({
    title: '确认删除',
    content: `确认删除公告「${item.title}」？`,
    success: async ({ confirm }) => {
      if (!confirm) return
      try {
        await announcementApi.delete(item.id)
        list.value = list.value.filter(a => a.id !== item.id)
        uni.showToast({ title: '删除成功', icon: 'success' })
        await loadList()
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    },
  })
}

onMounted(loadList)
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content  { padding: 24rpx; }
.action-bar { margin-bottom: 16rpx; }
.btn-add { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; border-radius: 12rpx; height: 80rpx; font-size: 28rpx; font-weight: 600; border: none; width: 100%; }

.search-card { background: #fff; border-radius: 16rpx; padding: 16rpx 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.search-wrap { display: flex; align-items: center; gap: 12rpx; background: #f5f7fa; border-radius: 40rpx; padding: 14rpx 20rpx; }
.search-input { flex: 1; font-size: 26rpx; color: #303133; background: transparent; }
.ph { color: #c0c4cc; }

.announce-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.ac-header { display: flex; align-items: flex-start; gap: 10rpx; margin-bottom: 12rpx; }
.top-tag { background: #fdf6ec; color: #e6a23c; font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 4rpx; white-space: nowrap; flex-shrink: 0; }
.ac-title { flex: 1; font-size: 30rpx; font-weight: 600; color: #303133; line-height: 1.5; }
.status-tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 4rpx; white-space: nowrap; flex-shrink: 0; }
.tag-pub   { background: #f0f9eb; color: #67c23a; }
.tag-draft { background: #f4f4f5; color: #909399; }
.ac-preview { display: block; font-size: 26rpx; color: #909399; line-height: 1.6; margin-bottom: 16rpx; }
.ac-footer  { display: flex; justify-content: space-between; align-items: center; }
.ac-time    { font-size: 22rpx; color: #c0c4cc; }
.ac-actions { display: flex; gap: 12rpx; }
.action-btn { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 8rpx; }
.edit-btn { color: #409eff; background: #ecf5ff; }
.del-btn  { color: #f56c6c; background: #fef0f0; }

.loading-box { display: flex; justify-content: center; padding: 60rpx; color: #909399; font-size: 28rpx; }
.empty-box { display: flex; flex-direction: column; align-items: center; padding: 80rpx 40rpx; gap: 16rpx; }
.empty-icon { font-size: 80rpx; }
.empty-box text:last-child { font-size: 28rpx; color: #909399; }

/* 弹窗 */
.modal-mask    { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box     { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; max-height: 85vh; display: flex; flex-direction: column; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; flex-shrink: 0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; padding: 8rpx; }
.modal-footer { display: flex; gap: 16rpx; padding: 24rpx 40rpx; border-top: 2rpx solid #f0f0f0; flex-shrink: 0; }
.modal-footer button { flex: 1; border-radius: 12rpx; height: 88rpx; font-size: 28rpx; border: none; }
.btn-cancel-m  { background: #f5f7fa; color: #606266; }
.btn-confirm-m { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; }
.btn-confirm-m[disabled] { opacity: 0.6; }

/* 表单 */
.form-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; gap: 16rpx; }
.form-label  { font-size: 28rpx; color: #606266; width: 120rpx; flex-shrink: 0; }
.form-input  { flex: 1; font-size: 28rpx; color: #303133; text-align: right; min-height: 40rpx; }
.picker-val  { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #303133; }
.arrow { color: #c0c4cc; }
.form-row-block { padding: 20rpx 0; }
.form-label-block { display: block; font-size: 28rpx; color: #606266; margin-bottom: 12rpx; }
.form-textarea { width: 100%; background: #f5f7fa; border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; color: #303133; min-height: 200rpx; box-sizing: border-box; line-height: 1.7; }
</style>

