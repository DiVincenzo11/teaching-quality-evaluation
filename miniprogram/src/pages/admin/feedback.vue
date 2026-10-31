<template>
  <view class="page-bg">
    <nav-bar title="反馈查看" />
    <view class="content">
      <!-- 筛选 -->
      <view class="card filter-bar">
        <text class="fl">学期</text>
        <picker mode="selector" :range="['全部', ...semLabels]" :value="semIndexOffset" @change="onSemChange">
          <view class="picker-btn">
            <text>{{ semIndexOffset === 0 ? '全部' : semLabels[semIndexOffset - 1] }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view v-if="loading" class="loading-box"><text>加载中...</text></view>
      <view v-else-if="!list.length" class="empty-box">
        <text class="empty-icon">💬</text>
        <text>暂无反馈数据</text>
      </view>
      <view v-for="fb in list" :key="fb.id" class="fb-card" @click="viewDetail(fb)">
        <view class="fb-head">
          <view class="avatar-circle">{{ fb.teacherName?.[0] || 'T' }}</view>
          <view class="fb-info">
            <text class="fb-name">{{ fb.teacherName }}</text>
            <text class="fb-meta">{{ fb.semesterName }}</text>
          </view>
          <view class="fb-score-badge" :class="scoreBadgeClass(fb.avgScore)">
            <text class="fbs-val">{{ fb.avgScore != null ? Number(fb.avgScore).toFixed(1) : '--' }}</text>
            <text class="fbs-lbl">均分</text>
          </view>
        </view>
        <view class="fb-preview">
          <text class="fb-text">{{ truncate(fb.reflection) }}</text>
        </view>
        <view class="fb-footer">
          <text class="fb-time">{{ fb.updateTime?.slice(0, 10) }}</text>
          <text class="fb-more">查看详情 ›</text>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="detailVisible" class="modal-mask">
      <view class="modal-overlay" @click="detailVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <view>
            <text class="modal-title">{{ current?.teacherName }}</text>
            <text class="modal-sub">{{ current?.semesterName }}</text>
          </view>
          <text class="modal-close" @click="detailVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 65vh;" v-if="current">
          <view style="padding: 32rpx 40rpx;">
            <view class="dg-row">
              <view class="dg-item">
                <text class="dg-val">{{ current.avgScore != null ? Number(current.avgScore).toFixed(2) : '--' }}</text>
                <text class="dg-lbl">学期均分</text>
              </view>
              <view class="dg-item">
                <text class="dg-val">{{ current.evalCount ?? '--' }}</text>
                <text class="dg-lbl">评价次数</text>
              </view>
            </view>
            <view class="section-block">
              <text class="sb-title">教学反思</text>
              <text class="sb-content">{{ current.reflection || '暂无内容' }}</text>
            </view>
            <view class="section-block" v-if="current.improvementPlan">
              <text class="sb-title">改进计划</text>
              <text class="sb-content">{{ current.improvementPlan }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/admin/feedback" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminFeedbackApi, adminSemesterApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const list         = ref([])
const loading      = ref(false)
const semesters    = ref([])
const semIndexOffset = ref(0)
const detailVisible= ref(false)
const current      = ref(null)

const semLabels = computed(() => semesters.value.map(s => s.semesterName))

function scoreBadgeClass(s) {
  const n = Number(s)
  if (n >= 85) return 'badge-green'
  if (n >= 70) return 'badge-orange'
  if (!s && s !== 0) return 'badge-gray'
  return 'badge-red'
}
function truncate(s) { if (!s) return '暂无内容'; return s.length > 60 ? s.slice(0, 60) + '...' : s }

async function onSemChange(e) {
  semIndexOffset.value = e.detail.value
  await loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = {}
    if (semIndexOffset.value > 0) params.semesterId = semesters.value[semIndexOffset.value - 1]?.id
    const res = await adminFeedbackApi.list(params)
    list.value = res.data || []
  } finally { loading.value = false }
}

function viewDetail(fb) { current.value = fb; detailVisible.value = true }

onMounted(async () => {
  const semRes = await adminSemesterApi.list()
  semesters.value = semRes.data || []
  await loadList()
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content { padding: 24rpx; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; }
.fl { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }
.fb-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06); }
.fb-head { display: flex; align-items: center; gap: 20rpx; margin-bottom: 20rpx; }
.fb-info { flex: 1; }
.fb-name { display: block; font-size: 30rpx; font-weight: 600; color: #303133; margin-bottom: 4rpx; }
.fb-meta { font-size: 22rpx; color: #909399; }
.fb-score-badge { display: flex; flex-direction: column; align-items: center; padding: 12rpx 20rpx; border-radius: 16rpx; flex-shrink: 0; }
.badge-green  { background: #f0f9eb; }
.badge-orange { background: #fdf6ec; }
.badge-red    { background: #fef0f0; }
.badge-gray   { background: #f4f4f5; }
.fbs-val { font-size: 32rpx; font-weight: 700; display: block; }
.badge-green  .fbs-val { color: #67c23a; }
.badge-orange .fbs-val { color: #e6a23c; }
.badge-red    .fbs-val { color: #f56c6c; }
.badge-gray   .fbs-val { color: #909399; }
.fbs-lbl { font-size: 20rpx; color: #909399; }
.fb-preview { background: #f5f7fa; border-radius: 12rpx; padding: 16rpx 20rpx; margin-bottom: 16rpx; }
.fb-text { font-size: 26rpx; color: #303133; line-height: 1.6; }
.fb-footer { display: flex; justify-content: space-between; align-items: center; }
.fb-time { font-size: 22rpx; color: #c0c4cc; }
.fb-more { font-size: 24rpx; color: #409eff; }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box  { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; }
.modal-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-sub    { display: block; font-size: 24rpx; color: #909399; margin-top: 4rpx; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.dg-row { display: flex; gap: 24rpx; margin-bottom: 32rpx; background: #f5f7fa; border-radius: 16rpx; padding: 24rpx; }
.dg-item { flex: 1; text-align: center; }
.dg-val  { display: block; font-size: 40rpx; font-weight: 700; color: #1a2a6c; margin-bottom: 6rpx; }
.dg-lbl  { font-size: 22rpx; color: #909399; }
.section-block { margin-bottom: 28rpx; }
.sb-title { display: block; font-size: 28rpx; font-weight: 600; color: #303133; margin-bottom: 12rpx; padding-bottom: 10rpx; border-bottom: 3rpx solid #1a2a6c; }
.sb-content { font-size: 26rpx; color: #606266; line-height: 1.8; background: #f5f7fa; border-radius: 12rpx; padding: 20rpx; display: block; }
</style>



