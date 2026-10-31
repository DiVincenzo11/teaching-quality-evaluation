<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">我的评价历史</span>
          <el-select v-model="selectedSemester" placeholder="请选择学期" style="width:260px"
                     @change="onSemesterChange">
            <el-option v-for="s in sortedSemesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
        </div>
      </template>

      <!-- 按学期分组展示 -->
      <div v-loading="loading">
        <template v-if="groupedBySemester.length">
        <div v-for="group in groupedBySemester" :key="group.semesterId" class="semester-group">
          <div class="semester-title">
            <el-icon><Calendar /></el-icon>
            {{ group.semesterName }}
          </div>
          <el-table :data="group.records" stripe border size="default">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="teacherName" label="教师姓名" width="110" align="center" />
            <el-table-column prop="courses"     label="负责课程" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">
                <span style="color:#606266">{{ row.courses || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="department"  label="院系" show-overflow-tooltip min-width="120" />
            <el-table-column prop="choiceScore" label="单选题分" width="95" align="center" />
            <el-table-column prop="essayScore"  label="简答题分" width="95" align="center" />
            <el-table-column prop="totalScore"  label="总分" width="100" align="center" sortable>
              <template #default="{ row }">
                <el-tag :type="row.totalScore >= 85 ? 'success' : row.totalScore >= 70 ? 'primary' : row.totalScore >= 60 ? 'warning' : 'danger'">
                  {{ row.totalScore }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="essayContent" label="我的评语" show-overflow-tooltip min-width="160" />
            <el-table-column prop="createTime"   label="评价时间" width="165" />
          </el-table>
        </div>
      </template>
      <el-empty v-else-if="!loading"
                :description="selectedSemester ? '该学期暂无评价记录' : '您还没有提交过评价，请先完成评价'" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { studentApi } from '@/api/index'
import { Calendar } from '@element-plus/icons-vue'

const records = ref([])
const semesters = ref([])
const selectedSemester = ref(null)
const loading = ref(false)

const sortedSemesters = computed(() => semesters.value || [])

const groupedBySemester = computed(() => {
  const list = records.value
  if (!list.length) return []
  const map = new Map()
  for (const r of list) {
    const key = r.semesterId ?? 0
    const name = r.semesterName || `学期${key}`
    if (!map.has(key)) map.set(key, { semesterId: key, semesterName: name, records: [] })
    map.get(key).records.push(r)
  }
  return Array.from(map.values()).sort((a, b) => (a.semesterId || 0) - (b.semesterId || 0))
})

/** 学期显示顺序：2022-2023第一 → 2025-2026第二（含无评价学期） */
const SEMESTER_ORDER = [
  '2022-2023学年第一学期', '2022-2023学年第二学期',
  '2023-2024学年第一学期', '2023-2024学年第二学期',
  '2024-2025学年第一学期', '2024-2025学年第二学期',
  '2025-2026学年第一学期', '2025-2026学年第二学期',
]

async function loadSemesters() {
  const res = await studentApi.allSemesters()
  const list = res.data || []
  semesters.value = list.sort((a, b) => {
    const i = SEMESTER_ORDER.indexOf(a.semesterName)
    const j = SEMESTER_ORDER.indexOf(b.semesterName)
    return (i < 0 ? 999 : i) - (j < 0 ? 999 : j)
  })
}

async function loadHistory() {
  if (!selectedSemester.value) {
    records.value = []
    return
  }
  loading.value = true
  try {
    const res = await studentApi.historyBySemester(selectedSemester.value)
    records.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onSemesterChange() {
  loadHistory()
}

onMounted(async () => {
  await loadSemesters()
  if (semesters.value?.length && !selectedSemester.value) {
    selectedSemester.value = semesters.value[0].id
  }
  await loadHistory()
})
</script>

<style scoped>
.title { font-size: 16px; font-weight: 600; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.semester-group { margin-bottom: 24px; }
.semester-group:last-child { margin-bottom: 0; }
.semester-title {
  font-size: 15px; font-weight: 600; color: #303133;
  margin-bottom: 12px; padding: 8px 0; border-bottom: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px;
}
.semester-title .el-icon { color: #409eff; }
</style>
