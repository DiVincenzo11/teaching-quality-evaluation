<template>
  <view class="page-bg">
    <nav-bar title="教学质量报告" />

    <!-- 学期选择 -->
    <view class="card filter-row">
      <text class="fl">选择学期</text>
      <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
        <view class="picker-btn">
          <text>{{ semLabels[semIndex] || '全部' }}</text>
          <text class="arrow">›</text>
        </view>
      </picker>
    </view>

    <view v-if="loading" class="loading-box">
      <text class="loading-icon">⏳</text>
      <text>报告生成中，请稍候...</text>
    </view>

    <view v-else-if="noData" class="empty-box">
      <text class="empty-icon">📄</text>
      <text>该学期暂无评价数据，无法生成报告</text>
    </view>

    <template v-else-if="report">
      <!-- ① 基本信息 -->
      <view class="card">
        <view class="card-title">基本信息</view>
        <view class="info-row"><text class="il">教师姓名</text><text class="iv">{{ report.teacherName }}</text></view>
        <view class="info-row"><text class="il">所在院系</text><text class="iv">{{ report.department || '--' }}</text></view>
        <view class="info-row" v-if="report.title"><text class="il">职称</text><text class="iv">{{ report.title }}</text></view>
        <view class="info-row"><text class="il">当前学期</text><text class="iv">{{ currentSemName }}</text></view>
        <view class="info-row" v-if="report.courses?.length">
          <text class="il">授课科目</text>
          <text class="iv">{{ report.courses.join('、') }}</text>
        </view>
        <view class="info-row" v-if="report.classes">
          <text class="il">所教班级</text>
          <text class="iv">{{ report.classes }}</text>
        </view>
      </view>

      <!-- ② 评价总览 -->
      <view class="card">
        <view class="card-title">评价总览</view>
        <view class="level-badge" :class="levelClass">{{ report.overallLevel || '暂无' }}</view>
        <view class="overview-grid">
          <view class="ov-item green">
            <text class="ov-val">{{ report.avgScore ?? '--' }}</text>
            <text class="ov-lbl">综合均分</text>
          </view>
          <view class="ov-item blue">
            <text class="ov-val">{{ report.evalCount ?? '--' }}</text>
            <text class="ov-lbl">评价人次</text>
          </view>
          <view class="ov-item orange">
            <text class="ov-val">{{ report.rank != null ? '#' + report.rank : '--' }}</text>
            <text class="ov-lbl">全校排名</text>
          </view>
          <view class="ov-item gray">
            <text class="ov-val">{{ report.allAvgScore ?? '--' }}</text>
            <text class="ov-lbl">全校均分</text>
          </view>
        </view>
        <view class="compare-bar" v-if="report.avgScore && report.allAvgScore">
          <text class="cb-label">我的均分 vs 全校均分</text>
          <view class="bar-wrap">
            <view class="bar-row">
              <text class="bar-name">本人</text>
              <view class="bar-track">
                <view class="bar-fill mine" :style="{ width: Math.min(report.avgScore / 100 * 100, 100) + '%' }" />
              </view>
              <text class="bar-val">{{ report.avgScore }}</text>
            </view>
            <view class="bar-row">
              <text class="bar-name">全校</text>
              <view class="bar-track">
                <view class="bar-fill all" :style="{ width: Math.min(report.allAvgScore / 100 * 100, 100) + '%' }" />
              </view>
              <text class="bar-val">{{ report.allAvgScore }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ③ 维度得分率 -->
      <view class="card" v-if="dimList.length">
        <view class="card-title">各维度得分率</view>
        <view v-for="d in dimList" :key="d.name" class="dim-item">
          <view class="dim-head">
            <text class="dim-name">{{ d.name }}</text>
            <text class="dim-pct" :style="{ color: d.color }">{{ d.pct }}%</text>
          </view>
          <view class="prog-track">
            <view class="prog-fill" :style="{ width: d.pct + '%', background: d.color }" />
          </view>
        </view>
      </view>

      <!-- ④ 优势维度 -->
      <view class="card" v-if="report.strengths?.length">
        <view class="card-title">✅ 优势维度</view>
        <view v-for="(s, i) in report.strengths" :key="i" class="point-item strength">
          <text class="point-icon">▸</text>
          <text class="point-text">{{ s }}</text>
        </view>
      </view>

      <!-- ⑤ 诊断与改进 -->
      <view class="card" v-if="report.weaknesses?.length">
        <view class="card-title">⚠️ 诊断与改进方向</view>
        <view v-for="w in report.weaknesses" :key="w.dimension" class="weakness-item">
          <view class="wk-head">
            <text class="wk-dim">{{ w.dimension }}</text>
            <view class="severity-badge" :class="sevClass(w.severity)">{{ w.severity }}</view>
          </view>
          <view class="wk-stat-row">
            <text class="wk-stat">当前得分率 <text class="wk-num">{{ w.scoreRatio }}%</text></text>
            <text class="wk-stat">目标 <text class="wk-num">{{ w.threshold }}%</text></text>
            <text class="wk-stat">差距 <text class="wk-gap">-{{ w.gap }}%</text></text>
          </view>
          <view class="wk-prog">
            <view class="wk-fill" :style="{ width: w.scoreRatio + '%' }" />
            <view class="wk-target-line" :style="{ left: w.threshold + '%' }" />
          </view>
        </view>
      </view>

      <!-- ⑥ 知识图谱推荐 -->
      <view class="card" v-if="report.graphRecommendations?.length">
        <view class="card-title">📚 知识图谱提升路径</view>
        <view v-for="rec in report.graphRecommendations" :key="rec.problem" class="rec-item">
          <view class="rec-head">
            <text class="rec-problem">{{ rec.problem }}</text>
            <view class="severity-badge" :class="sevClass(rec.severity)">{{ rec.severity }}</view>
          </view>
          <!-- 图谱路径 -->
          <view class="graph-path" v-if="rec.graphPath?.length">
            <view v-for="(step, si) in rec.graphPath" :key="si" class="gp-step-wrap">
              <view class="gp-step">{{ step }}</view>
              <text v-if="si < rec.graphPath.length - 1" class="gp-arrow">→</text>
            </view>
          </view>
          <!-- 改进策略 -->
          <view class="strategy-list" v-if="rec.strategies?.length">
            <text class="sub-title">改进策略</text>
            <view v-for="(st, si) in rec.strategies" :key="si" class="strategy-item">
              <text class="st-num">{{ si + 1 }}</text>
              <text class="st-text">{{ st }}</text>
            </view>
          </view>
          <!-- 推荐资源 -->
          <view class="resource-list" v-if="rec.resources?.length">
            <text class="sub-title">推荐学习资源</text>
            <view v-for="res in rec.resources" :key="res.title" class="res-item">
              <view class="res-type-tag">{{ res.type }}</view>
              <text class="res-title">{{ res.title }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ⑦ 学生评语 -->
      <view class="card" v-if="report.comments?.length">
        <view class="card-title">💬 学生匿名评语（{{ report.comments.length }}条）</view>
        <view class="comment-scroll">
          <view v-for="(c, i) in visibleComments" :key="i" class="comment-item">
            <text class="c-avatar">{{ avatarEmoji(i) }}</text>
            <text class="c-text">{{ c }}</text>
          </view>
        </view>
        <view v-if="report.comments.length > commentLimit" class="load-more" @click="commentLimit += 5">
          <text>查看更多评语 ({{ report.comments.length - commentLimit }} 条)</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherApi } from '@/api/index'
import NavBar from '@/components/nav-bar/index.vue'

const semesters    = ref([])
const semIndex     = ref(0)
const report       = ref(null)
const loading      = ref(false)
const noData       = ref(false)
const commentLimit = ref(5)

const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)
const currentSemName = computed(() => semesters.value[semIndex.value]?.semesterName || '--')

