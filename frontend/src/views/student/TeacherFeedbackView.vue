<template>
  <div class="page">
    <div class="page-header">
      <span class="page-title">教师反馈查看</span>
      <el-select v-model="selectedSemester" placeholder="请选择学期" style="width:220px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
    </div>

    <el-alert v-if="!selectedSemester" type="info" :closable="false" show-icon style="margin-top:16px">
      请选择学期，查看教过您所在班级的教师的反馈与改进计划
    </el-alert>

    <div v-else v-loading="loading">
      <el-empty v-if="!loading && !feedbacks.length" description="该学期暂无教师反馈" style="margin-top:40px" />

      <div v-if="feedbacks.length" class="sort-bar">
        <span class="sort-label">排序：</span>
        <el-radio-group v-model="sortKey" size="small" @change="sortOrder = 'desc'">
          <el-radio-button value="none">默认</el-radio-button>
          <el-radio-button value="myScore">我的打分</el-radio-button>
          <el-radio-button value="avgScore">平均打分</el-radio-button>
        </el-radio-group>
        <el-button
          v-if="sortKey !== 'none'"
          size="small"
          :icon="sortOrder === 'desc' ? ArrowDown : ArrowUp"
          circle
          style="margin-left:8px"
          @click="sortOrder = sortOrder === 'desc' ? 'asc' : 'desc'"
        />
      </div>

      <div v-if="!loading && feedbacks.length" class="card-grid">
        <div
          v-for="fb in sortedFeedbacks"
          :key="fb.id"
          class="fb-card"
          @click="openDetail(fb)"
        >
          <!-- 头部：教师基本信息 -->
          <div class="fb-card-head">
            <div class="teacher-avatar">{{ fb.teacherName?.slice(-1) }}</div>
            <div class="teacher-meta">
              <div class="teacher-name">
                {{ fb.teacherName }}
                <el-tag v-if="fb.title" size="small" type="info" style="margin-left:6px">{{ fb.title }}</el-tag>
              </div>
              <div class="teacher-course">
                <el-icon size="12" color="#909399"><Collection /></el-icon>
                <span>{{ fb.courseNames || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- 分数行 -->
          <div class="score-row">
            <div class="score-item" :class="myScoreClass(fb.myScore)">
              <span class="score-val">{{ fb.myScore != null ? Number(fb.myScore).toFixed(2) : '—' }}</span>
              <span class="score-lbl">我的打分</span>
            </div>
            <div class="score-divider"></div>
            <div class="score-item avg">
              <span class="score-val">{{ fb.avgScore != null ? Number(fb.avgScore).toFixed(2) : '—' }}</span>
              <span class="score-lbl">平均打分</span>
            </div>
          </div>

          <!-- 摘要 -->
          <div class="fb-section">
            <div class="fb-section-label">
              <el-icon color="#67c23a"><EditPen /></el-icon> 教学反思
            </div>
            <div class="fb-excerpt">{{ fb.reflection }}</div>
          </div>
          <div class="fb-section">
            <div class="fb-section-label">
              <el-icon color="#409eff"><Aim /></el-icon> 改进计划
            </div>
            <div class="fb-excerpt">{{ fb.improvementPlan }}</div>
          </div>

          <!-- 底部 -->
          <div class="fb-card-foot">
            <span class="fb-time">{{ formatTime(fb.updateTime) }}</span>
            <el-button type="primary" link size="small">查看详情 →</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${current?.teacherName} 老师的教学反馈`"
      width="640px"
      destroy-on-close
    >
      <template v-if="current">
        <!-- 教师信息卡 -->
        <div class="detail-teacher-bar">
          <div class="teacher-avatar big">{{ current.teacherName?.slice(-1) }}</div>
          <div style="flex:1;min-width:0">
            <div class="teacher-name" style="font-size:17px;font-weight:700">
              {{ current.teacherName }}
              <el-tag v-if="current.title" size="small" type="info" style="margin-left:8px">{{ current.title }}</el-tag>
            </div>
            <div class="teacher-course" style="margin-top:4px;font-size:13px;color:#606266">
              <el-icon size="13"><Collection /></el-icon>
              所教科目：{{ current.courseNames || '—' }}
            </div>
            <div style="font-size:12px;color:#909399;margin-top:2px">{{ current.semesterName }}</div>
          </div>
          <!-- 分数展示 -->
          <div class="detail-scores">
            <div class="ds-item" :class="myScoreClass(current.myScore)">
              <div class="ds-val">{{ current.myScore != null ? Number(current.myScore).toFixed(2) : '—' }}</div>
              <div class="ds-lbl">我的打分</div>
            </div>
            <div class="ds-item avg">
              <div class="ds-val">{{ current.avgScore != null ? Number(current.avgScore).toFixed(2) : '—' }}</div>
              <div class="ds-lbl">平均打分</div>
            </div>
          </div>
        </div>

        <!-- 教学反思 -->
        <div class="detail-section">
          <div class="detail-section-title success">
            <el-icon><EditPen /></el-icon> 教学反思
          </div>
          <div class="detail-content">{{ current.reflection || '（暂无内容）' }}</div>
        </div>

        <!-- 改进计划 -->
        <div class="detail-section">
          <div class="detail-section-title primary">
            <el-icon><Aim /></el-icon> 改进计划
          </div>
          <div class="detail-content">{{ current.improvementPlan || '（暂无内容）' }}</div>
        </div>

        <div style="text-align:right;margin-top:12px;font-size:12px;color:#c0c4cc">
          更新于 {{ formatTime(current.updateTime) }}
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { studentApi } from '@/api/index'
import { Collection, EditPen, Aim, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

const feedbacks        = ref([])
const semesters        = ref([])
const selectedSemester = ref(null)
const loading          = ref(false)
const dialogVisible    = ref(false)
const current          = ref(null)
const sortKey          = ref('none')   // 'none' | 'myScore' | 'avgScore'
const sortOrder        = ref('desc')   // 'asc' | 'desc'

const sortedFeedbacks = computed(() => {
  if (sortKey.value === 'none') return feedbacks.value
  return [...feedbacks.value].sort((a, b) => {
    const va = Number(a[sortKey.value]) || 0
    const vb = Number(b[sortKey.value]) || 0
    return sortOrder.value === 'desc' ? vb - va : va - vb
  })
})

function openDetail(fb) {
  current.value = fb
  dialogVisible.value = true
}

function myScoreClass(score) {
  if (score == null) return 'no-score'
  const v = Number(score)
  if (v >= 90) return 'excellent'
  if (v >= 80) return 'good'
  if (v >= 70) return 'average'
  return 'poor'
}

function formatTime(t) {
  if (!t) return '—'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

async function loadData() {
  if (!selectedSemester.value) { feedbacks.value = []; return }
  loading.value = true
  try {
    const res = await studentApi.feedbackList(selectedSemester.value)
    feedbacks.value = res.data || []
  } finally { loading.value = false }
}

onMounted(async () => {
  const semRes = await studentApi.allSemesters()
  semesters.value = (semRes.data || []).sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
  const cur = semesters.value.find(s => s.isCurrent === 1) || semesters.value[semesters.value.length - 1]
  selectedSemester.value = cur?.id
  loadData()
})
</script>

<style scoped>
.page { padding: 20px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title { font-size: 18px; font-weight: 700; color: #303133; }

/* 排序栏 */
.sort-bar {
  display: flex; align-items: center; margin-bottom: 16px;
}
.sort-label { font-size: 13px; color: #606266; margin-right: 8px; white-space: nowrap; }

/* 卡片网格 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 4px;
}

.fb-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 18px 20px 14px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.fb-card:hover {
  box-shadow: 0 6px 24px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

/* 头部 */
.fb-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.teacher-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #4f7ef8);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.teacher-avatar.big {
  width: 52px; height: 52px; font-size: 20px;
}
.teacher-meta { flex: 1; min-width: 0; }
.teacher-name { font-size: 15px; font-weight: 600; color: #303133; display: flex; align-items: center; flex-wrap: wrap; }
.teacher-course {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399; margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* 摘要区 */
.fb-section { }
.fb-section-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 600; color: #606266;
  margin-bottom: 4px;
}
.fb-excerpt {
  font-size: 13px; color: #909399; line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部 */
.fb-card-foot {
  display: flex; align-items: center; justify-content: space-between;
  border-top: 1px solid #f0f0f0; padding-top: 10px; margin-top: 2px;
}
.fb-time { font-size: 12px; color: #c0c4cc; }

/* 分数行 */
.score-row {
  display: flex; align-items: center; gap: 0;
  background: #f5f7fa; border-radius: 10px; padding: 10px 0;
}
.score-item {
  flex: 1; text-align: center;
}
.score-divider { width: 1px; height: 32px; background: #dcdfe6; }
.score-val { display: block; font-size: 22px; font-weight: 700; line-height: 1.2; }
.score-lbl { display: block; font-size: 11px; color: #909399; margin-top: 2px; }
.score-item.excellent .score-val { color: #67c23a; }
.score-item.good      .score-val { color: #409eff; }
.score-item.average   .score-val { color: #e6a23c; }
.score-item.poor      .score-val { color: #f56c6c; }
.score-item.no-score  .score-val { color: #c0c4cc; }
.score-item.avg       .score-val { color: #303133; }

/* 弹窗分数 */
.detail-scores { display: flex; gap: 16px; margin-left: 12px; }
.ds-item { text-align: center; min-width: 56px; }
.ds-val { font-size: 22px; font-weight: 700; line-height: 1.2; }
.ds-lbl { font-size: 11px; color: #909399; margin-top: 2px; }
.ds-item.excellent .ds-val { color: #67c23a; }
.ds-item.good      .ds-val { color: #409eff; }
.ds-item.average   .ds-val { color: #e6a23c; }
.ds-item.poor      .ds-val { color: #f56c6c; }
.ds-item.no-score  .ds-val { color: #c0c4cc; }
.ds-item.avg       .ds-val { color: #303133; }

/* 弹窗详情 */
.detail-teacher-bar {
  display: flex; align-items: center; gap: 14px;
  background: #f5f7fa; border-radius: 10px; padding: 14px 16px;
  margin-bottom: 20px;
}
.detail-section { margin-bottom: 18px; }
.detail-section-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; font-weight: 700; margin-bottom: 8px;
}
.detail-section-title.success { color: #67c23a; }
.detail-section-title.primary { color: #409eff; }
.detail-content {
  font-size: 14px; color: #303133; line-height: 1.9;
  background: #fafafa; border-radius: 8px;
  padding: 14px 16px; border: 1px solid #f0f0f0;
  white-space: pre-wrap;
}
</style>
