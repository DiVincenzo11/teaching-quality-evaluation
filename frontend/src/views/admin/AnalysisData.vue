<template>
  <div class="page">
    <!-- 筛选栏 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <el-form :inline="true">
        <el-form-item label="选择学期">
          <el-select v-model="selectedSemester" placeholder="全部学期" clearable style="width:220px" @change="onSemesterChange">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSemesterChange"><el-icon><Search /></el-icon> 查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ===== Tab 1：教师排行 ===== -->
      <el-tab-pane label="综合排行" name="ranking">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header><span class="title">教师评价综合排行</span></template>
              <el-table :data="teacherStats" v-loading="loading" stripe border height="420">
                <el-table-column type="index" label="排名" width="60" align="center" />
                <el-table-column prop="realName"       label="姓名"   width="90" />
                <el-table-column prop="department"     label="院系"   show-overflow-tooltip />
                <el-table-column prop="title"          label="职称"   width="90" />
                <el-table-column prop="evalCount"      label="有效人数" width="90" align="center" />
                <el-table-column prop="excludedCount"  label="剔除人数" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.excludedCount > 0" type="danger" size="small">{{ row.excludedCount }}</el-tag>
                    <span v-else style="color:#909399">0</span>
                  </template>
                </el-table-column>
                <el-table-column prop="avgChoiceScore" label="单选均分" width="90" align="center" />
                <el-table-column prop="avgEssayScore"  label="简答均分" width="90" align="center" />
                <el-table-column prop="avgScore"       label="综合均分" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="scoreTagType(row.avgScore)" effect="light">{{ row.avgScore ?? '-' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" align="center">
                  <template #default="{ row }">
                    <el-button text type="primary" size="small" @click="showDetail(row)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header><span class="title">综合评分分布</span></template>
              <div ref="chartRef" style="height:440px"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ===== Tab 2：动态权重 ===== -->
      <el-tab-pane label="动态权重（熵权法）" name="weights">
        <el-card shadow="never">
          <template #header>
            <div class="header-flex">
              <span class="title">评价指标动态权重分析</span>
              <div class="header-controls">
                <el-select v-model="courseType" placeholder="课程类型" clearable style="width:130px" @change="loadWeights">
                  <el-option label="通用" :value="null" />
                  <el-option label="理论课" :value="1" />
                  <el-option label="实验课" :value="2" />
                </el-select>
                <el-button type="primary" size="small" @click="loadWeights">计算</el-button>
              </div>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              基于信息熵方法：指标得分差异越大（熵值越小），该指标区分度越高，权重越大。
              可按课程类型（理论课/实验课）进一步调整各维度权重系数。
            </template>
          </el-alert>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-table :data="weights" v-loading="weightsLoading" border stripe>
                <el-table-column prop="questionNo"      label="题号" width="60" align="center" />
                <el-table-column prop="questionContent" label="题目" show-overflow-tooltip />
                <el-table-column prop="entropy"         label="信息熵" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag type="info" size="small">{{ row.entropy }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="rawWeight"       label="基础权重%" width="100" align="center" />
                <el-table-column label="调整后权重%" width="130" align="center">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="row.adjustedWeight"
                      :color="wColor(row.adjustedWeight)"
                      :stroke-width="10"
                      :format="(p) => p + '%'"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :span="10">
              <div ref="weightChartRef" style="height:460px"></div>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab 3：教师画像聚类 ===== -->
      <el-tab-pane label="教师画像聚类（K-Means）" name="cluster">
        <el-card shadow="never">
          <template #header>
            <div class="header-flex">
              <span class="title">教师教学风格聚类分析</span>
              <el-button type="primary" size="small" @click="loadClusters">重新聚类</el-button>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              采用 K-Means++ 算法对教师进行聚类（K=3）：
              <b>严谨学术型</b>（教学内容严谨，单选得分高）、
              <b>风趣互动型</b>（学生评语突出，互动性强）、
              <b>均衡发展型</b>（各维度表现均衡稳定）
            </template>
          </el-alert>

          <div v-if="clusterData" v-loading="clusterLoading">
            <!-- 聚类分布概览 -->
            <el-row :gutter="16" style="margin-bottom:16px">
              <el-col :span="8" v-for="s in clusterData.clusterSummary" :key="s.cluster">
                <div class="cluster-card" :style="{ borderColor: s.color, background: hexToRgba(s.color, 0.06) }">
                  <div class="cluster-label" :style="{ color: s.color }">{{ s.label }}</div>
                  <div class="cluster-count">{{ s.count }} 位教师</div>
                  <div class="cluster-desc">{{ s.description }}</div>
                </div>
              </el-col>
            </el-row>

            <!-- 聚类散点图 -->
            <el-row :gutter="16">
              <el-col :span="12">
                <div ref="clusterChartRef" style="height:340px"></div>
              </el-col>
              <el-col :span="12">
                <el-table :data="clusterData.teachers" border stripe height="340">
                  <el-table-column prop="realName"   label="姓名"   width="80" />
                  <el-table-column prop="department" label="院系"   show-overflow-tooltip />
                  <el-table-column prop="avgScore"   label="均分"   width="70" align="center" />
                  <el-table-column label="类型" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag :color="row.clusterColor" style="color:#fff;border:none" size="small">
                        {{ row.clusterLabel }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else-if="!clusterLoading" description="点击「重新聚类」开始分析" :image-size="80" />
          <div v-else v-loading="true" style="height:200px" />
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab 4：参与度分析与异常剔除 ===== -->
      <el-tab-pane label="参与度异常分析" name="engagement">
        <el-card shadow="never">
          <template #header>
            <div class="header-flex">
              <span class="title">学生参与度分析与评价权重管理</span>
              <el-button type="danger" :loading="recalcLoading" @click="doRecalculate">
                <el-icon><Refresh /></el-icon> 重新计算权重 &amp; 剔除异常
              </el-button>
            </div>
          </template>

          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              系统根据学生填写的<b>前置问题</b>（出勤率 + 课外投入）计算<b>参与度权重</b>（0.50 ~ 1.00）。
              执行「重新计算」后：出勤率低且课外投入少且打分明显偏低（z-score &lt; -1.5）的数据将被<b>剔除</b>；
              参与度偏低但偏差较小的数据将进行<b>权重降级</b>（×0.8）；综合排行均采用加权平均。
            </template>
          </el-alert>

          <!-- 算法说明卡片 -->
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="12">
              <div class="algo-card">
                <div class="algo-title">参与度权重映射</div>
                <el-table :data="weightMapData" border stripe size="small">
                  <el-table-column prop="level" label="选项" width="110" />
                  <el-table-column prop="attend" label="出勤率权重" align="center" />
                  <el-table-column prop="study"  label="课外投入权重" align="center" />
                </el-table>
                <div style="font-size:12px;color:#909399;margin-top:8px">
                  参与度权重 = (出勤率权重 + 课外投入权重) ÷ 2
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="algo-card">
                <div class="algo-title">异常剔除规则（Z-Score 法）</div>
                <el-table :data="ruleData" border stripe size="small">
                  <el-table-column prop="condition" label="触发条件" show-overflow-tooltip />
                  <el-table-column prop="action" label="处理方式" width="110" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.type" size="small">{{ row.action }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <div style="font-size:12px;color:#909399;margin-top:8px">
                  z = (该生评分 − 同组均值) ÷ 标准差；按教师+学期分组计算
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 概览统计 -->
          <div v-if="engagementOverview" class="engagement-overview">
            <el-row :gutter="16">
              <el-col :span="4" v-for="item in overviewCards" :key="item.label">
                <div class="eng-stat-card" :class="item.cls">
                  <div class="eng-num">{{ item.value }}</div>
                  <div class="eng-label">{{ item.label }}</div>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="16" style="margin-top:20px">
              <el-col :span="12">
                <div class="algo-card">
                  <div class="algo-title">出勤率分布</div>
                  <div ref="attendChartRef" style="height:220px"></div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="algo-card">
                  <div class="algo-title">课外投入分布</div>
                  <div ref="studyChartRef" style="height:220px"></div>
                </div>
              </el-col>
            </el-row>
          </div>
          <el-empty v-else-if="!engagementLoading" description="暂无参与度数据（需有学生提交含前置问题的评价）" :image-size="80" />
          <div v-else v-loading="true" style="height:160px" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 教师详情弹窗（教学质量分析报告） -->
    <el-dialog v-model="detailVisible" :title="`${detailTeacher?.realName || detail?.teacherName} - 教学质量分析报告`" width="780px">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="综合均分">
            <el-tag :type="scoreTagType(detail.avgScore)" size="large">{{ detail.avgScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评价人数">{{ detail.evalCount }} 人</el-descriptions-item>
          <el-descriptions-item label="排名">
            <el-tag type="primary">第 {{ detail.rank }}/{{ detail.totalTeachers }} 名</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="全院均分">{{ detail.allAvgScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="综合等级">
            <el-tag :type="detail.overallLevel === '优秀' ? 'success' : detail.overallLevel === '良好' ? 'primary' : 'info'">
              {{ detail.overallLevel ?? '-' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 优势与短板 -->
        <template v-if="detail.strengths?.length || detail.weaknesses?.length">
          <div class="section-title" style="margin-top:16px">维度分析</div>
          <el-row :gutter="16">
            <el-col :span="12" v-if="detail.strengths?.length">
              <div class="dim-label" style="color:#67c23a">优势维度</div>
              <el-tag v-for="s in detail.strengths" :key="s" type="success" size="small" style="margin:2px">{{ s }}</el-tag>
            </el-col>
            <el-col :span="12" v-if="detail.weaknesses?.length">
              <div class="dim-label" style="color:#e6a23c">待改进维度</div>
              <el-tag v-for="w in detail.weaknesses" :key="w.dimension" type="warning" size="small" style="margin:2px">
                {{ w.dimension }} (得分率{{ w.scoreRatio }}%)
              </el-tag>
            </el-col>
          </el-row>
        </template>

        <div class="section-title">各题平均分</div>
        <el-table :data="detail.questionAvg" border stripe size="small">
          <el-table-column prop="questionNo"      label="题号"  width="60" align="center" />
          <el-table-column prop="questionContent" label="题目"  show-overflow-tooltip />
          <el-table-column prop="avgScore"        label="平均分" width="90" align="center" />
          <el-table-column prop="maxScore"        label="满分"   width="80" align="center" />
          <el-table-column label="得分率" width="100" align="center">
            <template #default="{ row }">
              <el-progress :percentage="Math.round(row.avgScore / row.maxScore * 100)" :stroke-width="10" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 改进建议（知识图谱推荐） -->
        <template v-if="detail.graphRecommendations?.length">
          <div class="section-title">改进建议与资源推荐</div>
          <el-collapse>
            <el-collapse-item v-for="(rec, i) in detail.graphRecommendations" :key="i" :name="i">
              <template #title>
                <span>{{ rec.problem }}</span>
                <el-tag :type="rec.severity === '严重' ? 'danger' : rec.severity === '中等' ? 'warning' : 'info'" size="small" style="margin-left:8px">
                  {{ rec.severity }} (得分率{{ rec.scoreRatio }}%)
                </el-tag>
              </template>
              <div v-if="rec.strategies?.length">
                <div class="dim-label" style="margin-bottom:4px">改进策略：</div>
                <ul style="margin:0 0 8px 0; padding-left:18px; font-size:13px">
                  <li v-for="s in rec.strategies" :key="s">{{ s }}</li>
                </ul>
              </div>
            </el-collapse-item>
          </el-collapse>
        </template>

        <div class="section-title">学生评语（匿名）</div>
        <div class="comments-list">
          <div v-if="!detail.comments?.length" class="no-comment">暂无评语</div>
          <el-tag
            v-for="(c, i) in detail.comments"
            :key="i" type="info" size="large"
            style="margin:4px;max-width:100%;white-space:normal;height:auto;line-height:1.5;padding:6px 10px"
          >{{ c }}</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { adminAnalysisApi, adminSemesterApi } from '@/api/index'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const semesters        = ref([])
const selectedSemester = ref(null)
const teacherStats     = ref([])
const loading          = ref(false)
const chartRef         = ref()
const detailVisible    = ref(false)
const detailLoading    = ref(false)
const detail           = ref(null)
const detailTeacher    = ref(null)
let   chartInstance    = null

const activeTab        = ref('ranking')
const courseType       = ref(null)

// 动态权重
const weights          = ref([])
const weightsLoading   = ref(false)
const weightChartRef   = ref()
let   weightChart      = null

// 聚类
const clusterData      = ref(null)
const clusterLoading   = ref(false)
const clusterChartRef  = ref()
let   clusterChart     = null

// 参与度分析
const engagementOverview = ref(null)
const engagementLoading  = ref(false)
const recalcLoading      = ref(false)
const attendChartRef     = ref()
const studyChartRef      = ref()
let   attendChart        = null
let   studyChart         = null

const ATTEND_LABELS = ['', '低于50%', '50~70%', '70~85%', '85~95%', '95%以上']
const STUDY_LABELS  = ['', '几乎不投入', '每周<1h', '每周1~3h', '每周3~5h', '每周>5h']

const weightMapData = [
  { level: '1（最低）', attend: '0.50', study: '0.60' },
  { level: '2',         attend: '0.70', study: '0.75' },
  { level: '3',         attend: '0.85', study: '0.90' },
  { level: '4',         attend: '0.95', study: '0.95' },
  { level: '5（最高）', attend: '1.00', study: '1.00' },
]

const ruleData = [
  { condition: '参与度权重 < 0.65 且 z-score < -1.5（低参与+极端低分）', action: '剔除',   type: 'danger'  },
  { condition: '参与度权重 < 0.75 且 z-score < -1.0（低参与+偏低分）',   action: '权重×0.8', type: 'warning' },
  { condition: '其他正常情况',                                             action: '保留原权重', type: 'success' },
]

const overviewCards = computed(() => {
  const o = engagementOverview.value
  if (!o) return []
  return [
    { label: '总评价数',   value: o.total,         cls: '' },
    { label: '有效评价',   value: o.total - o.excluded, cls: 'green' },
    { label: '已剔除',     value: o.excluded,      cls: 'red' },
    { label: '低出勤率',   value: o.lowAttendance, cls: 'orange' },
    { label: '低课外投入', value: o.lowStudy,      cls: 'orange' },
    { label: '双低记录',   value: o.bothLow,       cls: 'red' },
  ]
})

const scoreTagType = (s) => {
  const v = Number(s)
  if (v >= 90) return 'success'
  if (v >= 75) return 'primary'
  if (v >= 60) return 'warning'
  return 'danger'
}
const absaColor = (v) => {
  if (v >= 80) return '#67c23a'
  if (v >= 65) return '#409eff'
  if (v >= 50) return '#e6a23c'
  return '#f56c6c'
}
const wColor = (v) => {
  if (v >= 15) return '#f56c6c'
  if (v >= 10) return '#e6a23c'
  return '#409eff'
}

function hexToRgba(hex, a) {
  const r = parseInt(hex.slice(1,3),16)
  const g = parseInt(hex.slice(3,5),16)
  const b = parseInt(hex.slice(5,7),16)
  return `rgba(${r},${g},${b},${a})`
}

async function onSemesterChange() {
  await loadStats()
  if (activeTab.value === 'weights')    await loadWeights()
  if (activeTab.value === 'cluster')    await loadClusters()
  if (activeTab.value === 'engagement') await loadEngagement()
}

async function loadEngagement() {
  engagementLoading.value = true
  try {
    const res = await adminAnalysisApi.engagementStats(selectedSemester.value)
    const data = res.data?.[0]
    engagementOverview.value = data || null
    if (data) {
      await nextTick()
      renderAttendChart(data.attendanceDist || {})
      renderStudyChart(data.studyDist || {})
    }
  } finally { engagementLoading.value = false }
}

async function doRecalculate() {
  recalcLoading.value = true
  try {
    const res = await adminAnalysisApi.recalculateWeights(selectedSemester.value)
    const d = res.data
    ElMessage.success(`重新计算完成：共 ${d.totalRecords} 条，剔除 ${d.excluded} 条，降级 ${d.downgraded} 条，正常 ${d.normal} 条`)
    await loadStats()
    await loadEngagement()
  } finally { recalcLoading.value = false }
}

function renderAttendChart(dist) {
  if (!attendChartRef.value) return
  if (!attendChart) attendChart = echarts.init(attendChartRef.value)
  const labels = ATTEND_LABELS.slice(1)
  const values = [1,2,3,4,5].map(i => dist[i] || 0)
  attendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis:   { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis:   { type: 'value', name: '人数' },
    series:  [{ type: 'bar', data: values, itemStyle: { borderRadius: [4,4,0,0], color: '#5470c6' },
                label: { show: true, position: 'top' } }],
    grid:    { left: '8%', right: '4%', bottom: '14%', top: '14%' },
  })
}

function renderStudyChart(dist) {
  if (!studyChartRef.value) return
  if (!studyChart) studyChart = echarts.init(studyChartRef.value)
  const labels = STUDY_LABELS.slice(1)
  const values = [1,2,3,4,5].map(i => dist[i] || 0)
  studyChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis:   { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis:   { type: 'value', name: '人数' },
    series:  [{ type: 'bar', data: values, itemStyle: { borderRadius: [4,4,0,0], color: '#91cc75' },
                label: { show: true, position: 'top' } }],
    grid:    { left: '8%', right: '4%', bottom: '14%', top: '14%' },
  })
}

async function loadStats() {
  loading.value = true
  try {
    const res = await adminAnalysisApi.teacherStats(selectedSemester.value)
    teacherStats.value = res.data || []
    await nextTick()
    renderChart()
  } finally { loading.value = false }
}

async function loadWeights() {
  weightsLoading.value = true
  try {
    const res = await adminAnalysisApi.dynamicWeights(selectedSemester.value, courseType.value)
    weights.value = res.data || []
    await nextTick()
    renderWeightChart()
  } finally { weightsLoading.value = false }
}

async function loadClusters() {
  clusterLoading.value = true
  try {
    const res = await adminAnalysisApi.teacherClusters(selectedSemester.value)
    clusterData.value = res.data
    await nextTick()
    renderClusterChart()
  } finally { clusterLoading.value = false }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const scores = teacherStats.value.map(t => Number(t.avgScore) || 0)
  const labels = teacherStats.value.map(t => t.realName)
  chartInstance.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c} 分' },
    xAxis:   { type: 'value', max: 100, name: '分数' },
    yAxis:   { type: 'category', data: labels.slice().reverse(), axisLabel: { width: 60, overflow: 'truncate' } },
    series:  [{
      type: 'bar',
      data: scores.slice().reverse(),
      label: { show: true, position: 'right', formatter: '{c}' },
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: (params) => {
          const v = params.data
          if (v >= 90) return '#67c23a'
          if (v >= 75) return '#409eff'
          if (v >= 60) return '#e6a23c'
          return '#f56c6c'
        }
      }
    }],
    grid: { left: '2%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
  })
}

function renderWeightChart() {
  if (!weightChartRef.value || !weights.value.length) return
  if (!weightChart) weightChart = echarts.init(weightChartRef.value)
  const fullNameMap = {}
  const pieData = weights.value.map(w => {
    const shortName = `Q${w.questionNo}`
    fullNameMap[shortName] = w.questionContent || shortName
    return { name: shortName, value: w.adjustedWeight }
  })
  weightChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const full = fullNameMap[params.name] || params.name
        return `${params.name}<br/>${full}<br/>权重：${params.value}%（占比 ${params.percent}%）`
      },
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 0,
      left: 'center',
      textStyle: { fontSize: 11 },
      formatter: (name) => name,
    },
    series: [{
      type: 'pie',
      radius: ['38%', '62%'],
      center: ['50%', '44%'],
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 12,
          formatter: (params) => `${params.name}\n${params.percent}%`,
        },
      },
      data: pieData,
    }],
    color: ['#5470c6','#91cc75','#fac858','#ee6666','#73c0de','#3ba272','#fc8452','#9a60b4','#ea7ccc','#4ec9b0','#d4a017','#c084fc'],
  })
}

