<template>
  <div class="page">
    <!-- Step 1: 选择教师 / 查看历史 -->
    <div v-if="step === 1">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span class="title">{{ isCurrentSemester ? '选择评价教师' : '评价历史查看' }}</span>
            <el-select v-model="selectedSemester" placeholder="选择学期" style="width:220px"
                       @change="onSemesterChange">
              <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id">
                <span>{{ s.semesterName }}</span>
                <el-tag v-if="s.isCurrent === 1" size="small" type="success" style="margin-left:8px">当前</el-tag>
              </el-option>
            </el-select>
          </div>
        </template>

        <!-- ===== 当前学期：可评价 ===== -->
        <template v-if="isCurrentSemester">
          <el-form :inline="true" class="search-bar">
            <el-form-item>
              <el-input v-model="keyword" placeholder="搜索教师姓名/院系" clearable style="width:220px"
                        @input="loadTeachers" />
            </el-form-item>
          </el-form>

          <el-alert v-if="selectedSemester && teachers.length"
                    type="info" :closable="false" show-icon style="margin-bottom:12px">
            <template #title>
              以下 <b>{{ teachers.length }}</b> 位教师在本学期教授您所在班级的课程，点击可进行评价
            </template>
          </el-alert>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8" v-for="t in teachers" :key="t.userId">
              <div class="teacher-card" :class="{ 'evaluated': evaluatedIds.includes(t.userId) }"
                   @click="evaluatedIds.includes(t.userId) ? null : selectTeacher(t)">
                <div class="teacher-avatar">{{ t.realName?.[0] || 'T' }}</div>
                <div class="teacher-info">
                  <div class="t-name">{{ t.realName }}</div>
                  <div class="t-dept">{{ t.department }} · {{ t.title }}</div>
                  <div class="t-course">
                    <el-tag v-for="course in (t.courses || '').split('、').filter(Boolean)"
                            :key="course" size="small" type="primary" effect="plain"
                            style="margin:2px 2px 0 0">{{ course }}</el-tag>
                  </div>
                </div>
                <div class="teacher-status">
                  <template v-if="evaluatedIds.includes(t.userId)">
                    <div class="my-score">{{ getScore(t.userId) }}</div>
                    <div class="score-label">我的评分</div>
                    <el-button type="primary" link size="small" @click.stop="viewRecord(t)"
                               style="margin-top:4px">查看记录</el-button>
                  </template>
                  <el-button v-else type="primary" size="small" plain @click.stop="selectTeacher(t)">去评价</el-button>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-empty v-if="!teachers.length && selectedSemester"
                    description="该学期暂无可评价教师（或您的班级无对应课程）" />
          <el-empty v-else-if="!selectedSemester" description="请先选择学期" />
        </template>

        <!-- ===== 非当前学期：仅查看历史评价结果 ===== -->
        <template v-else-if="selectedSemester">
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
            <template #title>该学期已结束，仅可查看您提交的评价记录，无法再进行评价</template>
          </el-alert>

          <el-empty v-if="!historyRecords.length" description="该学期暂无评价记录" />
          <el-row v-else :gutter="16">
            <el-col :xs="24" :sm="12" :md="8" v-for="r in historyRecords" :key="r.teacherId">
              <div class="teacher-card evaluated">
                <div class="teacher-avatar">{{ r.teacherName?.[0] || 'T' }}</div>
                <div class="teacher-info">
                  <div class="t-name">{{ r.teacherName }}</div>
                  <div class="t-dept">{{ r.department }} · {{ r.title }}</div>
                  <div class="t-course">
                    <el-tag v-for="course in (r.courses || '').split('、').filter(Boolean)"
                            :key="course" size="small" type="primary" effect="plain"
                            style="margin:2px 2px 0 0">{{ course }}</el-tag>
                  </div>
                </div>
                <div class="teacher-status" style="text-align:center">
                  <div class="my-score">{{ r.totalScore != null ? Number(r.totalScore).toFixed(1) : '—' }}</div>
                  <div class="score-label">我的评分</div>
                  <el-button type="primary" link size="small" @click="viewRecordFromHistory(r)"
                             style="margin-top:4px">查看记录</el-button>
                </div>
              </div>
            </el-col>
          </el-row>
        </template>

        <el-empty v-else description="请先选择学期" />
      </el-card>
    </div>

    <!-- Step 2: 评价问卷 -->
    <div v-if="step === 2">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <el-button text @click="step = 1"><el-icon><Back /></el-icon> 返回选择</el-button>
            <span class="title">评价教师：{{ selectedTeacher?.realName }}</span>
            <el-tag type="info">满分 {{ totalMaxScore }} 分</el-tag>
          </div>
        </template>

        <!-- 前置问题（不计入总分，影响评价权重） -->
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom:20px">
          <template #title>
            以下 <b>2 道前置问题</b>不计入评分，但将影响本次评价在综合分析中的权重，请如实作答。
          </template>
        </el-alert>

        <div class="pre-question-block">
          <div class="question-group-title pre-title">前置问题</div>

          <div class="question-item">
            <div class="q-title">
              <span class="q-no pre-no">前1.</span>
              本学期您的上课出勤率大约是？
            </div>
            <el-radio-group v-model="evalForm.attendanceRate" class="option-group">
              <el-radio :value="1" border class="option-radio"><span>低于 50%</span></el-radio>
              <el-radio :value="2" border class="option-radio"><span>50% ~ 70%</span></el-radio>
              <el-radio :value="3" border class="option-radio"><span>70% ~ 85%</span></el-radio>
              <el-radio :value="4" border class="option-radio"><span>85% ~ 95%</span></el-radio>
              <el-radio :value="5" border class="option-radio"><span>95% 以上</span></el-radio>
            </el-radio-group>
          </div>

          <div class="question-item">
            <div class="q-title">
              <span class="q-no pre-no">前2.</span>
              本学期您在这门课上投入的课外时间是？
            </div>
            <el-radio-group v-model="evalForm.studyTime" class="option-group">
              <el-radio :value="1" border class="option-radio"><span>几乎不投入</span></el-radio>
              <el-radio :value="2" border class="option-radio"><span>每周不足 1 小时</span></el-radio>
              <el-radio :value="3" border class="option-radio"><span>每周 1 ~ 3 小时</span></el-radio>
              <el-radio :value="4" border class="option-radio"><span>每周 3 ~ 5 小时</span></el-radio>
              <el-radio :value="5" border class="option-radio"><span>每周 5 小时以上</span></el-radio>
            </el-radio-group>
          </div>
        </div>

        <el-form ref="formRef" :model="evalForm" label-position="top" v-loading="questionsLoading">
          <!-- 单选题（按分类展示） -->
          <template v-for="(cat, catIdx) in categorizedQuestions" :key="cat.title">
            <div class="question-group-title">
              {{ ['一', '二', '三'][catIdx] }}、{{ cat.title }}
            </div>
            <div v-for="q in cat.questions" :key="q.id" class="question-item">
              <div class="q-title">
                <span class="q-no">{{ q.questionNo }}.</span>
                {{ q.questionContent }}
                <span class="q-score">（满分 {{ q.maxScore }} 分）</span>
              </div>
              <el-form-item :prop="`answers.${q.id}`"
                            :rules="[{ required: true, message: '请选择选项', trigger: 'change' }]">
                <el-radio-group v-model="evalForm.answers[q.id]" class="option-group">
                  <el-radio :value="1" border class="option-radio">
                    <span>非常不符合</span><span class="o-score">{{ ((q.maxScore||7.5)*0).toFixed(1) }}分</span>
                  </el-radio>
                  <el-radio :value="2" border class="option-radio">
                    <span>不符合</span><span class="o-score">{{ ((q.maxScore||7.5)*0.25).toFixed(1) }}分</span>
                  </el-radio>
                  <el-radio :value="3" border class="option-radio">
                    <span>一般</span><span class="o-score">{{ ((q.maxScore||7.5)*0.5).toFixed(1) }}分</span>
                  </el-radio>
                  <el-radio :value="4" border class="option-radio">
                    <span>符合</span><span class="o-score">{{ ((q.maxScore||7.5)*0.75).toFixed(1) }}分</span>
                  </el-radio>
                  <el-radio :value="5" border class="option-radio">
                    <span>非常符合</span><span class="o-score">{{ (q.maxScore||7.5) }}分</span>
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </div>
          </template>

          <!-- 简答题 -->
          <div class="question-group-title">四、简答题</div>
          <div v-for="q in essayQuestions" :key="q.id" class="question-item">
            <div class="q-title">
              <span class="q-no">{{ q.questionNo }}.</span>
              {{ q.questionContent }}
              <span class="q-score">（满分 {{ q.maxScore }} 分）</span>
            </div>
            <el-form-item>
              <el-input
                v-model="evalForm.essayContent"
                type="textarea"
                :rows="3"
                placeholder="请用不超过50个字简单评价这位老师..."
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </div>

          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="submitEval"
                       style="min-width:160px">
              <el-icon><Check /></el-icon> 提交评价
            </el-button>
            <el-button @click="step = 1">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- Step 3: 提交成功 -->
    <!-- 作答记录详情弹窗 -->
    <el-dialog v-model="recordDetailVisible" :title="`评价记录 - ${recordDetail?.teacherName}`" width="640px">
      <div v-if="recordDetail" v-loading="recordDetailLoading">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="学期">{{ recordDetail.semesterName }}</el-descriptions-item>
          <el-descriptions-item label="总分">
            <el-tag type="success">{{ recordDetail.totalScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="单选题分">{{ recordDetail.choiceScore }}</el-descriptions-item>
          <el-descriptions-item label="简答题分">{{ recordDetail.essayScore }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title">各题作答</div>
        <div v-for="a in recordDetail.answers" :key="a.questionNo" class="answer-row">
          <div class="q-row">{{ a.questionNo }}. {{ a.questionContent }}</div>
          <div class="a-row">
            <el-tag size="small" type="info">{{ a.answerLabel }}</el-tag>
            <span class="a-score">{{ a.score }} 分</span>
          </div>
        </div>
        <div v-if="recordDetail.essayContent" class="section-title">我的评语</div>
        <div v-if="recordDetail.essayContent" class="essay-content">{{ recordDetail.essayContent }}</div>
      </div>
    </el-dialog>

    <div v-if="step === 3">
      <el-card shadow="never" style="text-align:center; padding:40px">
        <el-result icon="success" title="评价提交成功！" :sub-title="`感谢您对 ${selectedTeacher?.realName} 老师的评价`">
          <template #extra>
            <el-button type="primary" @click="step = 1">继续评价</el-button>
            <el-button @click="$router.push('/student/history')">查看历史</el-button>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { teacherApi, studentApi, questionApi } from '@/api/index'
import { ElMessage } from 'element-plus'

const step             = ref(1)
const semesters        = ref([])
const selectedSemester = ref(null)
const teachers         = ref([])
const historyRecords   = ref([])   // 历史学期评价记录
const evaluatedIds     = ref([])
const evalScoreMap     = ref({})   // teacherId -> totalScore（当前学期已评价的分数）
const recordDetailVisible = ref(false)
const recordDetailLoading = ref(false)
const recordDetail     = ref(null)
const selectedTeacher  = ref(null)
const questions        = ref([])
const questionsLoading = ref(false)
const submitting       = ref(false)
const keyword          = ref('')
const formRef          = ref()

/** 当前选中的学期对象 */
const selectedSemesterObj = computed(() =>
  semesters.value.find(s => s.id === selectedSemester.value) || null
)
/** 是否为当前学期（isCurrent=1） */
const isCurrentSemester = computed(() =>
  selectedSemesterObj.value?.isCurrent === 1
)

const evalForm = reactive({ answers: {}, essayContent: '', attendanceRate: null, studyTime: null })

const choiceQuestions = computed(() => questions.value.filter(q => q.questionType === 1))
const essayQuestions  = computed(() => questions.value.filter(q => q.questionType === 2))
const totalMaxScore   = computed(() => questions.value.reduce((s, q) => s + (q.maxScore || 0), 0))

const questionCategories = [
  { title: '教学内容与方法', nos: [1, 2, 3, 4] },
  { title: '教学态度与互动', nos: [5, 6, 7, 8] },
  { title: '教学效果与收获', nos: [9, 10, 11, 12] },
]

const categorizedQuestions = computed(() =>
  questionCategories.map(cat => ({
    ...cat,
    questions: cat.nos
      .map(no => choiceQuestions.value.find(q => q.questionNo === no))
      .filter(Boolean),
  }))
)

function getScore(teacherId) {
  const s = evalScoreMap.value[teacherId]
  return s != null ? Number(s).toFixed(1) : '—'
}

async function viewRecord(teacher) {
  if (!selectedSemester.value) return
  recordDetailVisible.value = true
  recordDetail.value = null
  recordDetailLoading.value = true
  try {
    const res = await studentApi.evalRecordDetail(teacher.userId, selectedSemester.value)
    recordDetail.value = res.data
  } finally {
    recordDetailLoading.value = false
  }
}

async function viewRecordFromHistory(r) {
  if (!selectedSemester.value) return
  recordDetailVisible.value = true
  recordDetail.value = null
  recordDetailLoading.value = true
  try {
    const res = await studentApi.evalRecordDetail(r.teacherId, selectedSemester.value)
    recordDetail.value = res.data
  } finally {
    recordDetailLoading.value = false
  }
}

/** 学期切换时根据 isCurrent 决定加载模式 */
async function onSemesterChange() {
  teachers.value = []
  historyRecords.value = []
  evaluatedIds.value = []
  evalScoreMap.value = {}
  if (!selectedSemester.value) return
  if (isCurrentSemester.value) {
    await loadTeachers()
  } else {
    await loadHistory()
  }
}

async function loadTeachers() {
  if (!selectedSemester.value) { teachers.value = []; return }
  const res = await teacherApi.listForStudent(selectedSemester.value, keyword.value)
  teachers.value = res.data || []
  await checkEvaluated()
}

async function loadHistory() {
  if (!selectedSemester.value) { historyRecords.value = []; return }
  const res = await studentApi.historyBySemester(selectedSemester.value)
  historyRecords.value = res.data || []
}

async function checkEvaluated() {
  if (!selectedSemester.value) return
  const hRes = await studentApi.history()
  const history = hRes.data || []
  const forSemester = history.filter(r => r.semesterId === selectedSemester.value)
  evaluatedIds.value = forSemester.map(r => r.teacherId)
  const map = {}
  forSemester.forEach(r => { map[r.teacherId] = r.totalScore })
  evalScoreMap.value = map
}

async function selectTeacher(teacher) {
  if (evaluatedIds.value.includes(teacher.userId)) {
    ElMessage.warning('您已对该教师完成本学期评价')
    return
  }
  if (!selectedSemester.value) {
    ElMessage.warning('请先选择学期')
    return
  }
  const canRes = await studentApi.canEvaluate(teacher.userId, selectedSemester.value)
  if (!canRes.data) {
    ElMessage.warning('您已对该教师完成本学期评价，不能重复提交')
    return
  }
  selectedTeacher.value = teacher
  evalForm.answers = {}
  evalForm.essayContent = ''
  evalForm.attendanceRate = null
  evalForm.studyTime = null
  step.value = 2
  questionsLoading.value = true
  try {
    const res = await questionApi.list()
    questions.value = res.data || []
  } finally { questionsLoading.value = false }
}

async function submitEval() {
  if (!evalForm.attendanceRate) {
    ElMessage.warning('请回答前置问题：出勤率')
    return
  }
  if (!evalForm.studyTime) {
    ElMessage.warning('请回答前置问题：课外投入时间')
    return
  }
  await formRef.value.validate()
  const answerList = Object.entries(evalForm.answers).map(([questionId, answerValue]) => ({
    questionId: Number(questionId),
    answerValue,
  }))
  if (answerList.length < choiceQuestions.value.length) {
    ElMessage.warning('请回答所有单选题')
    return
  }
  submitting.value = true
  try {
    await studentApi.submit({
      teacherId:      selectedTeacher.value.userId,
      semesterId:     selectedSemester.value,
      answers:        answerList,
      essayContent:   evalForm.essayContent,
      attendanceRate: evalForm.attendanceRate,
      studyTime:      evalForm.studyTime,
    })
    step.value = 3
    evaluatedIds.value.push(selectedTeacher.value.userId)
  } finally { submitting.value = false }
}

onMounted(async () => {
  const semRes = await studentApi.allSemesters()
  semesters.value = semRes.data || []
  // 优先选当前学期，其次最新学期
  const current = semesters.value.find(s => s.isCurrent === 1)
  selectedSemester.value = current?.id ?? semesters.value[semesters.value.length - 1]?.id
  onSemesterChange()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.title { font-size: 16px; font-weight: 600; }
.search-bar { margin-bottom: 16px; }
.teacher-card {
  background: white;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 16px;
}
.teacher-card:hover { border-color: #409eff; box-shadow: 0 4px 16px rgba(64,158,255,0.15); transform: translateY(-2px); }
.teacher-card.evaluated { opacity: 0.7; }
.teacher-avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: white; font-size: 20px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.teacher-info { flex: 1; overflow: hidden; }
.t-name   { font-size: 16px; font-weight: 600; color: #303133; }
.t-dept   { font-size: 13px; color: #606266; margin: 2px 0; }
.t-course { font-size: 12px; color: #8c9ab0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pre-question-block {
  background: #fffbf0;
  border: 1px solid #fde9a0;
  border-radius: 10px;
  padding: 16px 20px 8px;
  margin-bottom: 28px;
}
.pre-title { border-bottom-color: #e6a23c !important; color: #b8660a !important; }
.pre-no { color: #e6a23c !important; }
.question-group-title {
  font-size: 16px; font-weight: 700; color: #1a2a6c;
  padding: 16px 0 8px; border-bottom: 2px solid #1a2a6c;
  margin-bottom: 20px;
}
.question-item { margin-bottom: 28px; }
.q-title { font-size: 15px; color: #303133; margin-bottom: 14px; line-height: 1.6; }
.q-no    { font-weight: 700; color: #1a2a6c; margin-right: 6px; }
.q-score { color: #8c9ab0; font-size: 13px; }
.option-group { display: flex; flex-wrap: wrap; gap: 10px; }
.option-radio {
  display: flex; flex-direction: column; align-items: center;
  padding: 10px 14px; min-width: 90px; height: auto;
  border-radius: 8px;
}
.option-radio :deep(.el-radio__label) { display: flex; flex-direction: column; align-items: center; }
.o-score { font-size: 12px; color: #8c9ab0; margin-top: 2px; }
.teacher-status .my-score { font-size: 18px; font-weight: 700; color: #67c23a; }
.teacher-status .score-label { font-size: 11px; color: #909399; margin-top: 2px; }
.section-title { font-size: 14px; font-weight: 600; color: #303133; margin: 16px 0 8px; }
.answer-row { margin-bottom: 12px; padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.answer-row:last-of-type { border-bottom: none; }
.q-row { font-size: 13px; color: #606266; margin-bottom: 4px; }
.a-row { display: flex; align-items: center; gap: 8px; }
.a-score { font-size: 13px; color: #67c23a; font-weight: 600; }
.essay-content { padding: 10px; background: #f5f7fa; border-radius: 8px; font-size: 14px; color: #303133; }
</style>
