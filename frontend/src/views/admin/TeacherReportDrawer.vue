<template>
  <el-drawer
    v-model="visible"
    :title="report ? `${report.teacherName} · 教学质量分析报告` : '加载中...'"
    size="720px"
    direction="rtl"
    :before-close="handleClose"
    class="report-drawer"
  >
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="report" class="report-body">

      <!-- 基本信息 + 综合评级 -->
      <div class="info-banner">
        <div class="teacher-avatar">{{ report.teacherName?.charAt(0) }}</div>
        <div class="teacher-meta">
          <div class="teacher-name">{{ report.teacherName }}
            <el-tag size="small" style="margin-left:8px">{{ report.title || '教师' }}</el-tag>
          </div>
          <div class="teacher-sub">{{ report.department }}</div>
          <div class="teacher-sub">
            课程：{{ report.courses?.join('、') || '—' }}
            <span style="margin-left:12px">班级：{{ report.classes || '—' }}</span>
          </div>
        </div>
        <div class="score-block">
          <div class="score-num" :class="scoreClass(report.avgScore)">{{ report.avgScore?.toFixed(1) }}</div>
          <div class="score-label">综合评分</div>
          <el-tag :type="levelTagType(report.overallLevel)" size="large" style="margin-top:6px">
            {{ report.overallLevel || '—' }}
          </el-tag>
        </div>
      </div>

      <!-- 排名 & 评价人次 -->
      <el-row :gutter="16" class="summary-row">
        <el-col :span="8">
          <div class="summary-card">
            <div class="summary-val">{{ report.rank ?? '—' }} / {{ report.totalTeachers ?? '—' }}</div>
            <div class="summary-key">综合排名</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-card">
            <div class="summary-val">{{ report.evalCount ?? '—' }}</div>
            <div class="summary-key">评价人次</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-card">
            <div class="summary-val">{{ report.allAvgScore?.toFixed(1) ?? '—' }}</div>
            <div class="summary-key">全校平均分</div>
          </div>
        </el-col>
      </el-row>

      <!-- 各题均分 -->
      <section class="report-section">
        <div class="section-title"><span class="dot blue"></span>各题得分详情</div>
        <div v-for="q in report.questionAvg" :key="q.questionNo" class="question-row">
          <div class="q-label">Q{{ q.questionNo }}. {{ q.questionContent }}</div>
          <div class="q-bar-wrap">
            <el-progress
              :percentage="+(q.avgScore / q.maxScore * 100).toFixed(1)"
              :color="barColor(q.avgScore / q.maxScore)"
              :stroke-width="10"
              striped
            />
          </div>
          <div class="q-score">{{ q.avgScore?.toFixed(1) }} / {{ q.maxScore }}</div>
        </div>
      </section>

      <!-- 维度得分 -->
      <section v-if="report.dimensionScores && Object.keys(report.dimensionScores).length" class="report-section">
        <div class="section-title"><span class="dot green"></span>维度得分分析</div>
        <div v-for="(val, dim) in report.dimensionScores" :key="dim" class="question-row">
          <div class="q-label">{{ dim }}</div>
          <div class="q-bar-wrap">
            <el-progress
              :percentage="+(val * 100).toFixed(1)"
              :color="barColor(val)"
              :stroke-width="14"
            />
          </div>
          <div class="q-score">{{ (val * 100).toFixed(1) }}%</div>
        </div>
      </section>

      <!-- 优势 & 劣势 -->
      <el-row :gutter="16" class="report-section">
        <el-col :span="12">
          <div class="section-title"><span class="dot green"></span>优势维度</div>
          <div v-if="report.strengths?.length" class="tag-list">
            <el-tag
              v-for="s in report.strengths" :key="s"
              type="success" effect="light" class="dim-tag"
            >{{ s }}</el-tag>
          </div>
          <el-empty v-else description="暂无突出优势" :image-size="48" />
        </el-col>
        <el-col :span="12">
          <div class="section-title"><span class="dot orange"></span>待提升维度</div>
          <div v-if="report.weaknesses?.length" class="weakness-list">
            <div v-for="w in report.weaknesses" :key="w.dimension" class="weakness-item">
              <div class="weakness-name">{{ w.dimension }}</div>
              <div class="weakness-meta">
                得分率 <strong>{{ w.scoreRatio?.toFixed(1) }}%</strong>
                &nbsp;·&nbsp;距优秀差 <strong class="gap-num">{{ w.gap?.toFixed(1) }}%</strong>
                <el-tag size="small" type="warning" style="margin-left:6px">{{ w.severity }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="无明显短板" :image-size="48" />
        </el-col>
      </el-row>

      <!-- 改进建议 -->
      <section v-if="report.graphRecommendations?.length" class="report-section">
        <div class="section-title"><span class="dot purple"></span>改进建议</div>
        <el-collapse accordion>
          <el-collapse-item
            v-for="(rec, idx) in report.graphRecommendations"
            :key="idx"
            :title="`${rec.problem}（${rec.severity}）`"
            :name="idx"
          >
            <div v-if="rec.strategies?.length" class="strategies">
              <div class="strategy-label">建议策略：</div>
              <ul>
                <li v-for="s in rec.strategies" :key="s">{{ s }}</li>
              </ul>
            </div>
            <div v-if="rec.resources?.length" class="resources">
              <div class="strategy-label">推荐资源：</div>
              <div v-for="r in rec.resources" :key="r.url" class="resource-item">
                <el-tag size="small" type="info">{{ r.type }}</el-tag>
                <a :href="r.url" target="_blank" rel="noopener" class="resource-link">{{ r.title }}</a>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </section>

      <!-- 学生评语 -->
      <section v-if="report.comments?.length" class="report-section">
        <div class="section-title">
          <span class="dot blue"></span>学生评语
          <span class="comment-count">共 {{ report.comments.length }} 条</span>
        </div>
        <el-scrollbar max-height="280px">
          <div v-for="(c, i) in report.comments" :key="i" class="comment-item">
            <el-icon class="comment-icon"><ChatDotRound /></el-icon>
            <span class="comment-text">{{ c }}</span>
          </div>
        </el-scrollbar>
      </section>
    </div>

    <el-empty v-else description="暂无报告数据" />
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { adminAnalysisApi } from '@/api/index'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue:  { type: Boolean, default: false },
  teacherId:   { type: [Number, String], default: null },
  semesterId:  { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const report  = ref(null)

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.teacherId) {
    await fetchReport()
  }
})
watch(visible, (val) => {
  if (!val) emit('update:modelValue', false)
})

