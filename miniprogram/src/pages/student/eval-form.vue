<template>
  <view class="page-bg" v-if="!submitted">
    <view class="form-header">
      <view class="back-btn" @click="goBack">‹ 返回</view>
      <text class="header-title">评价 {{ teacherName }}</text>
      <view class="score-badge">满分 {{ totalMaxScore }} 分</view>
    </view>

    <scroll-view scroll-y class="form-scroll">
      <view class="form-content">
        <!-- 前置问题 -->
        <view class="pre-block card">
          <view class="group-title pre-title">前置问题（不计入总分）</view>
          <view class="q-item">
            <text class="q-text">前1. 本学期您的上课出勤率大约是？</text>
            <view class="options-col">
              <view v-for="opt in attendanceOpts" :key="opt.value" class="option-row"
                    :class="{ selected: evalForm.attendanceRate === opt.value }"
                    @click="evalForm.attendanceRate = opt.value">
                <view class="radio-dot" :class="{ active: evalForm.attendanceRate === opt.value }"/>
                <text class="opt-text">{{ opt.label }}</text>
              </view>
            </view>
          </view>
          <view class="q-item">
            <text class="q-text">前2. 本学期您在这门课上投入的课外时间是？</text>
            <view class="options-col">
              <view v-for="opt in studyTimeOpts" :key="opt.value" class="option-row"
                    :class="{ selected: evalForm.studyTime === opt.value }"
                    @click="evalForm.studyTime = opt.value">
                <view class="radio-dot" :class="{ active: evalForm.studyTime === opt.value }"/>
                <text class="opt-text">{{ opt.label }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 单选题 -->
        <view v-for="(cat, ci) in categorizedQuestions" :key="cat.title" class="card">
          <view class="group-title">{{ ['一', '二', '三'][ci] }}、{{ cat.title }}</view>
          <view v-for="q in cat.questions" :key="q.id" class="q-item">
            <text class="q-text">
              {{ q.questionNo }}. {{ q.questionContent }}
              <text class="q-score-hint">（满分 {{ q.maxScore }} 分）</text>
            </text>
            <view class="options-grid">
              <view v-for="opt in buildOpts(q)" :key="opt.value" class="option-card"
                    :class="{ selected: evalForm.answers[q.id] === opt.value }"
                    @click="evalForm.answers[q.id] = opt.value">
                <text class="opt-lbl">{{ opt.label }}</text>
                <text class="opt-score">{{ opt.score }}分</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 简答题 -->
        <view class="card">
          <view class="group-title">四、综合评语（简答题）</view>
          <view v-for="q in essayQuestions" :key="q.id" class="q-item">
            <text class="q-text">
              {{ q.questionNo }}. {{ q.questionContent }}
              <text class="q-score-hint">（满分 {{ q.maxScore }} 分）</text>
            </text>
            <textarea
              v-model="evalForm.essayContent"
              class="essay-input"
              placeholder="请用简短的文字评价这位老师的教学（可空）..."
              maxlength="200"
            />
            <text class="word-count">{{ evalForm.essayContent.length }}/200</text>
          </view>
        </view>

        <view style="padding: 16rpx 0 40rpx;">
          <button class="btn-submit" :disabled="submitting" @click="submitEval">
            {{ submitting ? '提交中...' : '提交评价' }}
          </button>
          <button class="btn-cancel" @click="goBack">取消</button>
        </view>
      </view>
    </scroll-view>
  </view>

  <!-- 提交成功 -->
  <view v-else class="success-page">
    <text class="success-icon">✅</text>
    <text class="success-title">评价提交成功！</text>
    <text class="success-sub">感谢您对 {{ teacherName }} 老师的评价</text>
    <view class="success-btns">
      <view class="btn-go" @click="goBack">继续评价</view>
      <view class="btn-go outline" @click="goHistory">查看历史</view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { studentApi, questionApi } from '@/api/index'

const teacherName = ref('')
const teacherId   = ref(0)
const semesterId  = ref(0)

const submitted   = ref(false)
const submitting  = ref(false)
const questions   = ref([])

const evalForm = reactive({ answers: {}, essayContent: '', attendanceRate: null, studyTime: null })

const attendanceOpts = [
  { value: 1, label: '低于 50%' },
  { value: 2, label: '50% ~ 70%' },
  { value: 3, label: '70% ~ 85%' },
  { value: 4, label: '85% ~ 95%' },
  { value: 5, label: '95% 以上' },
]
const studyTimeOpts = [
  { value: 1, label: '几乎不投入' },
  { value: 2, label: '每周不足 1 小时' },
  { value: 3, label: '每周 1 ~ 3 小时' },
  { value: 4, label: '每周 3 ~ 5 小时' },
  { value: 5, label: '每周 5 小时以上' },
]

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
    questions: cat.nos.map(no => choiceQuestions.value.find(q => q.questionNo === no)).filter(Boolean)
  }))
)

function buildOpts(q) {
  const m = q.maxScore || 7.5
  return [
    { value: 1, label: '非常不符合', score: '0.0' },
    { value: 2, label: '不符合',     score: (m * 0.25).toFixed(1) },
    { value: 3, label: '一般',       score: (m * 0.50).toFixed(1) },
    { value: 4, label: '符合',       score: (m * 0.75).toFixed(1) },
    { value: 5, label: '非常符合',   score: m.toFixed(1) },
  ]
}

function goBack()    { uni.navigateBack() }
function goHistory() { uni.redirectTo({ url: '/pages/student/history' }) }

