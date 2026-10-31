<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div>
          <div class="card-header">
            <span class="title">我的评价详情</span>
            <el-select
              v-model="selectedSemesterId"
              placeholder="选择学期"
              size="small"
              style="width:220px"
              @change="loadData"
            >
              <el-option
                v-for="s in semesterList"
                :key="s.id"
                :label="s.semesterName"
                :value="s.id"
              />
            </el-select>
          </div>
          <el-alert
            v-if="!loading && semesterList.length === 0"
            type="warning"
            :closable="false"
            show-icon
            style="margin-top:12px"
          >
            <template #title>
              暂无授课学期数据，您当前没有关联的授课学期
            </template>
          </el-alert>
        </div>
      </template>

      <div v-if="data" v-loading="loading">
        <!-- 总分概览 -->
        <el-row :gutter="20" style="margin-bottom:24px">
          <el-col :span="8">
            <div class="score-card">
              <div class="score-circle" :class="scoreClass">{{ data.avgScore }}</div>
              <div class="score-label">综合平均分</div>
              <div class="score-sub">共 {{ data.evalCount }} 人参与评价</div>
            </div>
          </el-col>
          <el-col :span="16">
            <div ref="chartRef" style="height:180px"></div>
          </el-col>
        </el-row>

        <!-- 各题分析 -->
        <div class="section-title">各题平均得分</div>
        <el-table :data="data.questionAvg" border stripe size="small" style="margin-bottom:24px">
          <el-table-column prop="questionNo"      label="题号"  width="60" align="center" />
          <el-table-column prop="questionContent" label="题目内容" show-overflow-tooltip />
          <el-table-column prop="avgScore"        label="平均分" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="subScoreType(row.avgScore, row.maxScore)" size="small">
                {{ row.avgScore }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxScore" label="满分"  width="70" align="center" />
          <el-table-column label="得分率" width="140" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round((row.avgScore / row.maxScore) * 100)"
                :color="progressColor(row.avgScore, row.maxScore)"
              />
            </template>
          </el-table-column>
        </el-table>

        <!-- ABSA 情感分析 -->
        <template v-if="data.absa?.available">
          <div class="section-title">
            <el-icon color="#4f7ef8"><MagicStick /></el-icon>
            细粒度情感分析（BERT语义分析）
            <el-tooltip content="基于预训练BERT模型对学生评语进行方面级情感分析，非关键词匹配" placement="top">
              <el-icon color="#c0c4cc" style="cursor:help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-row :gutter="16" style="margin-bottom:24px">
            <el-col :span="14">
              <div class="absa-grid">
                <div
                  v-for="(score, aspect) in data.absa.dimensionAvg"
                  :key="aspect"
                  class="absa-item"
                >
                  <div class="absa-label">{{ aspect }}</div>
                  <el-progress
                    :percentage="score"
                    :color="absaColor(score)"
                    :stroke-width="12"
                    :text-inside="true"
                    :format="() => score + '%'"
                  />
                </div>
              </div>
            </el-col>
            <el-col :span="10">
              <div ref="absaChartRef" style="height:200px"></div>
            </el-col>
          </el-row>
          <el-alert type="success" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              基于 {{ data.absa.analyzeCount }} 条评语的语义分析，综合情感分：
              <b>{{ data.absa.overallAvg }}</b>
            </template>
          </el-alert>
        </template>
        <template v-else-if="(data.comments?.length || 0) > 0 && data.absa !== null && data.absa !== undefined && !data.absa?.available">
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>
              BERT情感分析服务暂不可用，请启动 nlp_service/start.bat 后刷新
            </template>
          </el-alert>
        </template>

        <!-- 学生评语 -->
        <div class="comments-section" v-if="data.comments?.length">
          <div class="comments-top-bar">
            <span class="section-title">学生评语（匿名，共 {{ data.comments.length }} 条）</span>
            <!-- 好评/中评/差评 筛选 -->
            <div class="comment-filter-bar">
              <button
                v-for="tab in commentTabs" :key="tab.key"
                class="filter-btn"
                :class="[tab.cls, { active: commentFilter === tab.key }]"
                @click="switchFilter(tab.key)"
              >
                {{ tab.label }}
                <span class="filter-count">{{ tab.count }}</span>
              </button>
            </div>
          </div>

          <div class="comments-list" v-if="filteredComments.length">
            <div
              v-for="(c, i) in paginatedComments"
              :key="commentPage * commentPageSize + i"
              class="comment-item"
              :class="commentItemClass"
            >
              <el-icon :color="commentIconColor" class="comment-icon"><ChatLineRound /></el-icon>
              <span class="comment-text">{{ c }}</span>
            </div>
          </div>
          <el-empty v-else description="该分类暂无评语" :image-size="60" style="padding:20px 0" />

          <div class="pagination-bar pagination-bottom">
            <span class="page-hint">第 {{ commentPage }} / {{ Math.ceil(filteredComments.length / commentPageSize) || 1 }} 页</span>
            <el-pagination
              v-model:current-page="commentPage"
              :page-size="commentPageSize"
              :total="filteredComments.length"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next, jumper"
              :hide-on-single-page="false"
              @size-change="commentPage = 1"
            />
          </div>
        </div>
        <el-empty v-else description="暂无学生评语" />
      </div>
      <el-empty v-else-if="!loading" description="暂无评价数据" />
      <div v-else v-loading="true" style="height:200px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { teacherApi } from '@/api/index'
import * as echarts from 'echarts'

const semesterList      = ref([])
const selectedSemesterId = ref(null)
const data              = ref(null)
const loading           = ref(false)
const commentPage       = ref(1)
const commentPageSize   = ref(10)
const commentFilter     = ref('all')   // 'all' | 'positive' | 'neutral' | 'negative'
const chartRef        = ref()
const absaChartRef    = ref()
let   chartInst       = null
let   absaChartInst   = null

const filteredComments = computed(() => {
  if (!data.value) return []
  if (commentFilter.value === 'positive') return data.value.commentsPositive || []
  if (commentFilter.value === 'neutral')  return data.value.commentsNeutral  || []
  if (commentFilter.value === 'negative') return data.value.commentsNegative || []
  return data.value.comments || []
})

const paginatedComments = computed(() => {
  const start = (commentPage.value - 1) * commentPageSize.value
  return filteredComments.value.slice(start, start + commentPageSize.value)
})

const commentTabs = computed(() => {
  const d = data.value
  return [
    { key: 'all',      label: '全部',  cls: 'tab-all',  count: d?.comments?.length || 0 },
    { key: 'positive', label: '好评',  cls: 'tab-pos',  count: d?.commentsPositive?.length || 0 },
    { key: 'neutral',  label: '中评',  cls: 'tab-neu',  count: d?.commentsNeutral?.length  || 0 },
    { key: 'negative', label: '差评',  cls: 'tab-neg',  count: d?.commentsNegative?.length || 0 },
  ]
})

const commentItemClass = computed(() => ({
  'item-positive': commentFilter.value === 'positive',
  'item-neutral':  commentFilter.value === 'neutral',
  'item-negative': commentFilter.value === 'negative',
}))

const commentIconColor = computed(() => {
  if (commentFilter.value === 'positive') return '#67c23a'
  if (commentFilter.value === 'negative') return '#f56c6c'
  if (commentFilter.value === 'neutral')  return '#909399'
  return '#909399'
})

function switchFilter(key) {
  commentFilter.value = key
  commentPage.value = 1
}

const scoreClass = computed(() => {
  const v = Number(data.value?.avgScore)
  if (v >= 90) return 'excellent'
  if (v >= 75) return 'good'
  if (v >= 60) return 'average'
  return 'poor'
})

async function loadData() {
  if (!selectedSemesterId.value) return
  loading.value = true
  data.value = null
  try {
    const res = await teacherApi.evalStats(selectedSemesterId.value)
    data.value = res.data
    commentPage.value  = 1
    commentFilter.value = 'all'
    await nextTick()
    renderChart()
    renderAbsaChart()
  } finally { loading.value = false }
}

function renderAbsaChart() {
  if (!absaChartRef.value || !data.value?.absa?.dimensionAvg) return
  if (!absaChartInst) absaChartInst = echarts.init(absaChartRef.value)
  const dims = data.value.absa.dimensionAvg
  const labels = Object.keys(dims)
  const values = Object.values(dims)
  absaChartInst.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    xAxis:   { type: 'category', data: labels, axisLabel: { interval: 0, fontSize: 11, rotate: 10 } },
    yAxis:   { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'bar', data: values,
      label: { show: true, position: 'top', formatter: '{c}%', fontSize: 11 },
      itemStyle: {
        borderRadius: [4,4,0,0],
        color: (p) => {
          const v = p.data
          if (v >= 80) return '#67c23a'
          if (v >= 65) return '#409eff'
          if (v >= 50) return '#e6a23c'
          return '#f56c6c'
        }
      },
    }],
    grid: { left: '4%', right: '4%', bottom: '12%', top: '12%', containLabel: true },
  })
}

