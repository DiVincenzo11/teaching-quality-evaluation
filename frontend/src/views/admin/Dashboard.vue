<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>系统概览</h2>
      <el-tag type="success">{{ currentSemester?.semesterName || '暂无当前学期' }}</el-tag>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="`--card-color: ${card.color}`">
          <div class="stat-icon">
            <el-icon size="32"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>教师评分排行</span>
              <el-select v-model="selectedSemester" size="small" style="width:200px"
                         @change="loadStats" placeholder="选择学期">
                <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
              </el-select>
            </div>
          </template>
          <el-table :data="teacherStats" stripe height="300" v-loading="statsLoading">
            <el-table-column type="index" label="#" width="44" />
            <el-table-column prop="realName"  label="姓名"  width="80" />
            <el-table-column prop="department" label="院系" width="150" show-overflow-tooltip />
            <el-table-column label="所教科目" min-width="110" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.courses?.length">{{ row.courses.join('、') }}</span>
                <span v-else class="no-data">—</span>
              </template>
            </el-table-column>
            <el-table-column label="所教班级" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.classes">{{ row.classes }}</span>
                <span v-else class="no-data">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="evalCount"  label="评价次数" width="80" align="center" />
            <el-table-column prop="avgScore"   label="平均分" width="86" align="center">
              <template #default="{ row }">
                <el-tag :type="row.avgScore >= 85 ? 'success' : row.avgScore >= 70 ? 'warning' : 'danger'">
                  {{ row.avgScore ?? '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="72" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openReport(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>评分分布</span></template>
          <div ref="chartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>

  <!-- 教师报告抽屉 -->
  <TeacherReportDrawer
    v-model="reportDrawerVisible"
    :teacher-id="reportTeacherId"
    :semester-id="selectedSemester"
  />
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { adminAnalysisApi, adminSemesterApi, adminDashboardApi } from '@/api/index'
import * as echarts from 'echarts'
import TeacherReportDrawer from './TeacherReportDrawer.vue'

const currentSemester    = ref(null)
const semesters          = ref([])
const selectedSemester   = ref(null)
const teacherStats       = ref([])
const statsLoading       = ref(false)
const chartRef           = ref()
let   chartInstance      = null

const reportDrawerVisible = ref(false)
const reportTeacherId     = ref(null)

function openReport(row) {
  reportTeacherId.value     = row.teacherId
  reportDrawerVisible.value = true
}

const statCards = ref([
  { label: '教师总数',   value: 0, icon: 'Avatar',    color: '#4f7ef8' },
  { label: '学生总数',   value: 0, icon: 'UserFilled', color: '#67c23a' },
  { label: '本学期评价', value: 0, icon: 'DocumentChecked', color: '#e6a23c' },
  { label: '公告数量',   value: 0, icon: 'Bell',       color: '#f56c6c' },
])

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await adminAnalysisApi.teacherStats(selectedSemester.value)
    teacherStats.value = res.data || []
    renderChart()
  } finally {
    statsLoading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)
  const scores = teacherStats.value.map(t => Number(t.avgScore) || 0)
  const ranges = ['0-60', '60-70', '70-80', '80-90', '90-100']
  const counts = [0, 0, 0, 0, 0]
  scores.forEach(s => {
    if (s < 60) counts[0]++
    else if (s < 70) counts[1]++
    else if (s < 80) counts[2]++
    else if (s < 90) counts[3]++
    else counts[4]++
  })
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis:   { type: 'category', data: ranges },
    yAxis:   { type: 'value', name: '人数' },
    series:  [{
      type: 'bar',
      data: counts,
      itemStyle: { borderRadius: [4,4,0,0],
        color: new echarts.graphic.LinearGradient(0,0,0,1,
          [{ offset:0, color:'#4f7ef8' }, { offset:1, color:'#11998e' }]) }
    }],
    grid: { left: '5%', right: '5%', bottom: '5%', top: '10%', containLabel: true },
  })
}

async function loadOverview() {
  try {
    const res = await adminDashboardApi.overview()
    const d = res.data || {}
    statCards.value[0].value = d.teacherCount ?? 0
    statCards.value[1].value = d.studentCount ?? 0
    statCards.value[2].value = d.evalCount ?? 0
    statCards.value[3].value = d.announcementCount ?? 0
  } catch { /* ignore */ }
}

onMounted(async () => {
  const semRes = await adminSemesterApi.list()
  semesters.value = semRes.data || []
  currentSemester.value = semesters.value.find(s => s.isCurrent === 1)
  selectedSemester.value = currentSemester.value?.id || semesters.value[0]?.id
  await Promise.all([loadOverview(), loadStats()])
})
</script>

<style scoped>
.dashboard { }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #303133; }
.stat-cards { margin-bottom: 20px; }
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}
.stat-icon {
  width: 56px; height: 56px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--card-color) 15%, white);
  display: flex; align-items: center; justify-content: center;
  color: var(--card-color);
}
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #8c9ab0; margin-top: 2px; }
.chart-row :deep(.el-card) { border-radius: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.no-data { color: #c0c4cc; }
</style>
