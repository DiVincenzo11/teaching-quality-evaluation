<template>
  <div class="dashboard">
    <div class="welcome-banner">
      <div>
        <h2>您好，{{ teacherInfo.realName || userStore.userInfo?.realName }} 老师 👋</h2>
        <p>欢迎使用教学质量评价系统，及时查看评价并持续改进教学质量</p>
        <p v-if="teacherInfo.courses?.length || teacherInfo.classes" class="teacher-intro">
          所教科目：{{ teacherInfo.courses?.join('、') || '—' }}；所教班级：{{ teacherInfo.classes || '—' }}
        </p>
      </div>
      <el-button type="primary" @click="$router.push('/teacher/eval')">查看评价详情</el-button>
    </div>

    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="8" v-for="card in cards" :key="card.label">
        <div class="stat-card" :style="`--c: ${card.color}`">
          <el-icon size="40" :color="card.color"><component :is="card.icon" /></el-icon>
          <div>
            <div class="val">{{ card.value }}</div>
            <div class="lbl">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>各维度评分</span></template>
          <div class="radar-with-scores">
            <!-- 雷达图 -->
            <div
              class="radar-chart-wrap"
              @mousemove="onRadarMouseMove"
              @mouseleave="radarTip.show = false"
            >
              <div ref="radarRef" style="width:100%;height:320px"></div>
              <div v-if="radarTip.show" class="radar-tip" :style="`left:${radarTip.x}px;top:${radarTip.y}px`">
                <div class="rt-title">Q{{ radarTip.questionNo }}</div>
                <div class="rt-pct" :style="`color:${radarTip.color}`">{{ radarTip.pct }}%</div>
                <div class="rt-sub">均分 {{ radarTip.avg }} / 满分 {{ radarTip.max }}</div>
              </div>
            </div>

            <!-- 维度汇总得分 -->
            <div v-if="questionGroups.length" class="dim-scores">
              <div
                v-for="group in questionGroups"
                :key="group.label"
                class="dim-card"
                :style="`border-left-color:${group.color}`"
              >
                <div class="dim-label">{{ group.shortLabel }}</div>
                <div class="dim-q-range" :style="`color:${group.color}`">
                  Q{{ group.from }}–Q{{ group.to }}
                </div>
                <div class="dim-score-row">
                  <span class="dim-score-val" :style="`color:${group.color}`">
                    {{ group.avgPct }}%
                  </span>
                  <span class="dim-score-sub">得分率</span>
                </div>
                <el-progress
                  :percentage="group.avgPct"
                  :color="group.color"
                  :stroke-width="6"
                  :show-text="false"
                />
                <div class="dim-score-detail">
                  均分 <b :style="`color:${group.color}`">{{ group.avgRaw }}</b> /
                  满分 {{ group.maxRaw }}
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>最新学生评语（匿名）</span></template>
          <div class="comments">
            <div v-if="!commentsPositive?.length && !commentsNeutral?.length && !commentsNegative?.length" class="empty">暂无评语</div>
            <template v-else>
              <!-- 好评/中评/差评占比统计条 -->
              <div class="pct-bar-wrap">
                <div class="pct-bar">
                  <div class="pct-seg good"  :style="`width:${pctPositive}%`" :title="`好评 ${pctPositive}%`"></div>
                  <div class="pct-seg mid"   :style="`width:${pctNeutral}%`"  :title="`中评 ${pctNeutral}%`"></div>
                  <div class="pct-seg bad"   :style="`width:${pctNegative}%`" :title="`差评 ${pctNegative}%`"></div>
                </div>
                <div class="pct-legend">
                  <span class="pct-item good-text">
                    <i class="dot good"></i>好评 <b>{{ pctPositive }}%</b>
                  </span>
                  <span class="pct-item mid-text">
                    <i class="dot mid"></i>中评 <b>{{ pctNeutral }}%</b>
                  </span>
                  <span class="pct-item bad-text">
                    <i class="dot bad"></i>差评 <b>{{ pctNegative }}%</b>
                  </span>
                </div>
              </div>

              <div v-if="commentsPositive?.length" class="comment-section">
                <div class="section-label good">好评 <span class="pct-badge good-bg">{{ pctPositive }}%</span></div>
                <el-tag
                  v-for="(c, i) in commentsPositive.slice(0, 3)"
                  :key="'p'+i"
                  type="success"
                  size="large"
                  effect="plain"
                  class="comment-tag"
                >"{{ c }}"</el-tag>
              </div>
              <div v-if="commentsNeutral?.length" class="comment-section">
                <div class="section-label mid">中评 <span class="pct-badge mid-bg">{{ pctNeutral }}%</span></div>
                <el-tag
                  v-for="(c, i) in commentsNeutral.slice(0, 3)"
                  :key="'m'+i"
                  type="info"
                  size="large"
                  effect="plain"
                  class="comment-tag"
                >"{{ c }}"</el-tag>
              </div>
              <div v-if="commentsNegative?.length" class="comment-section">
                <div class="section-label bad">差评 <span class="pct-badge bad-bg">{{ pctNegative }}%</span></div>
                <el-tag
                  v-for="(c, i) in commentsNegative.slice(0, 3)"
                  :key="'n'+i"
                  type="danger"
                  size="large"
                  effect="plain"
                  class="comment-tag"
                >"{{ c }}"</el-tag>
              </div>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { teacherApi, adminSemesterApi } from '@/api/index'
