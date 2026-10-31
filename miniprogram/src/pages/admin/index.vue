<template>
  <view class="page-bg">
    <nav-bar title="系统概览" :subtitle="currentSemester?.semesterName || '暂无当前学期'">
      <template #right>
        <view class="manage-btns">
          <view class="manage-btn" @click="goTo('/pages/admin/semesters')">学期</view>
          <view class="manage-btn" @click="goTo('/pages/admin/announce')">公告</view>
          <view class="manage-btn" @click="goTo('/pages/admin/questions')">题目</view>
        </view>
      </template>
    </nav-bar>

    <view class="content">
      <view class="stats-grid">
        <view v-for="card in statCards" :key="card.label" class="stat-card"
              :style="`border-top: 6rpx solid ${card.color}`">
          <text class="sc-icon">{{ card.icon }}</text>
          <text class="sc-val" :style="`color: ${card.color}`">{{ card.value ?? '-' }}</text>
          <text class="sc-lbl">{{ card.label }}</text>
        </view>
      </view>

      <!-- 快捷管理入口 -->
      <view class="card">
        <view class="card-title">管理功能</view>
        <view class="quick-grid">
          <view class="quick-item" @click="goTo('/pages/admin/users')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#1a2a6c,#2d6a9f)">👥</view>
            <text class="quick-text">用户管理</text>
          </view>
          <view class="quick-item" @click="goTo('/pages/admin/questions')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#11998e,#38ef7d)">📝</view>
            <text class="quick-text">题目管理</text>
          </view>
          <view class="quick-item" @click="goTo('/pages/admin/keywords')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#f093fb,#f5576c)">🏷️</view>
            <text class="quick-text">关键词</text>
          </view>
          <view class="quick-item" @click="goTo('/pages/admin/semesters')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#4facfe,#00f2fe)">📅</view>
            <text class="quick-text">学期管理</text>
          </view>
          <view class="quick-item" @click="goTo('/pages/admin/announce')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#fad961,#f76b1c)">📢</view>
            <text class="quick-text">公告管理</text>
          </view>
          <view class="quick-item" @click="switchTabTo('/pages/admin/analysis')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#43e97b,#38f9d7)">📊</view>
            <text class="quick-text">数据分析</text>
          </view>
          <view class="quick-item" @click="goTo('/pages/admin/feedback')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#a18cd1,#fbc2eb)">💬</view>
            <text class="quick-text">反馈查看</text>
          </view>
          <view class="quick-item" @click="switchTabTo('/pages/profile/index')">
            <view class="quick-icon" style="background: linear-gradient(135deg,#667eea,#764ba2)">👤</view>
            <text class="quick-text">个人中心</text>
          </view>
        </view>
      </view>

      <!-- 教师评分排行 -->
      <view class="card">
        <view class="card-header-row">
          <text class="card-title" style="margin-bottom:0">教师评分排行</text>
          <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
            <view class="picker-btn">
              <text>{{ semLabels[semIndex] || '全部' }}</text>
              <text class="arrow">›</text>
            </view>
          </picker>
        </view>
        <view v-if="statsLoading" class="loading-box" style="padding: 24rpx;"><text>加载中...</text></view>
        <view v-else-if="!teacherStats.length" class="empty-box" style="padding: 24rpx;"><text>暂无数据</text></view>
        <view v-for="(t, i) in teacherStats.slice(0, 10)" :key="t.userId" class="rank-item">
          <view class="rank-no" :class="['gold','silver','bronze'][i] || 'normal'">{{ i + 1 }}</view>
          <view class="rank-avatar">{{ t.realName?.[0] || 'T' }}</view>
          <view class="rank-body">
            <text class="rank-name">{{ t.realName }}</text>
            <text class="rank-dept">{{ t.department }}</text>
          </view>
          <view class="rank-right">
            <view class="score-tag" :class="scoreTagClass(t.avgScore)">{{ t.avgScore ?? '-' }}</view>
            <text class="eval-count">{{ t.evalCount }}次</text>
          </view>
        </view>
      </view>

      <!-- 评分分布 -->
      <view class="card">
        <view class="card-title">评分分布</view>
        <view v-if="!scoreDistData.length" class="empty-box" style="padding: 24rpx;"><text>暂无数据</text></view>
        <view v-else class="bar-chart">
          <view v-for="item in scoreDistData" :key="item.label" class="bar-item">
            <view class="bar-wrap">
              <view class="bar-fill" :style="`height: ${item.pct}%; background: ${item.color}`">
                <text class="bar-val">{{ item.count }}</text>
              </view>
            </view>
            <text class="bar-label">{{ item.label }}</text>
          </view>
        </view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/admin/index" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminDashboardApi, adminAnalysisApi, adminSemesterApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const overview        = ref(null)
const semesters       = ref([])
const semIndex        = ref(0)
const teacherStats    = ref([])
const statsLoading    = ref(false)
const currentSemester = ref(null)
const scoreDistRaw    = ref([])

const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)

