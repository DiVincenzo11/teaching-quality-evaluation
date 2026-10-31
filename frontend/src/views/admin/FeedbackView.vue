<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-title-row">
        <span class="page-title">教师反馈查看</span>
        <el-tag v-if="feedbacks.length" type="info" size="small" style="margin-left:10px">
          共 {{ sortedFeedbacks.length }}{{ sortedFeedbacks.length < feedbacks.length ? `/${feedbacks.length}` : '' }} 条
        </el-tag>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="query.semesterId" placeholder="选择学期" style="width:210px" @change="() => { keywordInput = ''; keyword = ''; loadData() }">
          <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
        </el-select>
        <el-input
          v-model="keywordInput"
          placeholder="搜索教师姓名"
          clearable
          style="width:160px"
          :prefix-icon="Search"
          @keyup.enter="doSearch"
          @clear="() => { keywordInput = ''; doSearch() }"
        />
        <el-button type="primary" :icon="Search" @click="doSearch">搜索</el-button>
        <el-divider direction="vertical" />
        <span style="font-size:13px;color:#606266">按均分排序：</span>
        <el-radio-group v-model="sortOrder" size="small">
          <el-radio-button value="none">默认</el-radio-button>
          <el-radio-button value="desc">高→低</el-radio-button>
          <el-radio-button value="asc">低→高</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 提示 -->
    <el-alert v-if="!query.semesterId" type="info" :closable="false" show-icon style="margin-top:16px">
      请先选择学期查看该学期的教师反馈
    </el-alert>

    <!-- 卡片列表 -->
    <div v-else v-loading="loading">
      <el-empty v-if="!loading && !feedbacks.length" description="该学期暂无教师反馈" style="margin-top:60px" />

      <div v-else class="card-grid">
        <div
          v-for="fb in sortedFeedbacks"
          :key="fb.id"
          class="fb-card"
          @click="openDetail(fb)"
        >
          <!-- 头部 -->
          <div class="fb-head">
            <div class="avatar">{{ fb.teacherName?.slice(-1) }}</div>
            <div class="meta">
              <div class="name-row">
                <span class="teacher-name">{{ fb.teacherName }}</span>
                <el-tag v-if="fb.title" size="small" type="info" style="margin-left:6px">{{ fb.title }}</el-tag>
              </div>
              <div class="course-row" v-if="fb.courseNames">
                <el-icon size="11" color="#909399"><Collection /></el-icon>
                <span>{{ fb.courseNames }}</span>
              </div>
              <div class="semester-row">
                <el-icon size="11" color="#c0c4cc"><Calendar /></el-icon>
                <span>{{ fb.semesterName }}</span>
              </div>
            </div>
            <!-- 均分徽章 -->
            <div class="score-badge" :class="'badge-' + scoreTagType(fb.avgScore)">
              <div class="score-val">{{ fb.avgScore != null ? Number(fb.avgScore).toFixed(2) : '--' }}</div>
              <div class="score-lbl">均分</div>
            </div>
          </div>

          <!-- 内容摘要 -->
          <div class="fb-body">
            <div class="section">
              <span class="sec-label green"><el-icon><EditPen /></el-icon>教学反思</span>
              <p class="excerpt">{{ fb.reflection || '暂无内容' }}</p>
            </div>
            <div class="section">
              <span class="sec-label blue"><el-icon><Aim /></el-icon>改进计划</span>
              <p class="excerpt">{{ fb.improvementPlan || '暂无内容' }}</p>
            </div>
          </div>

          <!-- 底部 -->
          <div class="fb-foot">
            <span class="time">{{ fb.updateTime?.slice(0, 10) }}</span>
            <span class="view-link">查看详情 →</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="`${current?.teacherName} — 教学反馈详情`"
      width="680px"
      destroy-on-close
    >
      <template v-if="current">
        <div class="detail-header">
          <div class="avatar large">{{ current.teacherName?.slice(-1) }}</div>
          <div class="detail-info">
            <div class="detail-name">
              {{ current.teacherName }}
              <el-tag v-if="current.title" size="small" type="info" style="margin-left:8px">{{ current.title }}</el-tag>
            </div>
            <div class="detail-sub" v-if="current.courseNames">
              <el-icon size="13"><Collection /></el-icon> {{ current.courseNames }}
            </div>
            <div class="detail-sub">
              <el-icon size="13"><Calendar /></el-icon> {{ current.semesterName }}
            </div>
          </div>
          <div class="score-badge large" :class="'badge-' + scoreTagType(current.avgScore)">
            <div class="score-val">{{ current.avgScore != null ? Number(current.avgScore).toFixed(2) : '--' }}</div>
            <div class="score-lbl">学期均分</div>
          </div>
        </div>

        <div class="detail-block green-block">
          <div class="block-title"><el-icon><EditPen /></el-icon> 教学反思</div>
          <div class="block-content">{{ current.reflection || '（暂无内容）' }}</div>
        </div>

        <div class="detail-block blue-block">
          <div class="block-title"><el-icon><Aim /></el-icon> 改进计划</div>
          <div class="block-content">{{ current.improvementPlan || '（暂无内容）' }}</div>
        </div>

        <div class="detail-time">提交时间：{{ current.updateTime }}</div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminFeedbackApi, adminSemesterApi } from '@/api/index'
import { Search, Collection, Calendar, EditPen, Aim } from '@element-plus/icons-vue'