async function fetchReport() {
  loading.value = true
  report.value  = null
  try {
    const res = await adminAnalysisApi.teacherReport(props.teacherId, props.semesterId)
    report.value = res.data
  } catch {
    ElMessage.error('获取报告失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleClose(done) {
  emit('update:modelValue', false)
  done()
}

function scoreClass(s) {
  if (s >= 85) return 'score-excellent'
  if (s >= 70) return 'score-good'
  return 'score-poor'
}
function levelTagType(level) {
  if (!level) return 'info'
  if (level.includes('优')) return 'success'
  if (level.includes('良')) return ''
  if (level.includes('中')) return 'warning'
  return 'danger'
}
function barColor(ratio) {
  if (ratio >= 0.85) return '#67c23a'
  if (ratio >= 0.70) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.loading-wrap { padding: 20px; }
.report-body  { display: flex; flex-direction: column; gap: 0; }

/* 顶部信息横幅 */
.info-banner {
  display: flex; align-items: flex-start; gap: 16px;
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f5e9 100%);
  border-radius: 12px; padding: 20px 24px; margin-bottom: 16px;
}
.teacher-avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: white; font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.teacher-meta { flex: 1; }
.teacher-name { font-size: 18px; font-weight: 700; color: #1a2a6c; }
.teacher-sub  { font-size: 13px; color: #5a7ab0; margin-top: 4px; }
.score-block  { text-align: center; flex-shrink: 0; }
.score-num    { font-size: 36px; font-weight: 800; line-height: 1; }
.score-excellent { color: #67c23a; }
.score-good      { color: #e6a23c; }
.score-poor      { color: #f56c6c; }
.score-label  { font-size: 12px; color: #8c9ab0; margin-top: 4px; }

/* 排名摘要 */
.summary-row { margin-bottom: 20px; }
.summary-card {
  background: white; border-radius: 10px;
  padding: 14px; text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.summary-val { font-size: 22px; font-weight: 700; color: #1a2a6c; }
.summary-key { font-size: 12px; color: #8c9ab0; margin-top: 4px; }

/* Section 通用 */
.report-section { margin-bottom: 24px; }
.section-title {
  font-size: 14px; font-weight: 600; color: #303133;
  margin-bottom: 12px; display: flex; align-items: center; gap: 8px;
}
.dot {
  width: 8px; height: 8px; border-radius: 50%; display: inline-block;
}
.dot.blue   { background: #4f7ef8; }
.dot.green  { background: #67c23a; }
.dot.orange { background: #e6a23c; }
.dot.purple { background: #9254de; }

/* 各题进度条 */
.question-row {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
}
.q-label {
  width: 220px; font-size: 13px; color: #555;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; flex-shrink: 0;
}
.q-bar-wrap { flex: 1; }
.q-score {
  width: 68px; text-align: right; font-size: 13px;
  color: #303133; font-weight: 600; flex-shrink: 0;
}

/* 优势/劣势 */
.tag-list   { display: flex; flex-wrap: wrap; gap: 8px; }
.dim-tag    { max-width: 100%; white-space: normal; height: auto; padding: 4px 10px; line-height: 1.5; }
.weakness-list { display: flex; flex-direction: column; gap: 10px; }
.weakness-item {
  background: #fff9f0; border-left: 3px solid #e6a23c;
  border-radius: 6px; padding: 10px 14px;
}
.weakness-name { font-weight: 600; font-size: 13px; color: #303133; }
.weakness-meta { font-size: 12px; color: #888; margin-top: 4px; }
.gap-num       { color: #f56c6c; }

/* 改进建议 */
.strategies, .resources { margin: 8px 0; }
.strategy-label { font-size: 13px; font-weight: 600; color: #555; margin-bottom: 6px; }
.strategies ul  { margin: 0; padding-left: 20px; }
.strategies li  { font-size: 13px; color: #555; line-height: 1.8; }
.resource-item  { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.resource-link  { font-size: 13px; color: #4f7ef8; text-decoration: none; }
.resource-link:hover { text-decoration: underline; }

/* 评语 */
.comment-count { font-size: 12px; color: #8c9ab0; font-weight: 400; margin-left: 4px; }
.comment-item  {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 0; border-bottom: 1px solid #f0f0f0;
}
.comment-icon { color: #4f7ef8; margin-top: 2px; flex-shrink: 0; }
.comment-text { font-size: 13px; color: #555; line-height: 1.7; }
</style>