const statCards = computed(() => [
  { label: '教师总数',   value: overview.value?.teacherCount, icon: '👨‍🏫', color: '#1a2a6c' },
  { label: '学生总数',   value: overview.value?.studentCount, icon: '🎒',  color: '#11998e' },
  { label: '评价总次数', value: overview.value?.evalCount,    icon: '✏️',  color: '#e6a23c' },
  { label: '本学期均分', value: overview.value?.avgScore,     icon: '📊',  color: '#f093fb' },
])

const scoreDistData = computed(() => {
  const dist = scoreDistRaw.value
  if (!dist.length) return []
  const max = Math.max(...dist.map(d => d.count), 1)
  const colors = ['#f56c6c','#e6a23c','#67c23a','#11998e','#1a2a6c']
  return dist.map((d, i) => ({
    ...d,
    pct: Math.round((d.count / max) * 90) + 10,
    color: colors[i] || '#1a2a6c',
  }))
})

function scoreTagClass(s) {
  const n = Number(s)
  if (n >= 85) return 'tag-success'
  if (n >= 70) return 'tag-warning'
  return 'tag-danger'
}

async function onSemChange(e) {
  semIndex.value = e.detail.value
  await loadStats()
}

async function loadStats() {
  statsLoading.value = true
  try {
    const [statsRes, distRes] = await Promise.allSettled([
      adminAnalysisApi.teacherStats(selectedSem.value),
      adminDashboardApi.scoreDist(selectedSem.value),
    ])
    if (statsRes.status === 'fulfilled') teacherStats.value = statsRes.value.data || []
    if (distRes.status  === 'fulfilled') scoreDistRaw.value  = distRes.value.data  || []
  } finally { statsLoading.value = false }
}

function goTo(url)         { uni.navigateTo({ url }) }
function switchTabTo(url)  { uni.switchTab({ url }) }

onMounted(async () => {
  const [ovRes, semRes] = await Promise.allSettled([
    adminDashboardApi.overview(),
    adminSemesterApi.list(),
  ])
  if (ovRes.status === 'fulfilled') overview.value = ovRes.value.data
  if (semRes.status === 'fulfilled') {
    semesters.value = semRes.value.data || []
    const cur = semesters.value.find(s => s.isCurrent === 1)
    currentSemester.value = cur
    const idx = semesters.value.indexOf(cur)
    if (idx >= 0) semIndex.value = idx
    await loadStats()
  }
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.manage-btns { display: flex; gap: 10rpx; flex-wrap: wrap; justify-content: flex-end; }
.manage-btn {
  background: rgba(255,255,255,0.2); color: #fff;
  border-radius: 20rpx; padding: 10rpx 20rpx;
  font-size: 22rpx; border: 2rpx solid rgba(255,255,255,0.4);
}
.content { padding: 24rpx; margin-top: -16rpx; }
.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; margin-bottom: 24rpx; }
.stat-card { background: #fff; border-radius: 16rpx; padding: 28rpx 20rpx; text-align: center; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06); }
.sc-icon { display: block; font-size: 48rpx; margin-bottom: 8rpx; }
.sc-val  { display: block; font-size: 40rpx; font-weight: 700; margin-bottom: 6rpx; }
.sc-lbl  { display: block; font-size: 22rpx; color: #909399; }

.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16rpx; }
.quick-item { display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.quick-icon {
  width: 96rpx; height: 96rpx; border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.12);
}
.quick-text { font-size: 22rpx; color: #606266; }

.card-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 26rpx; color: #1a2a6c; }
.arrow { color: #c0c4cc; }

.rank-item { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.rank-item:last-child { border-bottom: none; }
.rank-no {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: #f0f0f0; color: #909399; font-size: 24rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.gold   { background: #ffd700; color: #fff; }
.silver { background: #c0c0c0; color: #fff; }
.bronze { background: #cd7f32; color: #fff; }
.rank-avatar {
  width: 64rpx; height: 64rpx; border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff; font-size: 28rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.rank-body { flex: 1; overflow: hidden; }
.rank-name { display: block; font-size: 28rpx; font-weight: 600; color: #303133; }
.rank-dept { display: block; font-size: 22rpx; color: #909399; }
.rank-right { display: flex; flex-direction: column; align-items: center; gap: 6rpx; }
.score-tag { padding: 6rpx 16rpx; border-radius: 8rpx; font-size: 26rpx; font-weight: 700; }
.tag-success { background: #f0f9eb; color: #67c23a; }
.tag-warning { background: #fdf6ec; color: #e6a23c; }
.tag-danger  { background: #fef0f0; color: #f56c6c; }
.eval-count  { font-size: 20rpx; color: #c0c4cc; }

.bar-chart { display: flex; gap: 12rpx; height: 280rpx; align-items: flex-end; padding: 0 16rpx; }
.bar-item  { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-wrap  { flex: 1; width: 100%; display: flex; align-items: flex-end; padding-bottom: 8rpx; }
.bar-fill  { width: 70%; border-radius: 8rpx 8rpx 0 0; display: flex; align-items: flex-start; justify-content: center; padding-top: 6rpx; min-height: 40rpx; }
.bar-val   { font-size: 18rpx; color: #fff; font-weight: 600; }
.bar-label { font-size: 18rpx; color: #909399; margin-top: 6rpx; }
</style>