const feedbacks      = ref([])
const semesters      = ref([])
const loading        = ref(false)
const dialogVisible  = ref(false)
const current        = ref(null)
const sortOrder      = ref('none')   // 'none' | 'desc' | 'asc'
const query       = reactive({ semesterId: null })
const keywordInput = ref('')   // 输入框草稿
const keyword      = ref('')   // 实际生效的关键字

const sortedFeedbacks = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = kw
    ? feedbacks.value.filter(fb => fb.teacherName?.toLowerCase().includes(kw))
    : [...feedbacks.value]
  if (sortOrder.value === 'none') return list
  return list.sort((a, b) => {
    const va = Number(a.avgScore) || 0
    const vb = Number(b.avgScore) || 0
    return sortOrder.value === 'desc' ? vb - va : va - vb
  })
})

function scoreTagType(v) {
  const n = Number(v)
  if (!v && v !== 0) return 'info'
  if (n >= 90) return 'success'
  if (n >= 75) return 'primary'
  if (n >= 60) return 'warning'
  return 'danger'
}

function openDetail(fb) { current.value = fb; dialogVisible.value = true }

function doSearch() {
  keyword.value = keywordInput.value
}

async function loadData() {
  if (!query.semesterId) { feedbacks.value = []; return }
  loading.value = true
  try {
    const res = await adminFeedbackApi.list({ semesterId: query.semesterId })
    feedbacks.value = res.data || []
  } finally { loading.value = false }
}

onMounted(async () => {
  const semRes = await adminSemesterApi.list()
  semesters.value = (semRes.data || []).sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
  if (semesters.value.length && !query.semesterId) {
    const cur = semesters.value.find(s => s.isCurrent === 1) || semesters.value[semesters.value.length - 1]
    query.semesterId = cur?.id
    loadData()
  }
})
</script>

<style scoped>
.page { padding: 20px; }

/* 页头 */
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
}
.page-title-row { display: flex; align-items: center; }
.page-title { font-size: 18px; font-weight: 700; color: #303133; }
.filter-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

/* 卡片网格 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
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
  display: flex; flex-direction: column; gap: 12px;
}
.fb-card:hover {
  box-shadow: 0 6px 20px rgba(0,0,0,0.10);
  transform: translateY(-2px);
}

/* 头部 */
.fb-head { display: flex; align-items: flex-start; gap: 12px; }
.avatar {
  width: 42px; height: 42px; border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #4f7ef8);
  color: #fff; font-size: 16px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.avatar.large { width: 52px; height: 52px; font-size: 20px; }
.meta { flex: 1; min-width: 0; }
.name-row { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 3px; }
.teacher-name { font-size: 15px; font-weight: 700; color: #303133; }
.course-row, .semester-row {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399; margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* 摘要 */
.fb-body { display: flex; flex-direction: column; gap: 8px; }
.section {}
.sec-label {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 600; margin-bottom: 3px;
}
.sec-label.green { color: #67c23a; }
.sec-label.blue  { color: #409eff; }
.excerpt {
  font-size: 13px; color: #909399; line-height: 1.6; margin: 0;
  display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}

/* 均分徽章 */
.score-badge {
  flex-shrink: 0; text-align: center;
  border-radius: 10px; padding: 6px 10px;
  min-width: 52px;
}
.score-badge.large { min-width: 64px; padding: 8px 14px; border-radius: 12px; }
.score-val { font-size: 18px; font-weight: 800; line-height: 1.2; }
.score-badge.large .score-val { font-size: 22px; }
.score-lbl { font-size: 11px; font-weight: 500; margin-top: 2px; }
.badge-success { background: #f0f9eb; color: #67c23a; border: 1px solid #b3e19d; }
.badge-primary  { background: #ecf5ff; color: #409eff; border: 1px solid #a0cfff; }
.badge-warning  { background: #fdf6ec; color: #e6a23c; border: 1px solid #f5dab1; }
.badge-danger   { background: #fef0f0; color: #f56c6c; border: 1px solid #fbc4c4; }
.badge-info     { background: #f4f4f5; color: #909399; border: 1px solid #d3d4d6; }

/* 底部 */
.fb-foot {
  display: flex; align-items: center; justify-content: space-between;
  border-top: 1px solid #f0f0f0; padding-top: 10px; margin-top: 2px;
}
.time { font-size: 12px; color: #c0c4cc; }
.view-link { font-size: 12px; color: #409eff; font-weight: 500; }

/* 弹窗 */
.detail-header {
  display: flex; align-items: center; gap: 14px;
  background: #f5f7fa; border-radius: 10px;
  padding: 14px 18px; margin-bottom: 20px;
}
.detail-info { flex: 1; min-width: 0; }
.detail-name { font-size: 17px; font-weight: 700; color: #303133; display: flex; align-items: center; flex-wrap: wrap; }
.detail-sub { font-size: 13px; color: #606266; margin-top: 4px; display: flex; align-items: center; gap: 5px; }

.detail-block { border-radius: 10px; padding: 14px 16px; margin-bottom: 14px; }
.green-block { background: #f0f9eb; border: 1px solid #b3e19d; }
.blue-block  { background: #ecf5ff; border: 1px solid #a0cfff; }
.block-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 700; margin-bottom: 8px;
}
.green-block .block-title { color: #67c23a; }
.blue-block  .block-title { color: #409eff; }
.block-content {
  font-size: 14px; color: #303133; line-height: 1.9; white-space: pre-wrap;
}

.detail-time { text-align: right; font-size: 12px; color: #c0c4cc; margin-top: 8px; }
</style>
