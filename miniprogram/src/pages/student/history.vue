<template>
  <view class="page-bg">
    <nav-bar title="评价历史" />
    <view class="content">
      <view class="stats-row">
        <view class="stat-box">
          <text class="sn">{{ allRecords.length }}</text>
          <text class="sl">累计评价</text>
        </view>
        <view class="divider-v"/>
        <view class="stat-box">
          <text class="sn">{{ avgScore }}</text>
          <text class="sl">平均打分</text>
        </view>
        <view class="divider-v"/>
        <view class="stat-box">
          <text class="sn">{{ semCount }}</text>
          <text class="sl">已评学期</text>
        </view>
      </view>

      <view class="sort-row">
        <text class="sort-label">排序：</text>
        <view v-for="s in sortOpts" :key="s.value" class="sort-btn"
              :class="{ active: sortBy === s.value }" @click="sortBy = s.value">
          {{ s.label }}
        </view>
      </view>

      <view v-if="loading" class="loading-box"><text>加载中...</text></view>
      <view v-else-if="!sortedRecords.length" class="empty-box">
        <text class="empty-icon">📋</text>
        <text>暂无评价记录</text>
      </view>
      <view v-for="r in sortedRecords" :key="r.id" class="record-card" @click="viewDetail(r)">
        <view class="rc-left">
          <view class="avatar-circle">{{ r.teacherName?.[0] || 'T' }}</view>
        </view>
        <view class="rc-body">
          <text class="rc-name">{{ r.teacherName }}</text>
          <text class="rc-dept">{{ r.department }}</text>
          <text class="rc-course">{{ r.courses }}</text>
          <view class="rc-meta">
            <view class="tag tag-info">{{ r.semesterName }}</view>
          </view>
        </view>
        <view class="rc-right">
          <text class="score-big" :class="scoreClass(r.totalScore)">
            {{ r.totalScore != null ? Number(r.totalScore).toFixed(2) : '—' }}
          </text>
          <text class="score-lbl">总分</text>
          <text class="rc-time">{{ r.submitTime?.slice(0, 10) }}</text>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="detailVisible" class="modal-mask">
      <view class="modal-overlay" @click="detailVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">评价记录 — {{ currentRecord?.teacherName }}</text>
          <text class="modal-close" @click="detailVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 60vh;" v-if="currentRecord">
          <view style="padding: 32rpx 40rpx;">
            <view class="detail-row"><text class="dl">学期</text><text class="dv">{{ currentRecord.semesterName }}</text></view>
            <view class="detail-row"><text class="dl">总分</text><view class="tag tag-success">{{ currentRecord.totalScore }}</view></view>
            <view class="detail-row"><text class="dl">单选题分</text><text class="dv">{{ currentRecord.choiceScore }}</text></view>
            <view class="detail-row"><text class="dl">简答题分</text><text class="dv">{{ currentRecord.essayScore }}</text></view>
            <view class="section-title">各题作答</view>
            <view v-for="a in currentRecord.answers" :key="a.questionNo" class="answer-row">
              <text class="ar-q">{{ a.questionNo }}. {{ a.questionContent }}</text>
              <view class="ar-bottom">
                <view class="tag tag-info">{{ a.answerLabel }}</view>
                <text class="ar-score">{{ a.score }} 分</text>
              </view>
            </view>
            <template v-if="currentRecord.essayContent">
              <view class="section-title">我的评语</view>
              <view class="essay-box">{{ currentRecord.essayContent }}</view>
            </template>
          </view>
        </scroll-view>
        <view v-else class="loading-box"><text>加载中...</text></view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/student/history" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { studentApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const allRecords    = ref([])
const loading       = ref(false)
const sortBy        = ref('time')
const detailVisible = ref(false)
const currentRecord = ref(null)

const sortOpts = [
  { label: '时间',  value: 'time' },
  { label: '分数↓', value: 'score_desc' },
  { label: '分数↑', value: 'score_asc' },
]

