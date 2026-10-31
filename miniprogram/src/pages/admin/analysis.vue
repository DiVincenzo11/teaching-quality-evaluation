<template>
  <view class="page-bg">
    <nav-bar title="数据分析" />
    <view class="content">
      <view class="card filter-row">
        <text class="fl">学期</text>
        <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
          <view class="picker-btn">
            <text>{{ semLabels[semIndex] || '全部' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="tab-row">
        <view v-for="t in tabs" :key="t.key"
              class="tab-btn" :class="{ active: activeTab === t.key }"
              @click="switchTab(t.key)">
          {{ t.label }}
        </view>
      </view>

      <!-- 教师统计 -->
      <template v-if="activeTab === 'stats'">
        <view v-if="statsLoading" class="loading-box"><text>加载中...</text></view>
        <view v-else>
          <view class="card">
            <view class="card-title">评分分布概览</view>
            <view class="score-overview">
              <view class="so-item" v-for="item in scoreRanges" :key="item.label">
                <view class="so-bar-wrap">
                  <view class="so-bar" :style="`width: ${item.pct}%; background: ${item.color}`" />
                </view>
                <view class="so-meta">
                  <text class="so-label">{{ item.label }}</text>
                  <text class="so-count" :style="`color: ${item.color}`">{{ item.count }}</text>
                </view>
              </view>
            </view>
          </view>
          <view class="card">
            <view class="card-title">教师评价统计（{{ teacherStats.length }}人）</view>
            <view v-for="(t, i) in teacherStats" :key="t.userId" class="ts-row" @click="showTeacherDetail(t)">
              <text class="ts-rank">#{{ i + 1 }}</text>
              <view class="ts-avatar">{{ t.realName?.[0] || 'T' }}</view>
              <view class="ts-body">
                <text class="ts-name">{{ t.realName }}</text>
                <text class="ts-dept">{{ t.department }}</text>
              </view>
              <view class="ts-right">
                <view class="ts-score-tag" :class="scoreTagClass(t.avgScore)">{{ t.avgScore ?? '-' }}</view>
                <text class="ts-count">{{ t.evalCount }}次</text>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- 动态权重 -->
      <template v-if="activeTab === 'weights'">
        <view class="recalc-bar">
          <button class="btn-recalc" :disabled="recalcLoading" @click="recalculate">
            {{ recalcLoading ? '重算中...' : '重新计算权重' }}
          </button>
        </view>
        <view v-if="weightsLoading" class="loading-box"><text>加载中...</text></view>
        <view v-else-if="!weights.length" class="empty-box"><text>暂无权重数据</text></view>
        <view v-else class="card">
          <view class="card-title">动态权重分布（熵权法）</view>
          <view class="weight-total">
            各题权重之和：{{ weightSum }}%
          </view>
          <view v-for="w in weights" :key="w.questionNo" class="weight-row">
            <view class="wr-left">
              <text class="wr-no">Q{{ w.questionNo }}</text>
              <text class="wr-content">{{ w.questionContent }}</text>
              <text class="wr-entropy">熵值 {{ Number(w.entropy).toFixed(3) }}</text>
            </view>
            <view class="wr-right">
              <view class="weight-bar">
                <view class="weight-fill"
                      :style="`width: ${Math.min(isNaN(Number(w.adjustedWeight)) ? 0 : Number(w.adjustedWeight), 100)}%; background: ${weightColor(w.adjustedWeight)}`" />
              </view>
              <view class="weight-nums">
                <text class="weight-val" :style="`color: ${weightColor(w.adjustedWeight)}`">
                  {{ (isNaN(Number(w.adjustedWeight)) ? 0 : Number(w.adjustedWeight)).toFixed(1) }}%
                </text>
                <text class="weight-raw" v-if="w.rawWeight != null && Math.abs(Number(w.rawWeight) - Number(w.adjustedWeight)) > 0.01">
                  原 {{ (isNaN(Number(w.rawWeight)) ? 0 : Number(w.rawWeight)).toFixed(1) }}%
                </text>
              </view>
            </view>
          </view>
        </view>
      </template>

      <!-- 聚类分析 -->
      <template v-if="activeTab === 'cluster'">
        <view v-if="clusterLoading" class="loading-box"><text>加载中...</text></view>
        <view v-else-if="!clusterData || !clusterData.groups?.length" class="empty-box">
          <text class="empty-icon">🔬</text>
          <text>{{ clusterData?.message || '暂无聚类数据（至少需要3位教师的评价数据）' }}</text>
        </view>
        <view v-else>
          <view v-for="(group, gi) in clusterData.groups" :key="gi" class="cluster-card card">
            <view class="cluster-header" :style="`background: ${group.color || clusterColors[gi % 3]}`">
              <text class="cluster-title">{{ group.label }}</text>
              <text class="cluster-count">{{ group.count || group.teachers?.length || 0 }}人</text>
            </view>
            <view class="cluster-desc">{{ group.description }}</view>
            <view class="cluster-teachers">
              <view v-for="t in (group.teachers || []).slice(0, 6)" :key="t.teacherId" class="cluster-teacher">
                {{ t.teacherName }}
                <text v-if="t.avgScore" class="ct-score">{{ Number(t.avgScore).toFixed(1) }}</text>
              </view>
              <text v-if="(group.teachers?.length || 0) > 6" class="cluster-more">
                +{{ group.teachers.length - 6 }}人
              </text>
            </view>
          </view>
        </view>
      </template>

      <!-- 参与度分析 -->
      <template v-if="activeTab === 'engagement'">
        <view v-if="engLoading" class="loading-box"><text>加载中...</text></view>
        <view v-else-if="!engData" class="empty-box">
          <text class="empty-icon">📊</text>
          <text>暂无参与度数据</text>
        </view>
        <view v-else>
          <!-- 概览 -->
          <view class="card">
            <view class="card-title">参与度概览</view>
            <view class="eng-grid">
              <view class="eng-item">
                <text class="eng-val blue">{{ engData.total ?? 0 }}</text>
                <text class="eng-lbl">评价总数</text>
              </view>
              <view class="eng-item">
                <text class="eng-val orange">{{ engData.excluded ?? 0 }}</text>
                <text class="eng-lbl">已剔除异常</text>
              </view>
              <view class="eng-item">
                <text class="eng-val red">{{ engData.lowAttendance ?? 0 }}</text>
                <text class="eng-lbl">低出勤</text>
              </view>
              <view class="eng-item">
                <text class="eng-val purple">{{ engData.lowStudy ?? 0 }}</text>
                <text class="eng-lbl">低投入</text>
              </view>
            </view>
          </view>
          <!-- 出勤率分布 -->
          <view class="card">
            <view class="card-title">出勤率分布</view>
            <view v-if="!attendanceDist.some(i => i.count > 0)" class="empty-box"><text>暂无数据</text></view>
            <template v-else>
              <view v-for="item in attendanceDist" :key="item.label" class="dist-row">
                <text class="dist-label">{{ item.label }}</text>
                <view class="dist-bar">
                  <view class="dist-fill" :style="`width: ${item.pct}%; background: #1a2a6c`" />
                </view>
                <text class="dist-count">{{ item.count }}</text>
                <text class="dist-pct">{{ item.pct }}%</text>
              </view>
            </template>
          </view>
          <!-- 课外投入分布 -->
          <view class="card">
            <view class="card-title">课外投入分布</view>
            <view v-if="!studyDist.some(i => i.count > 0)" class="empty-box"><text>暂无数据</text></view>
            <template v-else>
              <view v-for="item in studyDist" :key="item.label" class="dist-row">
                <text class="dist-label">{{ item.label }}</text>
                <view class="dist-bar">
                  <view class="dist-fill" :style="`width: ${item.pct}%; background: #11998e`" />
                </view>
                <text class="dist-count">{{ item.count }}</text>
                <text class="dist-pct">{{ item.pct }}%</text>
              </view>
            </template>
          </view>
        </view>
      </template>
    </view>

    <!-- 教师详情弹窗 -->
    <view v-if="teacherDetailVisible" class="modal-mask">
      <view class="modal-overlay" @click="teacherDetailVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">{{ currentTeacher?.realName }} — 评价详情</text>
          <text class="modal-close" @click="teacherDetailVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 65vh;" v-if="currentTeacher">
          <view style="padding: 32rpx 40rpx;">
            <view class="dg-row">
              <view class="dg-item">
                <text class="dg-val">{{ currentTeacher.avgScore ?? '--' }}</text>
                <text class="dg-lbl">均分</text>
              </view>
              <view class="dg-item">
                <text class="dg-val">{{ currentTeacher.evalCount ?? '--' }}</text>
                <text class="dg-lbl">评价次数</text>
              </view>
            </view>
            <view class="detail-row"><text class="dl">院系</text><text class="dv">{{ currentTeacher.department }}</text></view>
            <view class="detail-row"><text class="dl">所教科目</text><text class="dv">{{ currentTeacher.courses?.join('、') }}</text></view>
            <view class="detail-row"><text class="dl">所教班级</text><text class="dv">{{ currentTeacher.classes }}</text></view>
          </view>
        </scroll-view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/admin/analysis" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAnalysisApi, adminSemesterApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const semesters    = ref([])
const semIndex     = ref(0)
const activeTab    = ref('stats')
const teacherStats = ref([])
const statsLoading = ref(false)
const weights      = ref([])
const weightsLoading  = ref(false)
const recalcLoading   = ref(false)
const clusterData  = ref(null)
const clusterLoading  = ref(false)
const engData      = ref(null)
const engLoading   = ref(false)
const teacherDetailVisible = ref(false)
const currentTeacher = ref(null)

const clusterColors = ['#1a2a6c', '#11998e', '#e6a23c']
const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)

const tabs = [
  { key: 'stats',      label: '统计' },
  { key: 'weights',    label: '权重' },
  { key: 'cluster',    label: '聚类' },
  { key: 'engagement', label: '参与度' },
]

const scoreRanges = computed(() => {
  const stats = teacherStats.value
  if (!stats.length) return []
  const ranges = [
    { label: '90-100', min: 90, max: 100, color: '#11998e' },
    { label: '80-90',  min: 80, max: 90,  color: '#67c23a' },
    { label: '70-80',  min: 70, max: 80,  color: '#e6a23c' },
    { label: '<70',    min: 0,  max: 70,  color: '#f56c6c' },
  ]
  const maxCount = stats.length
  return ranges.map(r => {
    const count = stats.filter(t => {
      const s = Number(t.avgScore)
      return s >= r.min && (r.max === 100 ? s <= 100 : s < r.max)
    }).length
    return { ...r, count, pct: maxCount ? Math.round((count / maxCount) * 100) : 0 }
  })
})

const attendanceDist = computed(() => {
  const d = engData.value?.attendanceDist || {}
  const labels = ['低于50%', '50-70%', '70-85%', '85-95%', '95%以上']
  const total = [1,2,3,4,5].reduce((a, k) => a + Number(d[k] || 0), 0) || 1
  return [1,2,3,4,5].map((k, i) => {
    const count = Number(d[k] || 0)
    return { label: labels[i], count, pct: Math.round(count / total * 100) }
  })
})

const studyDist = computed(() => {
  const d = engData.value?.studyDist || {}
  const labels = ['几乎不投入', '不足1小时', '1-3小时', '3-5小时', '5小时以上']
  const total = [1,2,3,4,5].reduce((a, k) => a + Number(d[k] || 0), 0) || 1
  return [1,2,3,4,5].map((k, i) => {
    const count = Number(d[k] || 0)
    return { label: labels[i], count, pct: Math.round(count / total * 100) }
  })
})

function scoreTagClass(s) {
  const n = Number(s)
  if (n >= 85) return 'tag-success'
  if (n >= 70) return 'tag-warning'
  return 'tag-danger'
}
// adjustedWeight 是百分比值（如 8.33 表示 8.33%）
function weightColor(w) {
  if (w >= 10) return '#1a2a6c'
  if (w >= 7)  return '#2d6a9f'
  return '#8c9ab0'
}

const weightSum = computed(() => {
  const sum = weights.value.reduce((acc, w) => acc + Number(w.adjustedWeight || 0), 0)
  return sum.toFixed(1)
})

async function onSemChange(e) {
  semIndex.value = e.detail.value
  await loadTab(activeTab.value)
}

async function switchTab(key) {
  activeTab.value = key
  await loadTab(key)
}

async function loadTab(key) {
  if (key === 'stats') {
    statsLoading.value = true
    try {
      const res = await adminAnalysisApi.teacherStats(selectedSem.value)
      teacherStats.value = res.data || []
    } finally { statsLoading.value = false }
  } else if (key === 'weights') {
    weightsLoading.value = true
    try {
      const res = await adminAnalysisApi.dynamicWeights(selectedSem.value)
      weights.value = res.data || []
    } finally { weightsLoading.value = false }
  } else if (key === 'cluster') {
    clusterLoading.value = true
    try {
      const res = await adminAnalysisApi.teacherClusters(selectedSem.value)
      const raw = res.data || {}
      if (raw.clusterSummary?.length) {
        // 将 clusterSummary + teachers 组合成 groups 结构
        clusterData.value = {
          message: raw.message || '',
          groups: raw.clusterSummary.map(s => ({
            label:       s.label,
            description: s.description,
            color:       s.color,
            count:       s.count,
            teachers:    (raw.teachers || [])
              .filter(t => t.clusterLabel === s.label)
              .map(t => ({ teacherId: t.teacherId, teacherName: t.realName, avgScore: t.avgScore }))
          }))
        }
      } else {
        clusterData.value = { groups: [], message: raw.message || '数据量不足，无法进行聚类分析' }
      }
    } finally { clusterLoading.value = false }
  } else if (key === 'engagement') {
    engLoading.value = true
    try {
      const res = await adminAnalysisApi.engagementStats(selectedSem.value)
      const list = res.data || []
      engData.value = list.find(d => d.type === 'overview') || list[0] || null
    } finally { engLoading.value = false }
  }
}

async function recalculate() {
  recalcLoading.value = true
  try {
    await adminAnalysisApi.recalculateWeights(selectedSem.value)
    uni.showToast({ title: '重算完成', icon: 'success' })
    await loadTab('weights')
  } finally { recalcLoading.value = false }
}

function showTeacherDetail(t) {
  currentTeacher.value = t; teacherDetailVisible.value = true
}

onMounted(async () => {
  const semRes = await adminSemesterApi.list()
  semesters.value = semRes.data || []
  const cur = semesters.value.findIndex(s => s.isCurrent === 1)
  if (cur >= 0) semIndex.value = cur
  await loadTab('stats')
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content { padding: 24rpx; margin-top: -16rpx; }
.filter-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0; }
.fl { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; }
.arrow { color: #c0c4cc; }
.tab-row { display: flex; gap: 12rpx; margin-bottom: 24rpx; overflow-x: auto; }
.tab-btn { padding: 16rpx 32rpx; border-radius: 20rpx; font-size: 26rpx; color: #606266; background: #fff; border: 2rpx solid #ebeef5; white-space: nowrap; flex-shrink: 0; }
.tab-btn.active { background: #1a2a6c; color: #fff; border-color: #1a2a6c; }
.recalc-bar { margin-bottom: 16rpx; }
.btn-recalc {
  background: linear-gradient(135deg, #e6a23c, #f5a623); color: #fff;
  border-radius: 12rpx; height: 80rpx; font-size: 28rpx; border: none; width: 100%;
}
.btn-recalc:disabled { opacity: 0.6; }
.score-overview { display: flex; flex-direction: column; gap: 16rpx; }
.so-item { display: flex; align-items: center; gap: 12rpx; }
.so-bar-wrap { flex: 1; height: 20rpx; background: #f0f0f0; border-radius: 10rpx; overflow: hidden; }
.so-bar { height: 100%; border-radius: 10rpx; }
.so-meta { width: 120rpx; display: flex; justify-content: space-between; }
.so-label { font-size: 22rpx; color: #606266; }
.so-count { font-size: 22rpx; font-weight: 600; }
.ts-row { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.ts-row:last-child { border-bottom: none; }
.ts-rank { font-size: 24rpx; color: #c0c4cc; width: 40rpx; }
.ts-avatar { width: 64rpx; height: 64rpx; border-radius: 50%; background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; font-size: 28rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ts-body  { flex: 1; overflow: hidden; }
.ts-name  { display: block; font-size: 28rpx; font-weight: 600; color: #303133; }
.ts-dept  { display: block; font-size: 22rpx; color: #909399; }
.ts-right { display: flex; flex-direction: column; align-items: center; gap: 4rpx; }
.ts-score-tag { padding: 6rpx 16rpx; border-radius: 8rpx; font-size: 26rpx; font-weight: 700; }
.tag-success { background: #f0f9eb; color: #67c23a; }
.tag-warning { background: #fdf6ec; color: #e6a23c; }
.tag-danger  { background: #fef0f0; color: #f56c6c; }
.ts-count { font-size: 20rpx; color: #c0c4cc; }
.weight-total { font-size: 22rpx; color: #909399; margin-bottom: 16rpx; text-align: right; }
.weight-row { display: flex; align-items: center; gap: 16rpx; padding: 18rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.weight-row:last-child { border-bottom: none; }
.wr-left  { width: 150rpx; flex-shrink: 0; }
.wr-no    { font-size: 24rpx; color: #1a2a6c; font-weight: 700; }
.wr-content { display: block; font-size: 20rpx; color: #606266; margin-top: 4rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wr-entropy { display: block; font-size: 18rpx; color: #c0c4cc; margin-top: 2rpx; }
.wr-right { flex: 1; display: flex; flex-direction: column; gap: 6rpx; }
.weight-bar { width: 100%; height: 16rpx; background: #f0f0f0; border-radius: 8rpx; overflow: hidden; }
.weight-fill { height: 100%; border-radius: 8rpx; transition: width 0.3s; }
.weight-nums { display: flex; align-items: center; gap: 12rpx; }
.weight-val  { font-size: 26rpx; font-weight: 700; white-space: nowrap; }
.weight-raw  { font-size: 20rpx; color: #c0c4cc; white-space: nowrap; }
.cluster-card { padding: 0; overflow: hidden; }
.cluster-header { padding: 20rpx 28rpx; display: flex; justify-content: space-between; align-items: center; }
.cluster-title { font-size: 28rpx; font-weight: 700; color: #fff; }
.cluster-count { font-size: 24rpx; color: rgba(255,255,255,0.8); }
.cluster-desc  { font-size: 24rpx; color: #606266; padding: 16rpx 28rpx; line-height: 1.7; }
.cluster-teachers { display: flex; flex-wrap: wrap; gap: 12rpx; padding: 0 28rpx 20rpx; }
.cluster-teacher { background: #f5f7fa; color: #303133; font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 6rpx; }
.cluster-more    { font-size: 22rpx; color: #909399; padding: 6rpx 0; align-self: center; }
.cluster-teacher { display: flex; align-items: center; gap: 8rpx; }
.ct-score { font-size: 18rpx; color: #909399; }
.eng-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }
.eng-item { background: #f5f7fa; border-radius: 16rpx; padding: 24rpx 12rpx; text-align: center; }
.eng-val  { display: block; font-size: 40rpx; font-weight: 700; margin-bottom: 6rpx; }
.eng-lbl  { display: block; font-size: 22rpx; color: #909399; }
.red    { color: #f56c6c; }
.dist-row { display: flex; align-items: center; gap: 12rpx; padding: 14rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.dist-row:last-child { border-bottom: none; }
.dist-label { width: 140rpx; font-size: 22rpx; color: #606266; flex-shrink: 0; }
.dist-bar   { flex: 1; height: 16rpx; background: #f0f0f0; border-radius: 8rpx; overflow: hidden; }
.dist-fill  { height: 100%; border-radius: 8rpx; }
.dist-count { font-size: 22rpx; color: #303133; width: 50rpx; text-align: right; }
.dist-pct   { font-size: 22rpx; color: #909399; width: 70rpx; text-align: right; }
.modal-mask   { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box    { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.dg-row   { display: flex; gap: 24rpx; margin-bottom: 32rpx; background: #f5f7fa; border-radius: 16rpx; padding: 24rpx; }
.dg-item  { flex: 1; text-align: center; }
.dg-val   { display: block; font-size: 40rpx; font-weight: 700; color: #1a2a6c; margin-bottom: 6rpx; }
.dg-lbl   { font-size: 22rpx; color: #909399; }
.detail-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.dl { font-size: 26rpx; color: #909399; }
.dv { font-size: 26rpx; color: #303133; }
</style>