function renderClusterChart() {
  if (!clusterChartRef.value || !clusterData.value?.teachers?.length) return
  if (!clusterChart) clusterChart = echarts.init(clusterChartRef.value)

  const colors = { '严谨学术型': '#5470c6', '风趣互动型': '#91cc75', '均衡发展型': '#fac858' }
  const series = ['严谨学术型', '风趣互动型', '均衡发展型'].map(label => ({
    name: label,
    type: 'scatter',
    symbolSize: 14,
    data: clusterData.value.teachers
      .filter(t => t.clusterLabel === label)
      .map(t => [
        Number(t.avgChoiceScore) || 0,
        Number(t.avgEssayScore) || 0,
        t.realName,
      ]),
    label: { show: true, formatter: (p) => p.data[2], position: 'top', fontSize: 11 },
    itemStyle: { color: colors[label] },
  }))

  clusterChart.setOption({
    tooltip: {
      formatter: (p) => `${p.data[2]}<br>单选均分: ${p.data[0]}<br>简答均分: ${p.data[1]}`
    },
    legend: { data: ['严谨学术型', '风趣互动型', '均衡发展型'], bottom: 0 },
    xAxis: { type: 'value', name: '单选均分', nameLocation: 'middle', nameGap: 25 },
    yAxis: { type: 'value', name: '简答均分', nameLocation: 'middle', nameGap: 35 },
    series,
    grid: { left: '8%', right: '4%', bottom: '14%', top: '6%', containLabel: true },
  })
}