function renderChart() {
  if (!chartRef.value || !data.value?.questionAvg) return
  if (!chartInst) chartInst = echarts.init(chartRef.value)
  const qavg = data.value.questionAvg
  chartInst.setOption({
    tooltip: { trigger: 'axis' },
    xAxis:   { type: 'category', data: qavg.map(q => `Q${q.questionNo}`), axisLabel: { interval: 0 } },
    yAxis:   { type: 'value', max: 10, name: '分' },
    series:  [{
      type: 'bar',
      data: qavg.map(q => q.avgScore),
      itemStyle: {
        borderRadius: [4,4,0,0],
        color: new echarts.graphic.LinearGradient(0,0,0,1,
          [{ offset:0, color:'#4f7ef8' }, { offset:1, color:'#a0c4ff' }])
      },
      label: { show: true, position: 'top', fontSize: 11 },
    }],
    grid: { left: '5%', right: '3%', bottom: '8%', top: '12%', containLabel: true },
  })
}

const absaColor = (v) => {
  if (v >= 80) return '#67c23a'
  if (v >= 65) return '#409eff'
  if (v >= 50) return '#e6a23c'
  return '#f56c6c'
}

const subScoreType = (s, max) => {
  const r = s / max
  if (r >= 0.9) return 'success'
  if (r >= 0.7) return 'primary'
  if (r >= 0.5) return 'warning'
  return 'danger'
}
const progressColor = (s, max) => {
  const r = s / max * 100
  if (r >= 90) return '#67c23a'
  if (r >= 70) return '#409eff'
  if (r >= 50) return '#e6a23c'
  return '#f56c6c'
}