import * as echarts from 'echarts'

const userStore        = useUserStore()
const radarRef         = ref()
const comments         = ref([])
const commentsPositive = ref([])
const commentsNeutral  = ref([])
const commentsNegative = ref([])
const questionAvg      = ref([])
const teacherInfo = ref({ realName: '', courses: [], classes: '' })
let   chartInst  = null

const radarTip = ref({ show: false, x: 0, y: 0, questionNo: '', pct: 0, avg: '0', max: 0, color: '#4f7ef8' })

function onRadarMouseMove(e) {
  if (!radarRef.value || !questionAvg.value.length) return
  const rect = radarRef.value.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const cx = rect.width  * 0.50
  const cy = rect.height * 0.58

  const mouseAngle = Math.atan2(-(my - cy), mx - cx)
  const n = questionAvg.value.length
  let nearest = 0, minDiff = Infinity
  for (let i = 0; i < n; i++) {
    // ECharts 雷达逆时针排列，Q1 在顶部
    let axisAngle = Math.PI / 2 + (i * 2 * Math.PI / n)
    while (axisAngle >  Math.PI) axisAngle -= 2 * Math.PI
    while (axisAngle < -Math.PI) axisAngle += 2 * Math.PI
    let diff = Math.abs(mouseAngle - axisAngle)
    if (diff > Math.PI) diff = 2 * Math.PI - diff
    if (diff < minDiff) { minDiff = diff; nearest = i }
  }

  const q = questionAvg.value[nearest]
  if (!q) return
  const max = Number(q.maxScore) || 10
  const avg = Number(q.avgScore) || 0
  const pct = Math.round(avg / max * 100)
  const no  = q.questionNo || (nearest + 1)
  const color = no <= 4 ? '#4f7ef8' : no <= 8 ? '#67c23a' : '#e6a23c'

  const tipW = 110
  const x = mx + 14 + tipW > rect.width ? mx - tipW - 6 : mx + 14
  radarTip.value = { show: true, x, y: my - 18, questionNo: no, pct, avg: avg.toFixed(2), max, color }
}

const DIMENSION_GROUPS = [
  { label: '一、教学内容与方法', shortLabel: '教学内容与方法', color: '#4f7ef8', from: 1, to: 4 },
  { label: '二、教学态度与互动', shortLabel: '教学态度与互动', color: '#67c23a', from: 5, to: 8 },
  { label: '三、教学效果与收获', shortLabel: '教学效果与收获', color: '#e6a23c', from: 9, to: 12 },
]

const questionGroups = computed(() =>
  DIMENSION_GROUPS.map(g => {
    const items = questionAvg.value.filter(q => q.questionNo >= g.from && q.questionNo <= g.to)
    const sumAvg = items.reduce((s, q) => s + (Number(q.avgScore) || 0), 0)
    const sumMax = items.reduce((s, q) => s + (Number(q.maxScore) || 10), 0)
    const pct    = sumMax > 0 ? Math.round(sumAvg / sumMax * 100) : 0
    return {
      ...g,
      items,
      avgRaw: sumAvg > 0 ? sumAvg.toFixed(1) : '—',
      maxRaw: sumMax,
      avgPct: pct,
    }
  }).filter(g => g.items.length > 0)
)

const cards = ref([
  { label: '综合平均分', value: '-', icon: 'Star',          color: '#4f7ef8' },
  { label: '评价人数',   value: '-', icon: 'UserFilled',     color: '#67c23a' },
  { label: '本学期',     value: '-', icon: 'Calendar',       color: '#e6a23c' },
])

const commentTotal = computed(() =>
  commentsPositive.value.length + commentsNeutral.value.length + commentsNegative.value.length
)
const pctPositive = computed(() => commentTotal.value ? Math.round(commentsPositive.value.length / commentTotal.value * 100) : 0)
const pctNeutral  = computed(() => commentTotal.value ? Math.round(commentsNeutral.value.length  / commentTotal.value * 100) : 0)
const pctNegative = computed(() => commentTotal.value ? Math.round(commentsNegative.value.length / commentTotal.value * 100) : 0)

onMounted(async () => {
  try {
    const [semRes, infoRes] = await Promise.all([teacherApi.semesters(), teacherApi.info()])
    if (infoRes?.data) teacherInfo.value = infoRes.data
    const semesters = semRes?.data || []
    // 优先当前学期，否则取最近授课学期（列表按时间升序，最后一项为最近）
    const displaySemester = semesters.find(s => s.isCurrent === 1) || semesters[semesters.length - 1]
    cards.value[2].value = displaySemester?.semesterName?.slice(0, 11) || '-'

    if (displaySemester) {
      const res = await teacherApi.evalStats(displaySemester.id)
      const d = res.data
      cards.value[0].value = d.avgScore ?? '-'
      cards.value[1].value = d.evalCount ?? 0
      comments.value        = d.comments        || []
      commentsPositive.value = d.commentsPositive || []
      commentsNeutral.value  = d.commentsNeutral  || []
      commentsNegative.value = d.commentsNegative || []
      questionAvg.value      = d.questionAvg      || []
      await nextTick()
      renderRadar(d.questionAvg || [])
    }
  } catch {}
})