async function submitEval() {
  if (!evalForm.attendanceRate) {
    uni.showToast({ title: '请回答前置问题：出勤率', icon: 'none' }); return
  }
  if (!evalForm.studyTime) {
    uni.showToast({ title: '请回答前置问题：课外投入', icon: 'none' }); return
  }
  const answerList = Object.entries(evalForm.answers).map(([questionId, answerValue]) => ({
    questionId: Number(questionId), answerValue
  }))
  if (answerList.length < choiceQuestions.value.length) {
    uni.showToast({ title: '请回答所有单选题', icon: 'none' }); return
  }
  submitting.value = true
  try {
    await studentApi.submit({
      teacherId:      teacherId.value,
      semesterId:     semesterId.value,
      answers:        answerList,
      essayContent:   evalForm.essayContent,
      attendanceRate: evalForm.attendanceRate,
      studyTime:      evalForm.studyTime,
    })
    submitted.value = true
  } finally { submitting.value = false }
}

onMounted(async () => {
  // 在 onMounted 阶段页面已完全注册，此时 getCurrentPages 可安全获取路由参数
  const pages = getCurrentPages()
  const page  = pages[pages.length - 1]
  const opts  = page?.options || {}
  teacherName.value = decodeURIComponent(opts.teacherName || '')
  teacherId.value   = Number(opts.teacherId  || 0)
  semesterId.value  = Number(opts.semesterId || 0)
  uni.setNavigationBarTitle({ title: `评价 ${teacherName.value}` })

  try {
    const res = await questionApi.list()
    questions.value = res.data || []
  } catch {}
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; display: flex; flex-direction: column; }
.form-header {
  background: #1a2a6c; padding: 80rpx 32rpx 28rpx;
  display: flex; align-items: center; gap: 16rpx;
}
.back-btn     { color: rgba(255,255,255,0.8); font-size: 30rpx; }
.header-title { flex: 1; font-size: 32rpx; font-weight: 700; color: #fff; }
.score-badge  { background: rgba(255,255,255,0.2); color: #fff; font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 20rpx; }
.form-scroll  { flex: 1; }
.form-content { padding: 24rpx 24rpx 0; }

.pre-block { background: #fffbf0; border: 2rpx solid #fde9a0; }
.group-title {
  font-size: 28rpx; font-weight: 700; color: #1a2a6c;
  padding-bottom: 16rpx; border-bottom: 3rpx solid #1a2a6c; margin-bottom: 24rpx;
}
.pre-title { color: #b8660a; border-bottom-color: #e6a23c; }
.q-item { margin-bottom: 32rpx; }
.q-text { display: block; font-size: 28rpx; color: #303133; line-height: 1.6; margin-bottom: 16rpx; }
.q-score-hint { color: #909399; font-size: 24rpx; }

.options-col { display: flex; flex-direction: column; gap: 12rpx; }
.option-row {
  display: flex; align-items: center; gap: 16rpx;
  padding: 16rpx 20rpx; border-radius: 12rpx;
  border: 2rpx solid #ebeef5; background: #fff;
}
.option-row.selected { border-color: #1a2a6c; background: #ecf5ff; }
.radio-dot { width: 32rpx; height: 32rpx; border-radius: 50%; border: 3rpx solid #dcdfe6; flex-shrink: 0; }
.radio-dot.active { border-color: #1a2a6c; background: #1a2a6c; }
.opt-text { font-size: 26rpx; color: #303133; }

.options-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10rpx; }
.option-card {
  display: flex; flex-direction: column; align-items: center; padding: 12rpx 4rpx;
  border-radius: 12rpx; border: 2rpx solid #ebeef5; background: #fff; text-align: center;
}
.option-card.selected { border-color: #1a2a6c; background: linear-gradient(135deg, #1a2a6c, #2d6a9f); }
.option-card.selected .opt-lbl,
.option-card.selected .opt-score { color: #fff; }
.opt-lbl   { font-size: 18rpx; color: #606266; }
.opt-score { font-size: 18rpx; color: #909399; margin-top: 4rpx; }

.essay-input {
  width: 100%; height: 160rpx; background: #f5f7fa; border-radius: 12rpx;
  padding: 16rpx; font-size: 26rpx; color: #303133; box-sizing: border-box;
}
.word-count { font-size: 22rpx; color: #c0c4cc; float: right; margin-top: 8rpx; }

.btn-submit {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff; border-radius: 16rpx; height: 96rpx;
  font-size: 32rpx; font-weight: 600; border: none; width: 100%; margin-bottom: 16rpx;
}
.btn-submit:disabled { opacity: 0.6; }
.btn-cancel {
  background: #f5f7fa; color: #606266; border-radius: 16rpx;
  height: 88rpx; font-size: 28rpx; border: 2rpx solid #ebeef5; width: 100%;
}

.success-page {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 24rpx; padding: 80rpx 40rpx;
}
.success-icon  { font-size: 120rpx; }
.success-title { font-size: 40rpx; font-weight: 700; color: #303133; }
.success-sub   { font-size: 28rpx; color: #909399; }
.success-btns  { display: flex; gap: 24rpx; margin-top: 24rpx; }
.btn-go {
  padding: 20rpx 48rpx; background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff; border-radius: 16rpx; font-size: 28rpx;
}
.btn-go.outline { background: #fff; color: #1a2a6c; border: 2rpx solid #1a2a6c; }
</style>