const DIM_COLORS = { '教学内容与方法': '#5470c6', '教学态度与互动': '#91cc75', '教学效果与收获': '#fac858' }

const dimList = computed(() => {
  if (!report.value?.dimensionScores) return []
  return Object.entries(report.value.dimensionScores).map(([name, ratio]) => ({
    name,
    pct: Math.round(ratio * 100),
    color: DIM_COLORS[name] || '#409eff',
  }))
})

const visibleComments = computed(() => (report.value?.comments || []).slice(0, commentLimit.value))

const levelClass = computed(() => {
  const map = { '优秀': 'lv-excellent', '良好': 'lv-good', '中等': 'lv-medium', '及格': 'lv-pass', '待提升': 'lv-low' }
  return map[report.value?.overallLevel] || 'lv-medium'
})

const avatarEmoji = (i) => ['😊', '🙂', '😄', '🤗', '😎', '🧐'][i % 6]

const SEV_MAP = { '严重': 'sev-critical', '中等': 'sev-medium', '轻微': 'sev-minor', '提升空间': 'sev-improve' }
const sevClass = (s) => SEV_MAP[s] || 'sev-minor'

async function onSemChange(e) {
  semIndex.value = e.detail.value
  commentLimit.value = 5
  await loadReport()
}

async function loadReport() {
  loading.value = true
  noData.value = false
  report.value = null
  try {
    const res = await teacherApi.report(selectedSem.value)
    const d = res.data
    if (!d || (d.evalCount === 0 && !d.dimensionScores)) {
      noData.value = true
    } else {
      report.value = d
    }
  } catch (e) {
    noData.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const semRes = await teacherApi.semesters()
    semesters.value = semRes.data || []
    const cur = semesters.value.findIndex(s => s.isCurrent === 1)
    semIndex.value = cur >= 0 ? cur : 0
  } catch {}
  await loadReport()
})
</script>

