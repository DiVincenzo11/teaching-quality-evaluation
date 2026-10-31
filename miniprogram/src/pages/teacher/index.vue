<template>
  <view class="page-bg">
    <nav-bar title="教学概览" />
    <view class="welcome-banner">
      <view class="wb-text">
        <text class="wb-title">{{ teacherInfo.realName || userInfo?.realName }} 老师 👋</text>
        <text class="wb-sub" v-if="teacherInfo.courses?.length">所教科目：{{ teacherInfo.courses?.join('、') }}</text>
        <text class="wb-sub" v-if="teacherInfo.classes">所教班级：{{ teacherInfo.classes }}</text>
      </view>
      <view class="wb-btn" @click="goEvalView">查看评价</view>
    </view>

    <view class="content">
      <view class="stats-row">
        <view class="stat-card" style="border-left: 8rpx solid #11998e;">
          <text class="sn green">{{ stats.avgScore ?? '--' }}</text>
          <text class="sl">综合均分</text>
        </view>
        <view class="stat-card" style="border-left: 8rpx solid #1a2a6c;">
          <text class="sn blue">{{ stats.evalCount ?? '--' }}</text>
          <text class="sl">评价次数</text>
        </view>
        <view class="stat-card" style="border-left: 8rpx solid #f093fb;">
          <text class="sn purple">{{ stats.rankInDept > 0 ? ('第' + stats.rankInDept + '名') : '--' }}</text>
          <text class="sl">院系排名</text>
        </view>
      </view>

      <view class="card sem-row">
        <text class="sem-label">当前学期</text>
        <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
          <view class="picker-btn">
            <text>{{ semLabels[semIndex] || '请选择学期' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <!-- 各维度评分 -->
      <view class="card">
        <view class="card-title">各维度评分（{{ questionData.length || 12 }}题）</view>
        <view v-if="loadingStats" class="loading-box"><text>加载评价数据中...</text></view>
        <view v-else-if="!hasData" class="empty-box">
          <text class="empty-icon">📊</text>
          <text>该学期暂无评价数据</text>
        </view>
        <template v-else>
          <view class="canvas-wrap" @click="hideTooltip">
            <canvas canvas-id="radarCanvas" id="radarCanvas"
                    :style="{ width: canvasW + 'px', height: canvasH + 'px' }"
                    @touchstart="onCanvasTouch" />
            <!-- 点击数据点弹出的提示框 -->
            <view v-if="tooltip.visible" class="tt-box"
                  :style="{ left: tooltip.left + 'px', top: tooltip.top + 'px' }"
                  @click.stop>
              <text class="tt-q">{{ tooltip.label }}</text>
              <text class="tt-pct">{{ tooltip.pct }}%</text>
              <text class="tt-detail">均分 {{ tooltip.avg }} / 满分 {{ tooltip.max }}</text>
              <view class="tt-arrow" />
            </view>
          </view>
          <view class="dim-list" v-if="questionGroups.length">
            <view v-for="g in questionGroups" :key="g.label" class="dim-item">
              <view class="dim-dot" :style="`background: ${g.color}`"/>
              <view class="dim-body">
                <view class="dim-row1">
                  <text class="dim-label">{{ g.shortLabel }}</text>
                  <text class="dim-range" :style="`color: ${g.color}`">Q{{ g.from }}–Q{{ g.to }}</text>
                  <text class="dim-pct" :style="`color: ${g.color}`">{{ g.avgPct }}%</text>
                </view>
                <view class="dim-progress">
                  <view class="prog-bar">
                    <view class="prog-fill" :style="`width: ${g.avgPct}%; background: ${g.color}`" />
                  </view>
                  <text class="dim-detail">均分 {{ g.avgRaw }} / {{ g.maxRaw }}</text>
                </view>
              </view>
            </view>
          </view>
        </template>
      </view>

      <!-- 教学质量报告入口 -->
      <view class="card report-entry" @click="uni.navigateTo({ url: '/pages/teacher/report' })">
        <view class="re-left">
          <text class="re-icon">📊</text>
          <view class="re-text">
            <text class="re-title">教学质量报告</text>
            <text class="re-sub">查看维度诊断、改进建议与资源推荐</text>
          </view>
        </view>
        <text class="re-arrow">›</text>
      </view>

      <!-- 最新评语 -->
      <view class="card">
        <view class="card-title">最新学生评语（匿名）</view>
        <view v-if="!essays.length" class="empty-box"><text>暂无评语</text></view>
        <view v-for="(e, i) in essays" :key="i" class="essay-item">
          <text class="essay-avatar">{{ avatarEmoji(i) }}</text>
          <view class="essay-body">
            <text class="essay-content">{{ e.essayContent }}</text>
            <text class="essay-time">{{ e.submitTime?.slice(0, 10) }}</text>
          </view>
        </view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/teacher/index" />
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { teacherApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const userStore  = useUserStore()
const userInfo   = computed(() => userStore.userInfo)

const teacherInfo  = reactive({ realName: '', courses: [], classes: '' })
const stats        = reactive({ avgScore: null, evalCount: null, rankInDept: null })
const questionGroups = ref([])
const essays       = ref([])
const loadingStats = ref(false)
const hasData      = ref(false)
const semesters    = ref([])
const semIndex     = ref(0)
const questionData = ref([])

const winInfo = uni.getWindowInfo()
const canvasW = ref(winInfo.windowWidth - 56)
const canvasH = ref(Math.round((winInfo.windowWidth - 56) * 0.85))

const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)

const DIM_COLORS = ['#5470c6', '#91cc75', '#fac858']
const DIM_GROUPS = [
  { label: '教学内容与方法', shortLabel: '内容与方法', from: 1, to: 4, color: DIM_COLORS[0] },
  { label: '教学态度与互动', shortLabel: '态度与互动', from: 5, to: 8, color: DIM_COLORS[1] },
  { label: '教学效果与收获', shortLabel: '效果与收获', from: 9, to: 12, color: DIM_COLORS[2] },
]

const avatarEmoji = (i) => ['😊','🙂','😄','🤗','😎','🧐'][i % 6]

// 雷达图点击提示
const tooltip = reactive({ visible: false, left: 0, top: 0, label: '', pct: 0, avg: '', max: '' })
let tooltipTimer = null

function hideTooltip() { tooltip.visible = false }

function onCanvasTouch(e) {
  const touch = e.touches[0]
  const tx = touch.x, ty = touch.y
  const W = canvasW.value, H = canvasH.value
  const cx = W / 2, cy = H / 2
  const r = Math.min(W, H) * 0.36
  const n = 12
  const qData = questionData.value.filter(q => q.questionNo >= 1 && q.questionNo <= n)

  let minDist = Infinity, nearestIdx = -1
  for (let i = 0; i < n; i++) {
    const q = qData.find(q => q.questionNo === i + 1)
    const pct = (q && q.maxScore > 0) ? Math.min(q.avgScore / q.maxScore, 1) : 0
    const angle = (Math.PI * 2 * i / n) - Math.PI / 2
    const px = cx + r * pct * Math.cos(angle)
    const py = cy + r * pct * Math.sin(angle)
    const dist = Math.sqrt((tx - px) ** 2 + (ty - py) ** 2)
    if (dist < minDist) { minDist = dist; nearestIdx = i }
  }

  if (nearestIdx >= 0 && minDist < 40) {
    const q = qData.find(q => q.questionNo === nearestIdx + 1)
    const pctVal = (q && q.maxScore > 0) ? Math.round((q.avgScore / q.maxScore) * 100) : 0
    const angle  = (Math.PI * 2 * nearestIdx / n) - Math.PI / 2
    const dataPct = (q && q.maxScore > 0) ? Math.min(q.avgScore / q.maxScore, 1) : 0
    const px = cx + r * dataPct * Math.cos(angle)
    const py = cy + r * dataPct * Math.sin(angle)

    const ttW = 130, ttH = 64
    let left = px - ttW / 2
    let top  = py - ttH - 14
    if (left < 4) left = 4
    if (left + ttW > W - 4) left = W - ttW - 4
    if (top < 4) top = py + 18

    tooltip.visible = true
    tooltip.left  = left
    tooltip.top   = top
    tooltip.label = `Q${nearestIdx + 1}`
    tooltip.pct   = pctVal
    tooltip.avg   = q?.avgScore != null ? Number(q.avgScore).toFixed(2) : '0.00'
    tooltip.max   = q?.maxScore != null ? Number(q.maxScore).toFixed(1)  : '0.0'

    clearTimeout(tooltipTimer)
    tooltipTimer = setTimeout(() => { tooltip.visible = false }, 3000)
  } else {
    tooltip.visible = false
  }
}

function goEvalView() { uni.switchTab({ url: '/pages/teacher/eval-view' }) }

async function onSemChange(e) {
  semIndex.value = e.detail.value
  await loadStats()
}

async function loadStats() {
  loadingStats.value = true
  hasData.value = false
  try {
    const res = await teacherApi.evalStats(selectedSem.value)
    const d = res.data || {}
    stats.avgScore   = d.avgScore
    stats.evalCount  = d.evalCount
    stats.rankInDept = d.rankInDept
    essays.value     = (d.essayList || []).slice(0, 8)

    const qData = d.questionData || []
    questionData.value = qData

    if (qData.length > 0) {
      hasData.value = true
      questionGroups.value = DIM_GROUPS.map(g => {
        const qNos  = Array.from({ length: g.to - g.from + 1 }, (_, i) => g.from + i)
        const items = qNos.map(no => qData.find(q => q.questionNo === no)).filter(Boolean)
        const pcts  = items.map(q => q.maxScore > 0 ? (q.avgScore / q.maxScore) * 100 : 0)
        const avgPct= items.length ? Math.round(pcts.reduce((a, b) => a + b, 0) / pcts.length) : 0
        const avgRaw= items.length ? (items.reduce((a, q) => a + (q.avgScore || 0), 0) / items.length).toFixed(2) : '0.00'
        const maxRaw= items.length ? (items.reduce((a, q) => a + (q.maxScore || 0), 0) / items.length).toFixed(1) : '0.0'
        return { ...g, avgPct, avgRaw, maxRaw }
      })
      setTimeout(() => drawRadar(qData), 300)
    } else {
      questionGroups.value = []
    }
  } catch (e) {
    console.error('loadStats error', e)
  } finally {
    loadingStats.value = false
  }
}

function drawRadar(qData) {
  const W = canvasW.value, H = canvasH.value
  const cx = W / 2, cy = H / 2
  const r = Math.min(W, H) * 0.36, n = 12
  const ctx = uni.createCanvasContext('radarCanvas')
  ctx.clearRect(0, 0, W, H)

  for (let lv = 1; lv <= 5; lv++) {
    ctx.beginPath()
    const lr = r * lv / 5
    for (let i = 0; i < n; i++) {
      const angle = (Math.PI * 2 * i / n) - Math.PI / 2
      const x = cx + lr * Math.cos(angle), y = cy + lr * Math.sin(angle)
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    }
    ctx.closePath()
    ctx.setStrokeStyle(lv === 5 ? '#bbb' : '#e8e8e8')
    ctx.setLineWidth(lv === 5 ? 1.5 : 1)
    ctx.stroke()
  }

  for (let i = 0; i < n; i++) {
    const angle = (Math.PI * 2 * i / n) - Math.PI / 2
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.lineTo(cx + r * Math.cos(angle), cy + r * Math.sin(angle))
    ctx.setStrokeStyle('#e0e0e0'); ctx.setLineWidth(1); ctx.stroke()
  }

  const validQ = qData.filter(q => q.questionNo >= 1 && q.questionNo <= 12)
  if (validQ.length >= 12) {
    ctx.beginPath()
    for (let i = 0; i < 12; i++) {
      const q = validQ.find(q => q.questionNo === i + 1)
      const pct = (q && q.maxScore > 0) ? Math.min(q.avgScore / q.maxScore, 1) : 0
      const angle = (Math.PI * 2 * i / n) - Math.PI / 2
      const x = cx + r * pct * Math.cos(angle), y = cy + r * pct * Math.sin(angle)
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
    }
    ctx.closePath()
    ctx.setFillStyle('rgba(45,106,159,0.22)'); ctx.fill()
    ctx.setStrokeStyle('#2d6a9f'); ctx.setLineWidth(2.5); ctx.stroke()
    for (let i = 0; i < 12; i++) {
      const q = validQ.find(q => q.questionNo === i + 1)
      const pct = (q && q.maxScore > 0) ? Math.min(q.avgScore / q.maxScore, 1) : 0
      const angle = (Math.PI * 2 * i / n) - Math.PI / 2
      const x = cx + r * pct * Math.cos(angle), y = cy + r * pct * Math.sin(angle)
      ctx.beginPath(); ctx.arc(x, y, 5, 0, Math.PI * 2)
      ctx.setFillStyle('#2d6a9f'); ctx.fill()
    }
  }

  // 只显示 Q 标签，不显示百分比数字（点击数据点时弹出浮层查看详情）
  const fontSize = Math.max(11, Math.round(W * 0.034))
  for (let i = 0; i < n; i++) {
    const angle = (Math.PI * 2 * i / n) - Math.PI / 2
    const lx = cx + (r + fontSize * 2.0) * Math.cos(angle)
    const ly = cy + (r + fontSize * 2.0) * Math.sin(angle)
    const q = validQ.find(q => q.questionNo === i + 1)
    const pct = (q && q.maxScore > 0) ? Math.round((q.avgScore / q.maxScore) * 100) : 0
    const color = pct >= 85 ? '#11998e' : pct >= 70 ? '#e6a23c' : '#f56c6c'
    ctx.setFillStyle(color)
    ctx.setFontSize(fontSize)
    ctx.setTextAlign('center')
    ctx.fillText(`Q${i + 1}`, lx, ly + fontSize * 0.4)
  }
  ctx.draw(true)
}

onMounted(async () => {
  try {
    const infoRes = await teacherApi.info(null)
    Object.assign(teacherInfo, infoRes.data || {})
  } catch {}
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
.welcome-banner {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  padding: 80rpx 40rpx 48rpx;
  display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx;
}
.wb-text  { flex: 1; }
.wb-title { display: block; font-size: 36rpx; font-weight: 700; color: #fff; margin-bottom: 8rpx; }
.wb-sub   { display: block; font-size: 22rpx; color: rgba(255,255,255,0.8); margin-top: 4rpx; }
.wb-btn {
  background: rgba(255,255,255,0.2); color: #fff; border-radius: 40rpx;
  padding: 14rpx 28rpx; font-size: 24rpx; border: 2rpx solid rgba(255,255,255,0.5);
  flex-shrink: 0; margin-top: 4rpx;
}
.content { padding: 24rpx; margin-top: -24rpx; }
.stats-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.stat-card {
  flex: 1; background: #fff; border-radius: 16rpx;
  padding: 24rpx 12rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.07); text-align: center;
}
.sn { display: block; font-size: 34rpx; font-weight: 700; margin-bottom: 6rpx; }
.sl { display: block; font-size: 20rpx; color: #909399; }
.green  { color: #11998e; }
.blue   { color: #1a2a6c; }
.purple { color: #9c27b0; }
.sem-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.sem-label { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }
.canvas-wrap { display: flex; justify-content: center; margin-bottom: 20rpx; overflow: hidden; position: relative; }

/* 雷达图点击提示浮层 */
.tt-box {
  position: absolute;
  background: rgba(30, 40, 80, 0.92);
  border-radius: 12rpx;
  padding: 12rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  pointer-events: none;
  z-index: 10;
  min-width: 130px;
}
.tt-q      { font-size: 24rpx; color: rgba(255,255,255,0.7); font-weight: 500; }
.tt-pct    { font-size: 38rpx; font-weight: 700; color: #fff; line-height: 1.1; }
.tt-detail { font-size: 20rpx; color: rgba(255,255,255,0.65); }
.tt-arrow  {
  position: absolute;
  bottom: -10rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 0; height: 0;
  border-left: 10rpx solid transparent;
  border-right: 10rpx solid transparent;
  border-top: 10rpx solid rgba(30, 40, 80, 0.92);
}
.dim-list { display: flex; flex-direction: column; gap: 16rpx; margin-top: 4rpx; }
.dim-item { display: flex; gap: 16rpx; align-items: flex-start; }
.dim-dot  { width: 16rpx; height: 16rpx; border-radius: 50%; margin-top: 10rpx; flex-shrink: 0; }
.dim-body { flex: 1; }
.dim-row1 { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.dim-label { font-size: 26rpx; color: #303133; font-weight: 500; flex: 1; }
.dim-range { font-size: 22rpx; }
.dim-pct   { font-size: 28rpx; font-weight: 700; }
.dim-progress { display: flex; align-items: center; gap: 12rpx; }
.prog-bar { flex: 1; height: 12rpx; background: #f0f0f0; border-radius: 6rpx; overflow: hidden; }
.prog-fill { height: 100%; border-radius: 6rpx; }
.dim-detail { font-size: 20rpx; color: #909399; white-space: nowrap; }
.essay-item { display: flex; gap: 16rpx; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.essay-item:last-child { border-bottom: none; }
.essay-avatar { font-size: 52rpx; flex-shrink: 0; }
.essay-body   { flex: 1; }
.essay-content { display: block; font-size: 26rpx; color: #303133; line-height: 1.7; margin-bottom: 8rpx; }
.essay-time    { font-size: 22rpx; color: #c0c4cc; }

/* 教学质量报告入口 */
.report-entry { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 28rpx; cursor: pointer; }
.re-left   { display: flex; align-items: center; gap: 20rpx; }
.re-icon   { font-size: 52rpx; flex-shrink: 0; }
.re-text   { display: flex; flex-direction: column; gap: 6rpx; }
.re-title  { font-size: 30rpx; font-weight: 600; color: #303133; }
.re-sub    { font-size: 22rpx; color: #909399; }
.re-arrow  { font-size: 40rpx; color: #c0c4cc; }
</style>