onMounted(async () => {
  let list = []
  try {
    const semRes = await teacherApi.semesters()
    list = semRes?.data || []
  } catch (_) { /* 接口失败时保持空列表 */ }
  semesterList.value = list
  const currentIdx = list.findIndex(s => s.isCurrent === 1)
  const defaultSemester = currentIdx > 0 ? list[currentIdx - 1] : (list[0] || list[list.length - 1])
  selectedSemesterId.value = defaultSemester?.id ?? list[0]?.id
  loadData()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.semester-text { font-size: 14px; color: #606266; }
.score-card { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 180px; }
.score-circle {
  width: 90px; height: 90px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: white;
  margin-bottom: 10px;
}
.score-circle.excellent { background: #67c23a; }
.score-circle.good      { background: #409eff; }
.score-circle.average   { background: #e6a23c; }
.score-circle.poor      { background: #f56c6c; }
.score-label { font-size: 14px; color: #303133; font-weight: 600; }
.score-sub   { font-size: 12px; color: #8c9ab0; margin-top: 4px; }
.section-title { font-size: 15px; font-weight: 600; color: #303133; margin: 16px 0 10px; }
.absa-grid { display: flex; flex-direction: column; gap: 10px; }
.absa-item {}
.absa-label { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 4px; }
.page { padding-bottom: 24px; }
.comments-section { margin-top: 8px; }
.comments-top-bar {
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  margin-bottom: 12px;
}
.comments-top-bar .section-title { margin: 0; }

/* 筛选按钮组 */
.comment-filter-bar { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 12px; border-radius: 16px; font-size: 13px; font-weight: 600;
  cursor: pointer; border: 1.5px solid transparent; background: #f5f7fa;
  color: #606266; transition: all 0.2s;
}
.filter-btn:hover { opacity: 0.85; }
.filter-count {
  font-size: 11px; background: rgba(0,0,0,0.08);
  border-radius: 8px; padding: 0 5px; min-width: 18px; text-align: center;
}

.tab-all.active  { background: #ecf5ff; border-color: #409eff; color: #409eff; }
.tab-all.active .filter-count { background: #409eff; color: white; }
.tab-pos.active  { background: #f0f9eb; border-color: #67c23a; color: #67c23a; }
.tab-pos.active .filter-count { background: #67c23a; color: white; }
.tab-neu.active  { background: #f4f4f5; border-color: #909399; color: #606266; }
.tab-neu.active .filter-count { background: #909399; color: white; }
.tab-neg.active  { background: #fef0f0; border-color: #f56c6c; color: #f56c6c; }
.tab-neg.active .filter-count { background: #f56c6c; color: white; }

/* 评语条目颜色 */
.item-positive { border-left: 3px solid #67c23a !important; }
.item-neutral  { border-left: 3px solid #909399 !important; }
.item-negative { border-left: 3px solid #f56c6c !important; }
.pagination-bar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  padding: 10px 16px; background: #ecf5ff; border: 1px solid #c6e2ff; border-radius: 8px;
  min-height: 48px;
}
.page-hint { font-size: 14px; color: #409eff; font-weight: 600; white-space: nowrap; flex-shrink: 0; }
.pagination-bottom { margin-top: 16px; }
.comments-list {
  display: flex; flex-direction: column; gap: 6px;
  background: #fafbfc; border-radius: 8px; padding: 12px; border: 1px solid #ebeef5;
}
.comment-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 8px 12px; font-size: 14px; color: #606266; line-height: 1.5;
  background: white; border-radius: 6px; border: 1px solid #f0f2f5;
}
.comment-icon { flex-shrink: 0; margin-top: 2px; }
.comment-text { flex: 1; word-break: break-word; }
</style>