async function showDetail(row) {
  detailTeacher.value = row
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await adminAnalysisApi.teacherReport(row.teacherId, selectedSemester.value)
    detail.value = res.data
  } finally { detailLoading.value = false }
}

watch(activeTab, async (name) => {
  if (name === 'weights' && !weights.value.length)         await loadWeights()
  if (name === 'cluster' && !clusterData.value)            await loadClusters()
  if (name === 'engagement') {
    if (!engagementOverview.value) {
      await loadEngagement()
    } else {
      await nextTick()
      const data = engagementOverview.value
      renderAttendChart(data.attendanceDist || {})
      renderStudyChart(data.studyDist || {})
    }
  }
})

onMounted(async () => {
  const semRes = await adminSemesterApi.list()
  semesters.value = semRes.data || []
  const cur = semesters.value.find(s => s.isCurrent === 1)
  selectedSemester.value = cur?.id || semesters.value[0]?.id
  await loadStats()
})
</script>

<style scoped>
.title { font-size: 15px; font-weight: 600; }
.dim-label { font-size: 12px; font-weight: 600; margin-bottom: 6px; }
.header-flex { display: flex; align-items: center; justify-content: space-between; }
.header-controls { display: flex; align-items: center; gap: 8px; }
.section-title { font-weight: 600; color: #303133; margin: 16px 0 8px; font-size: 14px; display: flex; align-items: center; gap: 6px; }
.comments-list { display: flex; flex-wrap: wrap; gap: 4px; padding: 8px 0; }
.no-comment { color: #c0c4cc; font-size: 13px; }

.cluster-card {
  border: 2px solid; border-radius: 12px; padding: 16px; text-align: center; height: 120px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
}
.cluster-label { font-size: 16px; font-weight: 700; }
.cluster-count { font-size: 22px; font-weight: 700; color: #303133; }
.cluster-desc { font-size: 11px; color: #909399; text-align: center; line-height: 1.4; }

.algo-card {
  background: #f8f9ff; border: 1px solid #e8ebf7; border-radius: 10px;
  padding: 14px 16px; height: 100%;
}
.algo-title { font-weight: 700; color: #1a2a6c; font-size: 14px; margin-bottom: 10px; }
.engagement-overview { margin-top: 4px; }
.eng-stat-card {
  background: #f5f7ff; border: 1px solid #dde1f7; border-radius: 10px;
  padding: 14px 12px; text-align: center;
}
.eng-stat-card.green  { background: #f0fff4; border-color: #b7eb8f; }
.eng-stat-card.red    { background: #fff0f0; border-color: #ffa8a8; }
.eng-stat-card.orange { background: #fff8f0; border-color: #ffd591; }
.eng-num   { font-size: 26px; font-weight: 700; color: #1a2a6c; }
.eng-label { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
