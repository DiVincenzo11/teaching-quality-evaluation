<template>
  <view class="container" style="padding-bottom: 40rpx;">
    <!-- 说明 -->
    <view class="tip-card card">
      <text class="tip-icon">💡</text>
      <text class="tip-text">关键词用于本地情感分析（NLP服务不可用时的降级方案），分为正面、中性、负面三类。</text>
    </view>

    <!-- 类型切换 -->
    <view class="type-tabs">
      <view v-for="t in typeOpts" :key="t.value" class="type-tab"
            :class="{ active: activeType === t.value }" @click="changeType(t.value)">
        <text class="tt-dot" :style="`background: ${t.color}`" />
        {{ t.label }}（{{ countOf(t.value) }}）
      </view>
    </view>

    <!-- 新增 -->
    <view class="add-row">
      <view class="add-input-wrap">
        <input v-model="newWord" class="add-input" placeholder="输入关键词，按添加" @confirm="addKeyword" />
      </view>
      <button class="btn-add" @click="addKeyword">添加</button>
    </view>

    <!-- 列表 -->
    <view v-if="loading" class="loading-box"><text>加载中...</text></view>
    <view v-else-if="!filteredList.length" class="empty-box">
      <text class="empty-icon">🏷️</text>
      <text>暂无{{ currentTypeName }}关键词</text>
    </view>
    <view v-else>
      <view class="kw-grid">
        <view v-for="kw in filteredList" :key="kw.id" class="kw-item" :class="kwClass">
          <text class="kw-word">{{ kw.word }}</text>
          <text class="kw-del" @click="deleteKw(kw)">✕</text>
        </view>
      </view>
    </view>

    <!-- 编辑弹窗（批量导入） -->
    <view v-if="importVisible" class="modal-mask">
      <view class="modal-overlay" @click="importVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">批量导入关键词</text>
          <text class="modal-close" @click="importVisible = false">✕</text>
        </view>
        <view style="padding: 24rpx 40rpx;">
          <text class="import-hint">每行一个关键词，将全部导入为「{{ currentTypeName }}」类型</text>
          <textarea v-model="importText" class="import-textarea" placeholder="例：&#13;&#10;优秀&#13;&#10;认真&#13;&#10;用心" />
        </view>
        <view class="modal-footer">
          <button class="btn-cancel-m" @click="importVisible = false">取消</button>
          <button class="btn-confirm-m" :disabled="importing" @click="doImport">
            {{ importing ? '导入中...' : '确认导入' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminKeywordApi } from '@/api/index'

const list    = ref([])
const loading = ref(false)
const activeType = ref(1)
const newWord    = ref('')
const importVisible = ref(false)
const importText    = ref('')
const importing     = ref(false)

const typeOpts = [
  { label: '正面', value: 1, color: '#67c23a' },
  { label: '中性', value: 2, color: '#909399' },
  { label: '负面', value: 3, color: '#f56c6c' },
]

const filteredList = computed(() => list.value.filter(k => k.wordType === activeType.value))
const currentTypeName = computed(() => typeOpts.find(t => t.value === activeType.value)?.label || '')
const kwClass = computed(() => ({ 1: 'kw-pos', 2: 'kw-neu', 3: 'kw-neg' }[activeType.value] || ''))

function countOf(type) { return list.value.filter(k => k.wordType === type).length }

async function loadList() {
  loading.value = true
  try {
    const res = await adminKeywordApi.list({})
    list.value = res.data || []
  } finally { loading.value = false }
}

function changeType(v) { activeType.value = v }

async function addKeyword() {
  const word = newWord.value.trim()
  if (!word) { uni.showToast({ title: '请输入关键词', icon: 'none' }); return }
  try {
    await adminKeywordApi.create({ word, wordType: activeType.value })
    newWord.value = ''
    uni.showToast({ title: '添加成功', icon: 'success' })
    loadList()
  } catch {}
}

async function deleteKw(kw) {
  uni.showModal({ title: '确认删除', content: `确认删除关键词「${kw.word}」？`, success: async ({ confirm }) => {
    if (!confirm) return
    await adminKeywordApi.delete(kw.id)
    uni.showToast({ title: '删除成功', icon: 'success' })
    loadList()
  }})
}

async function doImport() {
  const words = importText.value.split('\n').map(w => w.trim()).filter(Boolean)
  if (!words.length) { uni.showToast({ title: '请输入关键词', icon: 'none' }); return }
  importing.value = true
  try {
    for (const word of words) {
      try { await adminKeywordApi.create({ word, wordType: activeType.value }) } catch {}
    }
    uni.showToast({ title: `已导入 ${words.length} 个关键词`, icon: 'success' })
    importVisible.value = false; importText.value = ''
    loadList()
  } finally { importing.value = false }
}

onMounted(loadList)
</script>

<style scoped>
.tip-card { display: flex; align-items: flex-start; gap: 16rpx; background: #ecf5ff; border: 2rpx solid #b3d8ff; }
.tip-icon { font-size: 36rpx; flex-shrink: 0; }
.tip-text { font-size: 26rpx; color: #409eff; line-height: 1.7; }

.type-tabs { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.type-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 8rpx;
  padding: 18rpx 0; border-radius: 16rpx; font-size: 26rpx;
  color: #606266; background: #fff; border: 2rpx solid #ebeef5;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.type-tab.active { border-color: #1a2a6c; color: #1a2a6c; font-weight: 600; }
.tt-dot { width: 16rpx; height: 16rpx; border-radius: 50%; flex-shrink: 0; }

.add-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.add-input-wrap {
  flex: 1; background: #fff; border-radius: 12rpx;
  padding: 0 24rpx; border: 2rpx solid #ebeef5;
  display: flex; align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.add-input { flex: 1; height: 80rpx; font-size: 28rpx; color: #303133; }
.btn-add {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff; border-radius: 12rpx; height: 80rpx;
  padding: 0 32rpx; font-size: 28rpx; border: none; white-space: nowrap;
}

.kw-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.kw-item {
  display: flex; align-items: center; gap: 10rpx;
  padding: 12rpx 20rpx; border-radius: 12rpx; font-size: 26rpx;
}
.kw-pos  { background: #f0f9eb; border: 2rpx solid #b7e394; color: #67c23a; }
.kw-neu  { background: #f4f4f5; border: 2rpx solid #d3d4d6; color: #606266; }
.kw-neg  { background: #fef0f0; border: 2rpx solid #fab6b6; color: #f56c6c; }
.kw-word { font-weight: 500; }
.kw-del  { font-size: 24rpx; opacity: 0.6; }

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
.import-hint { display: block; font-size: 24rpx; color: #909399; margin-bottom: 16rpx; }
.import-textarea { width: 100%; background: #f5f7fa; border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; color: #303133; min-height: 200rpx; box-sizing: border-box; }
</style>