<style scoped>
.page-bg { background: #f5f7fa; min-height: 100vh; padding-bottom: 40rpx; }

/* 通用卡片 */
.card { background: #fff; border-radius: 20rpx; margin: 20rpx 24rpx 0; padding: 28rpx 28rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,.06); }
.card-title { font-size: 30rpx; font-weight: 700; color: #303133; margin-bottom: 20rpx; padding-bottom: 14rpx; border-bottom: 2rpx solid #f0f2f5; }

/* 学期选择 */
.filter-row { display: flex; justify-content: space-between; align-items: center; }
.fl { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }

/* 加载 / 空状态 */
.loading-box, .empty-box { text-align: center; padding: 80rpx 40rpx; color: #909399; font-size: 28rpx; }
.loading-icon, .empty-icon { display: block; font-size: 64rpx; margin-bottom: 16rpx; }

/* 基本信息 */
.info-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.il { font-size: 26rpx; color: #909399; flex-shrink: 0; width: 150rpx; }
.iv { font-size: 26rpx; color: #303133; font-weight: 500; flex: 1; text-align: right; }

/* 等级徽章 */
.level-badge { display: inline-block; font-size: 24rpx; font-weight: 700; padding: 6rpx 24rpx; border-radius: 30rpx; margin-bottom: 20rpx; }
.lv-excellent { background: #f0f9eb; color: #67c23a; }
.lv-good      { background: #ecf5ff; color: #409eff; }
.lv-medium    { background: #fdf6ec; color: #e6a23c; }
.lv-pass      { background: #f4f4f5; color: #909399; }
.lv-low       { background: #fef0f0; color: #f56c6c; }

/* 总览格子 */
.overview-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; margin-bottom: 24rpx; }
.ov-item { background: #f5f7fa; border-radius: 16rpx; padding: 24rpx 16rpx; text-align: center; }
.ov-val  { display: block; font-size: 44rpx; font-weight: 700; margin-bottom: 8rpx; }
.ov-lbl  { display: block; font-size: 22rpx; color: #909399; }
.green  .ov-val { color: #11998e; }
.blue   .ov-val { color: #1a2a6c; }
.orange .ov-val { color: #e6a23c; }
.gray   .ov-val { color: #909399; }

/* 对比条形图 */
.compare-bar { }
.cb-label { display: block; font-size: 24rpx; color: #909399; margin-bottom: 12rpx; }
.bar-wrap { display: flex; flex-direction: column; gap: 12rpx; }
.bar-row { display: flex; align-items: center; gap: 12rpx; }
.bar-name { font-size: 22rpx; color: #606266; width: 50rpx; flex-shrink: 0; }
.bar-track { flex: 1; height: 16rpx; background: #f0f2f5; border-radius: 8rpx; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 8rpx; transition: width .4s; }
.bar-fill.mine { background: linear-gradient(90deg, #1a2a6c, #11998e); }
.bar-fill.all  { background: #c0c4cc; }
.bar-val { font-size: 22rpx; color: #303133; font-weight: 500; width: 60rpx; text-align: right; flex-shrink: 0; }

/* 维度得分率 */
.dim-item { margin-bottom: 20rpx; }
.dim-item:last-child { margin-bottom: 0; }
.dim-head { display: flex; justify-content: space-between; margin-bottom: 8rpx; }
.dim-name { font-size: 26rpx; color: #303133; }
.dim-pct  { font-size: 26rpx; font-weight: 700; }
.prog-track { height: 16rpx; background: #f0f2f5; border-radius: 8rpx; overflow: hidden; }
.prog-fill { height: 100%; border-radius: 8rpx; transition: width .4s; }

/* 优势列表 */
.point-item { display: flex; gap: 12rpx; padding: 12rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.point-item:last-child { border-bottom: none; }
.point-icon { font-size: 28rpx; flex-shrink: 0; margin-top: 2rpx; }
.point-text { font-size: 26rpx; line-height: 1.7; flex: 1; }
.strength .point-text { color: #11998e; }

/* 诊断弱项 */
.weakness-item { margin-bottom: 24rpx; padding-bottom: 24rpx; border-bottom: 2rpx solid #f5f5f5; }
.weakness-item:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
.wk-head { display: flex; align-items: center; gap: 12rpx; margin-bottom: 10rpx; }
.wk-dim  { font-size: 28rpx; font-weight: 600; color: #303133; }
.wk-stat-row { display: flex; gap: 20rpx; margin-bottom: 10rpx; }
.wk-stat { font-size: 22rpx; color: #909399; }
.wk-num  { color: #303133; font-weight: 500; }
.wk-gap  { color: #f56c6c; font-weight: 600; }
.wk-prog { position: relative; height: 14rpx; background: #f0f2f5; border-radius: 8rpx; overflow: visible; }
.wk-fill { position: absolute; left: 0; top: 0; height: 100%; background: linear-gradient(90deg, #e6a23c, #f56c6c); border-radius: 8rpx; }
.wk-target-line { position: absolute; top: -4rpx; width: 4rpx; height: 22rpx; background: #909399; border-radius: 2rpx; }

/* 严重程度徽章 */
.severity-badge { font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 20rpx; font-weight: 500; }
.sev-critical { background: #fef0f0; color: #f56c6c; }
.sev-medium   { background: #fdf6ec; color: #e6a23c; }
.sev-minor    { background: #ecf5ff; color: #409eff; }
.sev-improve  { background: #f0f9eb; color: #67c23a; }

/* 知识图谱推荐 */
.rec-item { margin-bottom: 32rpx; padding-bottom: 32rpx; border-bottom: 2rpx solid #f0f2f5; }
.rec-item:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
.rec-head { display: flex; align-items: center; gap: 12rpx; margin-bottom: 14rpx; }
.rec-problem { font-size: 28rpx; font-weight: 600; color: #303133; }

.graph-path { display: flex; flex-wrap: wrap; align-items: center; gap: 6rpx; margin-bottom: 16rpx; padding: 14rpx; background: #f5f7fa; border-radius: 12rpx; }
.gp-step-wrap { display: flex; align-items: center; gap: 6rpx; }
.gp-step { font-size: 22rpx; background: #ecf5ff; color: #409eff; padding: 6rpx 14rpx; border-radius: 8rpx; }
.gp-arrow { font-size: 20rpx; color: #c0c4cc; }

.sub-title { display: block; font-size: 24rpx; color: #909399; margin: 12rpx 0 10rpx; font-weight: 600; }
.strategy-list, .resource-list { }
.strategy-item { display: flex; align-items: flex-start; gap: 12rpx; padding: 8rpx 0; }
.st-num { flex-shrink: 0; width: 32rpx; height: 32rpx; background: #1a2a6c; color: #fff; font-size: 20rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.st-text { font-size: 26rpx; color: #606266; line-height: 1.6; flex: 1; }

.res-item { display: flex; align-items: center; gap: 12rpx; padding: 10rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.res-item:last-child { border-bottom: none; }
.res-type-tag { flex-shrink: 0; font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; background: #f0f9eb; color: #67c23a; }
.res-title { font-size: 24rpx; color: #303133; flex: 1; }

/* 评语 */
.comment-item { display: flex; gap: 16rpx; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; align-items: flex-start; }
.comment-item:last-child { border-bottom: none; }
.c-avatar { font-size: 40rpx; flex-shrink: 0; }
.c-text { font-size: 26rpx; color: #606266; line-height: 1.7; flex: 1; }
.load-more { margin-top: 20rpx; text-align: center; padding: 16rpx; background: #f5f7fa; border-radius: 12rpx; font-size: 26rpx; color: #409eff; }
</style>