const avgScore = computed(() => {
  if (!allRecords.value.length) return '—'
  const scores = allRecords.value.filter(r => r.totalScore != null).map(r => Number(r.totalScore))
  if (!scores.length) return '—'
  return (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(2)
})

const semCount = computed(() => new Set(allRecords.value.map(r => r.semesterId)).size)

const sortedRecords = computed(() => {
  const list = [...allRecords.value]
  if (sortBy.value === 'score_desc') return list.sort((a, b) => (Number(b.totalScore) || 0) - (Number(a.totalScore) || 0))
  if (sortBy.value === 'score_asc')  return list.sort((a, b) => (Number(a.totalScore) || 0) - (Number(b.totalScore) || 0))
  return list
})

function scoreClass(s) {
  const n = Number(s)
  if (n >= 85) return 'score-green'
  if (n >= 70) return 'score-orange'
  return 'score-red'
}

async function viewDetail(r) {
  detailVisible.value = true; currentRecord.value = null
  try {
    const res = await studentApi.evalRecordDetail(r.teacherId, r.semesterId)
    currentRecord.value = res.data
  } catch {}
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await studentApi.historyAll()
    allRecords.value = res.data || []
  } finally { loading.value = false }
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content { padding: 24rpx; margin-top: -16rpx; }

.stats-row {
  background: #fff; border-radius: 20rpx; padding: 32rpx;
  display: flex; align-items: center; margin-bottom: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
}
.stat-box  { flex: 1; text-align: center; }
.sn { display: block; font-size: 40rpx; font-weight: 700; color: #1a2a6c; }
.sl { display: block; font-size: 22rpx; color: #909399; margin-top: 6rpx; }
.divider-v { width: 2rpx; height: 60rpx; background: #f0f0f0; }

.sort-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 20rpx; }
.sort-label { font-size: 26rpx; color: #909399; }
.sort-btn {
  padding: 10rpx 24rpx; border-radius: 20rpx; font-size: 24rpx;
  color: #606266; background: #fff; border: 2rpx solid #ebeef5;
}
.sort-btn.active { background: #1a2a6c; color: #fff; border-color: #1a2a6c; }

.record-card {
  background: #fff; border-radius: 20rpx; padding: 28rpx;
  margin-bottom: 16rpx; display: flex; gap: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
}
.rc-body   { flex: 1; overflow: hidden; }
.rc-name   { display: block; font-size: 30rpx; font-weight: 600; color: #303133; margin-bottom: 6rpx; }
.rc-dept   { display: block; font-size: 24rpx; color: #909399; }
.rc-course { display: block; font-size: 22rpx; color: #c0c4cc; margin-bottom: 8rpx; }
.rc-meta   { display: flex; gap: 8rpx; }
.rc-right  { display: flex; flex-direction: column; align-items: center; gap: 6rpx; flex-shrink: 0; }
.score-big   { font-size: 36rpx; font-weight: 700; line-height: 1; }
.score-green  { color: #67c23a; }
.score-orange { color: #e6a23c; }
.score-red    { color: #f56c6c; }
.score-lbl { font-size: 20rpx; color: #909399; }
.rc-time   { font-size: 20rpx; color: #c0c4cc; }

.modal-mask  { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box   { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.detail-row   { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.dl { font-size: 26rpx; color: #909399; }
.dv { font-size: 26rpx; color: #303133; font-weight: 500; }
.section-title { font-size: 28rpx; font-weight: 600; color: #303133; margin: 24rpx 0 16rpx; }
.answer-row  { padding: 16rpx 0; border-bottom: 2rpx dashed #f0f0f0; }
.ar-q        { display: block; font-size: 24rpx; color: #606266; margin-bottom: 10rpx; }
.ar-bottom   { display: flex; align-items: center; gap: 16rpx; }
.ar-score    { font-size: 24rpx; color: #67c23a; font-weight: 600; }
.essay-box   { background: #f5f7fa; border-radius: 12rpx; padding: 20rpx; font-size: 26rpx; color: #303133; line-height: 1.8; }
</style>



