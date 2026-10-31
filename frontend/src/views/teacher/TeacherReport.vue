<template>
  <div class="page">
    <div v-if="report" v-loading="loading">
      <div class="report-toolbar">
        <el-select v-model="selectedSemesterId" placeholder="选择学期" clearable style="width:220px" @change="loadReport">
          <el-option
            v-for="s in semesterList"
            :key="s.id"
            :label="s.semesterName"
            :value="s.id"
          />
        </el-select>
        <span class="semester-text" v-if="currentSemesterName">{{ currentSemesterName }}</span>
        <el-button type="primary" size="small" :loading="loading" @click="loadReport">
          <el-icon><Refresh /></el-icon> 刷新报告
        </el-button>
      </div>
      <!-- 报告头部 -->
      <div class="report-header">
        <div class="report-badge">教学能力提升报告</div>
        <div class="teacher-info">
          <h2>{{ report.teacherName }} <span class="title-badge">{{ report.title }}</span></h2>
          <p>{{ report.department }}</p>
          <p v-if="report.courses?.length || report.classes" class="teacher-intro">
            所教科目：{{ report.courses?.join('、') || '—' }}；所教班级：{{ report.classes || '—' }}
          </p>
        </div>
        <div class="score-overview">
          <div class="score-big" :class="scoreClass">{{ formatScore(report.avgScore) }}</div>
          <div class="score-labels">
            <div>综合均分</div>
            <el-tag :type="levelTagType" size="large">{{ report.overallLevel }}</el-tag>
          </div>
        </div>
        <div class="rank-info">
          <div class="rank-num">第 <span>{{ report.rank }}</span> / {{ report.totalTeachers }}</div>
          <div>全体教师排名</div>
          <div class="compare">全体均分：{{ formatScore(report.allAvgScore) }}</div>
        </div>
      </div>

      <el-row :gutter="16" style="margin-top:16px">
        <!-- 左侧：优势与短板 -->
        <el-col :span="10">
          <!-- 优势 -->
          <el-card shadow="never" style="margin-bottom:16px">
            <template #header>
              <span class="section-head success">
                <el-icon><Trophy /></el-icon> 教学优势
              </span>
            </template>
            <div v-if="report.strengths?.length">
              <div v-for="(s, i) in report.strengths" :key="i" class="strength-item">
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
                <span>{{ s }}</span>
              </div>
            </div>
            <el-empty v-else description="暂未检测到显著优势，继续努力！" :image-size="60" />
          </el-card>

          <!-- 待提升维度 -->
          <el-card shadow="never">
            <template #header>
              <span class="section-head warning">
                <el-icon><Warning /></el-icon> 待提升维度
              </span>
            </template>
            <div v-if="report.weaknesses?.length">
              <div v-for="w in report.weaknesses" :key="w.dimension" class="weakness-item">
                <div class="w-header">
                  <span class="w-name">{{ w.dimension }}</span>
                  <el-tag :type="severityType(w.severity)" size="small">{{ w.severity }}</el-tag>
                </div>
                <el-progress
                  :percentage="w.scoreRatio"
                  :color="w.scoreRatio < 50 ? '#f56c6c' : w.scoreRatio < 65 ? '#e6a23c' : w.scoreRatio < 75 ? '#409eff' : '#67c23a'"
                  :stroke-width="8"
                />
                <div class="w-detail">
                  当前 <b>{{ w.scoreRatio }}%</b>，目标 <b>{{ w.threshold }}%</b>，
                  差距 <b :style="{ color: w.severity === '提升空间' ? '#e6a23c' : '#f56c6c' }">{{ w.gap }}%</b>
                </div>
              </div>
            </div>
            <el-empty v-else description="各维度表现良好，暂无明显短板" :image-size="60" />
          </el-card>
        </el-col>

        <!-- 右侧：雷达图 + 题目得分 -->
        <el-col :span="14">
          <el-card shadow="never" style="margin-bottom:16px">
            <template #header><span class="section-head">各维度得分雷达图</span></template>
            <div ref="radarRef" style="height:280px"></div>
          </el-card>

          <el-card shadow="never">
            <template #header><span class="section-head">各题平均得分</span></template>
            <el-table v-if="report.questionAvg?.length" :data="report.questionAvg" border stripe size="small" max-height="240">
              <el-table-column prop="questionNo"      label="题号"  width="55" align="center" />
              <el-table-column prop="questionContent" label="题目" show-overflow-tooltip />
              <el-table-column label="得分率" width="120" align="center">
                <template #default="{ row }">
                  <el-progress
                    :percentage="Math.round((Number(row.avgScore) || 0) / (Number(row.maxScore) || 1) * 100)"
                    :stroke-width="8"
                    :color="pColor(Number(row.avgScore) || 0, Number(row.maxScore) || 1)"
                  />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 知识图谱推荐 -->
      <el-card shadow="never" style="margin-top:16px" v-if="report.graphRecommendations?.length">
        <template #header>
          <span class="section-head primary">
            <el-icon><Share /></el-icon> 知识图谱推荐 · 个性化提升路径
          </span>
        </template>
        <el-row :gutter="16">
          <el-col :span="8" v-for="rec in report.graphRecommendations" :key="rec.problem">
            <div class="graph-card" :class="rec.severity">
              <div class="graph-title">
                <el-tag :type="severityType(rec.severity)" size="small" style="margin-right:8px">{{ rec.severity }}</el-tag>
                {{ rec.problem }}（{{ rec.scoreRatio }}%）
              </div>

              <!-- 图谱路径展示 -->
              <div class="graph-path">
                <div v-for="(step, i) in rec.graphPath" :key="i" class="path-step">
                  <div class="path-node">{{ step }}</div>
                  <el-icon v-if="i < rec.graphPath.length - 1" class="path-arrow"><ArrowRight /></el-icon>
                </div>
              </div>

              <div class="strategy-list">
                <div class="sl-title">改进策略</div>
                <div v-for="(s, i) in rec.strategies" :key="i" class="strategy-item">
                  <el-icon color="#409eff"><Right /></el-icon>
                  <span>{{ s }}</span>
                </div>
              </div>

              <div class="resource-list">
                <div class="sl-title">推荐资源</div>
                <div v-for="(r, i) in rec.resources" :key="i" class="resource-item">
                  <el-tag size="small" :type="resTagType(r.type)" style="margin-right:6px">{{ r.type }}</el-tag>
                  <a
                    v-if="r.url && r.url !== '#'"
                    :href="r.url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >{{ r.title }}</a>
                  <span v-else class="res-no-link">{{ r.title }}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

    </div>

    <el-empty v-else-if="!loading" description="暂无报告数据" :image-size="100" />
    <div v-else v-loading="true" style="height:300px" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { teacherApi, commonApi } from '@/api/index'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const currentSemesterName = ref('')