function renderRadar(qavg) {
  if (!radarRef.value) return
  if (!chartInst) chartInst = echarts.init(radarRef.value)

  const indicators = qavg.map((q, i) => ({ name: `Q${q.questionNo || i + 1}`, max: Number(q.maxScore) || 10 }))
  const values     = qavg.map(q => Number(q.avgScore) || 0)

  chartInst.setOption({
    legend: { show: false },
    radar: {
      indicator: indicators,
      radius: '48%',
      center: ['50%', '58%'],
      axisName: { fontSize: 11, color: '#606266' },
    },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '平均分', areaStyle: { opacity: 0.25 } }],
      lineStyle: { color: '#4f7ef8', width: 1.5 },
      itemStyle:  { color: '#4f7ef8' },
      areaStyle:  { color: '#4f7ef8' },
      symbolSize: 6,
    }],
    tooltip: { show: false },
  })
}
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  border-radius: 16px;
  padding: 28px 32px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.welcome-banner h2 { font-size: 22px; margin-bottom: 6px; }
.welcome-banner p  { opacity: 0.85; font-size: 14px; }
.welcome-banner .teacher-intro { font-size: 13px; margin-top: 8px; opacity: 0.9; }
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.val { font-size: 30px; font-weight: 700; color: #303133; }
.lbl { font-size: 13px; color: #8c9ab0; }
.comments { padding: 8px 0; }
.comment-section { margin-bottom: 12px; }
.comment-section:last-child { margin-bottom: 0; }
.section-label { font-size: 12px; font-weight: 600; margin-bottom: 6px; }
.section-label.good { color: #67c23a; }
.section-label.mid  { color: #909399; }
.section-label.bad  { color: #f56c6c; }
.comment-tag { margin: 4px 4px 4px 0; white-space: normal; height: auto; line-height: 1.6; padding: 6px 10px; display: inline-block; }
.empty { color: #c0c4cc; font-size: 13px; padding: 20px 0; text-align: center; width: 100%; }

/* 占比统计条 */
.pct-bar-wrap { margin-bottom: 14px; }
.pct-bar {
  display: flex; height: 8px; border-radius: 4px; overflow: hidden;
  background: #f0f2f5; margin-bottom: 8px;
}
.pct-seg { height: 100%; transition: width 0.4s; }
.pct-seg.good { background: #67c23a; }
.pct-seg.mid  { background: #909399; }
.pct-seg.bad  { background: #f56c6c; }
.pct-legend { display: flex; gap: 16px; }
.pct-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #606266; }
.pct-item b { font-size: 13px; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot.good { background: #67c23a; }
.dot.mid  { background: #909399; }
.dot.bad  { background: #f56c6c; }
.good-text b { color: #67c23a; }
.mid-text  b { color: #909399; }
.bad-text  b { color: #f56c6c; }

/* 雷达图 + 维度得分并排 */
.radar-with-scores {
  display: flex; align-items: stretch; gap: 12px;
}
.radar-chart-wrap { flex: 1; min-width: 0; position: relative; }

/* 自定义 radar tooltip */
.radar-tip {
  position: absolute; pointer-events: none; z-index: 99;
  background: white; border: 1px solid #e4e7ed; border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  padding: 8px 12px; min-width: 100px;
}
.rt-title { font-size: 13px; font-weight: 700; color: #303133; }
.rt-pct   { font-size: 22px; font-weight: 700; line-height: 1.3; }
.rt-sub   { font-size: 11px; color: #909399; margin-top: 2px; }
.dim-scores {
  width: 136px; flex-shrink: 0;
  display: flex; flex-direction: column; justify-content: space-between; gap: 8px;
}
.dim-card {
  flex: 1; border-left: 4px solid #4f7ef8;
  background: #fafbfc; border-radius: 8px;
  padding: 10px 12px;
  display: flex; flex-direction: column; gap: 4px;
}
.dim-label { font-size: 12px; font-weight: 600; color: #303133; line-height: 1.3; }
.dim-q-range { font-size: 11px; font-weight: 600; opacity: 0.75; margin-top: 1px; }
.dim-score-row { display: flex; align-items: baseline; gap: 4px; }
.dim-score-val { font-size: 22px; font-weight: 700; line-height: 1; }
.dim-score-sub { font-size: 11px; color: #909399; }
.dim-score-detail { font-size: 11px; color: #909399; margin-top: 2px; }
.dim-score-detail b { font-size: 12px; }

/* 分类标签旁的百分比徽章 */
.pct-badge {
  display: inline-block; font-size: 11px; font-weight: 600;
  padding: 1px 6px; border-radius: 8px; margin-left: 6px; color: white;
}
.good-bg { background: #67c23a; }
.mid-bg  { background: #909399; }
.bad-bg  { background: #f56c6c; }
</style>
