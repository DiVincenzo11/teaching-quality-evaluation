<template>
  <view class="page-bg">
    <nav-bar title="评价查看" />
    <view class="content">
      <!-- 学期选择 -->
      <view class="card filter-row">
        <text class="fl">学期</text>
        <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
          <view class="picker-btn">
            <text>{{ semLabels[semIndex] || '全部' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view v-if="loading" class="loading-box"><text>加载中...</text></view>
      <view v-else-if="!statsData" class="empty-box">
        <text class="empty-icon">📊</text>
        <text>该学期暂无评价数据</text>
      </view>
      <template v-else>
        <!-- 总体指标 -->
        <view class="card">
          <view class="card-title">综合指标</view>
          <view class="metrics-grid">
            <view class="metric-item">
              <text class="metric-val green">{{ statsData.avgScore ?? '--' }}</text>
              <text class="metric-lbl">综合均分</text>
            </view>
            <view class="metric-item">
              <text class="metric-val blue">{{ statsData.evalCount ?? '--' }}</text>
              <text class="metric-lbl">评价人次</text>
            </view>
            <view class="metric-item">
              <text class="metric-val orange">{{ statsData.choiceAvgScore ?? '--' }}</text>
              <text class="metric-lbl">单选题均分</text>
            </view>
            <view class="metric-item">
              <text class="metric-val purple">{{ statsData.essayAvgScore ?? '--' }}</text>
              <text class="metric-lbl">简答题均分</text>
            </view>
          </view>
        </view>

        <!-- 各题详情 -->
        <view class="card" v-if="statsData.questionData?.length">
          <view class="card-title">各题平均分</view>
          <view v-for="q in statsData.questionData" :key="q.questionNo" class="q-row">
            <text class="q-no">Q{{ q.questionNo }}</text>
            <view class="q-bar-wrap">
              <text class="q-content">{{ q.questionContent }}</text>
              <view class="q-bar">
                <view class="q-fill"
                      :style="`width: ${q.maxScore > 0 ? (q.avgScore/q.maxScore*100) : 0}%; background: ${barColor(q.avgScore, q.maxScore)}`" />
              </view>
            </view>
            <view class="q-right">
              <text class="q-score" :style="`color: ${barColor(q.avgScore, q.maxScore)}`">{{ q.avgScore != null ? Number(q.avgScore).toFixed(2) : '--' }}</text>
              <text class="q-max">/{{ q.maxScore }}</text>
            </view>
          </view>
        </view>

        <!-- 学生评语 -->
        <view class="card" v-if="statsData.essayList?.length">
          <view class="essay-header">
            <text class="card-title">学生评语（匿名）</text>
            <text class="essay-total">共 {{ statsData.essayList.length }} 条</text>
          </view>
          <view v-for="(e, i) in pagedEssays" :key="essayPage + '_' + i" class="essay-item">
            <text class="essay-avatar">{{ avatarEmoji((essayPage - 1) * essayPageSize + i) }}</text>
            <view class="essay-body">
              <text class="essay-content">{{ e.essayContent }}</text>
              <view v-if="e.sentimentScore != null" class="sentiment-row">
                <view class="sentiment-tag" :class="sentimentClass(e.sentimentLabel)">
                  {{ e.sentimentLabel || '中性' }}
                </view>
                <text class="sentiment-score">情感分 {{ e.sentimentScore }}</text>
              </view>
              <text class="essay-time">{{ e.submitTime?.slice(0, 10) }}</text>
            </view>
          </view>
          <!-- 分页控制 -->
          <view v-if="totalEssayPages > 1" class="pagination">
            <view class="page-btn" :class="{ disabled: essayPage <= 1 }" @click="prevEssayPage">‹ 上一页</view>
            <text class="page-info">{{ essayPage }} / {{ totalEssayPages }}</text>
            <view class="page-btn" :class="{ disabled: essayPage >= totalEssayPages }" @click="nextEssayPage">下一页 ›</view>
          </view>
        </view>
      </template>
    </view>

    <custom-tab-bar current-path="pages/teacher/eval-view" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const semesters  = ref([])
const semIndex   = ref(0)
const statsData  = ref(null)
const loading    = ref(false)

// 评语分页
const essayPage     = ref(1)
const essayPageSize = 5

const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)

const pagedEssays = computed(() => {
  const list = statsData.value?.essayList || []
  const start = (essayPage.value - 1) * essayPageSize
  return list.slice(start, start + essayPageSize)
})
const totalEssayPages = computed(() =>
  Math.ceil((statsData.value?.essayList?.length || 0) / essayPageSize)
)

function prevEssayPage() { if (essayPage.value > 1) essayPage.value-- }
function nextEssayPage() { if (essayPage.value < totalEssayPages.value) essayPage.value++ }

const avatarEmoji = (i) => ['😊','🙂','😄','🤗','😎','🧐'][i % 6]

function barColor(score, max) {
  if (!max) return '#1a2a6c'
  const pct = score / max * 100
  if (pct >= 85) return '#11998e'
  if (pct >= 70) return '#e6a23c'
  return '#f56c6c'
}

function sentimentClass(label) {
  if (label === '正面' || label === 'positive') return 'sent-pos'
  if (label === '负面' || label === 'negative') return 'sent-neg'
  return 'sent-neu'
}

async function onSemChange(e) {
  semIndex.value = e.detail.value
  essayPage.value = 1
  await loadStats()
}

async function loadStats() {
  loading.value = true
  statsData.value = null
  try {
    const res = await teacherApi.evalStats(selectedSem.value)
    if (res.data?.evalCount > 0) statsData.value = res.data
  } finally { loading.value = false }
}

onMounted(async () => {
  try {
    const semRes = await teacherApi.semesters()
    semesters.value = semRes.data || []
    const cur = semesters.value.findIndex(s => s.isCurrent === 1)
    semIndex.value = cur >= 0 ? cur : 0
  } catch {}
  await loadStats()
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content { padding: 24rpx; margin-top: -16rpx; }
.filter-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0; }
.fl { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }

.metrics-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }
.metric-item { background: #f5f7fa; border-radius: 16rpx; padding: 24rpx 16rpx; text-align: center; }
.metric-val  { display: block; font-size: 40rpx; font-weight: 700; margin-bottom: 8rpx; }
.metric-lbl  { display: block; font-size: 22rpx; color: #909399; }
.green  { color: #11998e; }
.blue   { color: #1a2a6c; }
.orange { color: #e6a23c; }
.purple { color: #9c27b0; }

.q-row { display: flex; align-items: flex-start; gap: 12rpx; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.q-row:last-child { border-bottom: none; }
.q-no { font-size: 22rpx; font-weight: 700; color: #1a2a6c; width: 48rpx; flex-shrink: 0; padding-top: 4rpx; }
.q-bar-wrap { flex: 1; min-width: 0; }
.q-content { display: block; font-size: 22rpx; color: #606266; margin-bottom: 8rpx; line-height: 1.6; word-break: break-all; white-space: normal; }
.q-bar { height: 12rpx; background: #f0f0f0; border-radius: 6rpx; overflow: hidden; }
.q-fill { height: 100%; border-radius: 6rpx; }
.q-right { display: flex; flex-direction: column; align-items: flex-end; gap: 2rpx; flex-shrink: 0; padding-top: 4rpx; }
.q-score { font-size: 26rpx; font-weight: 700; }
.q-max   { font-size: 20rpx; color: #909399; }

.essay-item { display: flex; gap: 16rpx; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.essay-item:last-child { border-bottom: none; }
.essay-avatar { font-size: 52rpx; flex-shrink: 0; }
.essay-body   { flex: 1; }
.essay-content { display: block; font-size: 26rpx; color: #303133; line-height: 1.7; margin-bottom: 8rpx; }
.sentiment-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 6rpx; }
.sentiment-tag { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 4rpx; }
.sent-pos { background: #f0f9eb; color: #67c23a; }
.sent-neg { background: #fef0f0; color: #f56c6c; }
.sent-neu { background: #f4f4f5; color: #909399; }
.sentiment-score { font-size: 22rpx; color: #909399; }
.essay-time { font-size: 22rpx; color: #c0c4cc; }

.essay-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.essay-header .card-title { margin-bottom: 0; }
.essay-total  { font-size: 24rpx; color: #909399; }

.pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 32rpx; padding: 24rpx 0 8rpx;
  border-top: 2rpx solid #f5f5f5; margin-top: 8rpx;
}
.page-btn {
  font-size: 26rpx; color: #1a2a6c; padding: 12rpx 28rpx;
  background: #f0f4ff; border-radius: 40rpx;
  border: 2rpx solid #d0daf5;
}
.page-btn.disabled { color: #c0c4cc; background: #f5f5f5; border-color: #e8e8e8; }
.page-info { font-size: 26rpx; color: #606266; }
</style>