const selectedSemesterId  = ref(null)
const semesterList        = ref([])
const report              = ref(null)
const loading         = ref(false)
const radarRef        = ref()
let   radarInst       = null

const scoreClass = computed(() => {
  const v = Number(report.value?.avgScore ?? 0)
  if (v >= 90) return 'excellent'
  if (v >= 80) return 'good'
  if (v >= 70) return 'average'
  return 'poor'
})

const levelTagType = computed(() => {
  const lv = report.value?.overallLevel
  if (lv === '优秀') return 'success'
  if (lv === '良好') return 'primary'
  if (lv === '中等') return 'warning'
  return 'danger'
})

async function loadReport() {
  loading.value = true
  report.value = null
  try {
    const sid = selectedSemesterId.value
    const res = await teacherApi.report(sid || undefined)
    report.value = res.data
    if (report.value?.semesterId) {
      const s = semesterList.value.find(x => x.id === report.value.semesterId)
      currentSemesterName.value = s?.semesterName || '—'
    }
    await nextTick()
    renderRadar()
  } catch (e) {
    ElMessage.error('加载报告失败：' + (e?.message || '请稍后重试'))
  } finally {
    loading.value = false
  }
}

function renderRadar() {
  if (!radarRef.value) return
  if (!radarInst) radarInst = echarts.init(radarRef.value)
  const dims = report.value?.dimensionScores || {}
  const keys = Object.keys(dims)
  if (keys.length === 0) {
    radarInst.setOption({
      title: { text: '暂无维度数据', left: 'center', top: 'middle', textStyle: { color: '#909399', fontSize: 14 } },
    })
    return
  }
  const indicators = keys.map(k => ({ name: k, max: 100 }))
  const values = keys.map(k => Math.round((dims[k] || 0) * 100))

  radarInst.setOption({
    tooltip: { trigger: 'item' },
    radar: { indicator: indicators, radius: '65%', splitNumber: 4 },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '得分率(%)' }],
      areaStyle: { opacity: 0.3, color: '#4f7ef8' },
      lineStyle: { color: '#4f7ef8', width: 2 },
      itemStyle: { color: '#4f7ef8' },
      label: { show: true, formatter: '{c}%', fontSize: 11 },
    }],
  })
}

const formatScore = (v) => v != null ? (Number(v) === Math.floor(Number(v)) ? v : Number(v).toFixed(1)) : '—'
const severityType = (s) => s === '严重' ? 'danger' : s === '中等' ? 'warning' : s === '轻微' ? 'info' : 'success'
const resTagType   = (t) => {
  if (t === 'B站')   return 'danger'
  if (t === '雨课堂') return 'primary'
  if (t === 'MOOC')  return 'success'
  if (t === '学习通') return 'warning'
  if (t === '文献')  return 'info'
  return 'info'
}
const pColor = (s, max) => {
  const r = s / max * 100
  if (r >= 85) return '#67c23a'
  if (r >= 70) return '#409eff'
  if (r >= 55) return '#e6a23c'
  return '#f56c6c'
}

onMounted(async () => {
  try {
    let list = []
    try {
      const semRes = await teacherApi.semesters()
      list = semRes.data || []
    } catch (_) {
      list = []
    }
    if (list.length === 0) {
      const commonRes = await commonApi.semesters()
      list = commonRes.data || []
    }
    semesterList.value = list
    const cur = list.find(s => s.isCurrent === 1) || list[list.length - 1]
    selectedSemesterId.value = cur?.id ?? null
    currentSemesterName.value = cur?.semesterName || '—'
  } catch (e) {
    ElMessage.error('加载学期列表失败')
  }
  await loadReport()
})
</script>

<style scoped>
.report-toolbar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}
.semester-text { font-size: 14px; color: #606266; }
.section-head { font-size: 15px; font-weight: 600; color: #303133; display: flex; align-items: center; gap: 6px; }
.section-head.success { color: #67c23a; }
.section-head.warning { color: #e6a23c; }
.section-head.primary { color: #4f7ef8; }

.report-header {
  display: flex; align-items: center; gap: 24px;
  background: linear-gradient(135deg, #1a2a6c 0%, #2d6a9f 100%);
  border-radius: 16px; padding: 24px 32px; color: white;
}
.report-badge {
  background: rgba(255,255,255,.15); border-radius: 8px;
  padding: 4px 12px; font-size: 12px; letter-spacing: 1px; white-space: nowrap;
}
.teacher-info { flex: 1; }
.teacher-info h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; display: flex; align-items: center; gap: 8px; }
.teacher-info p { opacity: 0.8; font-size: 14px; margin: 0; }
.teacher-info .teacher-intro { margin-top: 6px; font-size: 13px; }
.title-badge { background: rgba(255,255,255,.2); border-radius: 6px; padding: 2px 8px; font-size: 13px; font-weight: 400; }
.score-overview { display: flex; align-items: center; gap: 12px; }
.score-big {
  width: 80px; height: 80px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: white; border: 3px solid rgba(255,255,255,.4);
}
.score-big.excellent { background: #67c23a; }
.score-big.good      { background: #409eff; }
.score-big.average   { background: #e6a23c; }
.score-big.poor      { background: #f56c6c; }
.score-labels { text-align: center; font-size: 13px; }
.rank-info { text-align: center; font-size: 13px; opacity: .9; }
.rank-num { font-size: 22px; font-weight: 700; }
.rank-num span { font-size: 32px; color: #ffd700; }
.compare { opacity: .7; font-size: 12px; margin-top: 4px; }

.strength-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 14px; color: #606266; border-bottom: 1px solid #f5f5f5; }
.weakness-item { padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.w-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.w-name { font-weight: 600; font-size: 14px; }
.w-detail { font-size: 12px; color: #909399; margin-top: 4px; }

.graph-card {
  border: 1px solid #ebeef5; border-radius: 12px; padding: 16px;
  height: 100%; transition: box-shadow .2s;
}
.graph-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.1); }
.graph-card.严重 { border-color: #fde2e2; background: #fff9f9; }
.graph-card.中等 { border-color: #fdf6ec; background: #fffdf8; }
.graph-card.轻微 { border-color: #e8f4ff; background: #f8fbff; }
.graph-title { font-weight: 600; font-size: 14px; margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.graph-path { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
.path-step { display: flex; align-items: center; gap: 4px; }
.path-node { background: #f0f5ff; border-radius: 6px; padding: 2px 8px; font-size: 12px; color: #4f7ef8; border: 1px solid #c6d5ff; }
.path-arrow { color: #bbbfc8; font-size: 12px; }
.strategy-list { margin-bottom: 10px; }
.resource-list {}
.sl-title { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 1px; }
.strategy-item { display: flex; align-items: flex-start; gap: 4px; font-size: 13px; color: #606266; padding: 2px 0; }
.resource-item { display: flex; align-items: center; font-size: 12px; padding: 3px 0; }
.resource-item a { color: #409eff; text-decoration: none; }
.resource-item a:hover { text-decoration: underline; }
.res-no-link { color: #909399; cursor: default; }

</style>
