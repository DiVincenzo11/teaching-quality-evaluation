<template>
  <view class="page-bg">
    <nav-bar title="评价教师" :subtitle="currentSemesterName" />

    <view class="content">
      <view class="card">
        <view class="sem-row">
          <text class="sem-label">选择学期</text>
          <picker mode="selector" :range="semesterLabels" :value="semIndex" @change="onSemesterChange">
            <view class="picker-btn">
              <text>{{ semesterLabels[semIndex] || '请选择' }}</text>
              <text class="arrow">›</text>
            </view>
          </picker>
        </view>
      </view>

      <view v-if="isCurrentSemester" class="card search-card">
        <view class="search-wrap">
          <text class="search-icon">🔍</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索教师姓名/院系"
            placeholder-class="ph"
            @input="loadTeachers"
          />
          <text v-if="keyword" class="clear-btn" @click="clearSearch">✕</text>
        </view>
      </view>

      <template v-if="isCurrentSemester">
        <view v-if="loadingTeachers" class="loading-box"><text>加载中...</text></view>
        <view v-else-if="!teachers.length" class="empty-box">
          <text class="empty-icon">📭</text>
          <text>该学期暂无可评价教师</text>
        </view>
        <view v-for="t in teachers" :key="t.userId" class="teacher-card"
              :class="{ evaluated: evaluatedIds.includes(t.userId) }">
          <view class="tc-left">
            <view class="avatar-circle">{{ t.realName?.[0] || 'T' }}</view>
          </view>
          <view class="tc-body">
            <text class="tc-name">{{ t.realName }}</text>
            <text class="tc-dept">{{ t.department }} · {{ t.title }}</text>
            <view class="tc-courses">
              <view v-for="c in (t.courses || '').split('、').filter(Boolean)" :key="c" class="course-tag">
                {{ c }}
              </view>
            </view>
          </view>
          <view class="tc-right">
            <template v-if="evaluatedIds.includes(t.userId)">
              <text class="score-big">{{ getScore(t.userId) }}</text>
              <text class="score-lbl">我的评分</text>
              <text class="view-link" @click="viewRecord(t)">查看记录</text>
            </template>
            <view v-else class="eval-btn" @click="selectTeacher(t)">去评价</view>
          </view>
        </view>
      </template>

      <template v-else-if="selectedSemester">
        <view class="warn-banner">📌 该学期已结束，仅可查看您提交的评价记录</view>
        <view v-if="!historyRecords.length" class="empty-box">
          <text class="empty-icon">📭</text>
          <text>该学期暂无评价记录</text>
        </view>
        <view v-for="r in historyRecords" :key="r.teacherId" class="teacher-card evaluated">
          <view class="tc-left">
            <view class="avatar-circle">{{ r.teacherName?.[0] || 'T' }}</view>
          </view>
          <view class="tc-body">
            <text class="tc-name">{{ r.teacherName }}</text>
            <text class="tc-dept">{{ r.department }} · {{ r.title }}</text>
          </view>
          <view class="tc-right">
            <text class="score-big">{{ r.totalScore != null ? Number(r.totalScore).toFixed(1) : '—' }}</text>
            <text class="score-lbl">我的评分</text>
            <text class="view-link" @click="viewRecordFromHistory(r)">查看记录</text>
          </view>
        </view>
      </template>

      <view v-else class="empty-box"><text>请先选择学期</text></view>
    </view>

    <!-- 评价记录详情 -->
    <view v-if="detailVisible" class="modal-mask">
      <view class="modal-overlay" @click="detailVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">评价记录 — {{ recordDetail?.teacherName }}</text>
          <text class="modal-close" @click="detailVisible = false">✕</text>
        </view>
        <scroll-view scroll-y style="max-height: 65vh;" v-if="recordDetail">
          <view style="padding: 32rpx 40rpx;">
            <view class="detail-row"><text class="dl">学期</text><text class="dv">{{ recordDetail.semesterName }}</text></view>
            <view class="detail-row"><text class="dl">总分</text><view class="tag tag-success">{{ recordDetail.totalScore }}</view></view>
            <view class="detail-row"><text class="dl">单选题分</text><text class="dv">{{ recordDetail.choiceScore }}</text></view>
            <view class="detail-row"><text class="dl">简答题分</text><text class="dv">{{ recordDetail.essayScore }}</text></view>
            <view class="section-title">各题作答</view>
            <view v-for="a in recordDetail.answers" :key="a.questionNo" class="answer-row">
              <text class="ar-q">{{ a.questionNo }}. {{ a.questionContent }}</text>
              <view class="ar-bottom">
                <view class="tag tag-info">{{ a.answerLabel }}</view>
                <text class="ar-score">{{ a.score }} 分</text>
              </view>
            </view>
            <template v-if="recordDetail.essayContent">
              <view class="section-title">我的评语</view>
              <view class="essay-box">{{ recordDetail.essayContent }}</view>
            </template>
          </view>
        </scroll-view>
        <view v-else class="loading-box"><text>加载中...</text></view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/student/evaluate" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherApi, studentApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const semesters        = ref([])
const semIndex         = ref(0)
const selectedSemester = ref(null)
const teachers         = ref([])
const historyRecords   = ref([])
const evaluatedIds     = ref([])
const evalScoreMap     = ref({})
const keyword          = ref('')
const loadingTeachers  = ref(false)
const detailVisible    = ref(false)
const recordDetail     = ref(null)

const semesterLabels    = computed(() => semesters.value.map(s => s.semesterName))
const selectedSemObj    = computed(() => semesters.value[semIndex.value] || null)
const isCurrentSemester = computed(() => selectedSemObj.value?.isCurrent === 1)
const currentSemesterName = computed(() => selectedSemObj.value?.semesterName || '')

function getScore(tid) {
  const s = evalScoreMap.value[tid]
  return s != null ? Number(s).toFixed(1) : '—'
}

async function onSemesterChange(e) {
  semIndex.value = e.detail.value
  selectedSemester.value = semesters.value[semIndex.value]?.id
  teachers.value = []; historyRecords.value = []; evaluatedIds.value = []; evalScoreMap.value = {}
  if (!selectedSemester.value) return
  if (isCurrentSemester.value) await loadTeachers()
  else await loadHistory()
}

async function loadTeachers() {
  if (!selectedSemester.value) return
  loadingTeachers.value = true
  try {
    const res = await teacherApi.listForStudent(selectedSemester.value, keyword.value)
    teachers.value = res.data || []
    await checkEvaluated()
  } finally { loadingTeachers.value = false }
}

async function loadHistory() {
  if (!selectedSemester.value) return
  const res = await studentApi.historyBySemester(selectedSemester.value)
  historyRecords.value = res.data || []
}

async function checkEvaluated() {
  const hRes = await studentApi.history()
  const history = hRes.data || []
  const forSem = history.filter(r => r.semesterId === selectedSemester.value)
  evaluatedIds.value = forSem.map(r => r.teacherId)
  const map = {}
  forSem.forEach(r => { map[r.teacherId] = r.totalScore })
  evalScoreMap.value = map
}

function clearSearch() { keyword.value = ''; loadTeachers() }

async function selectTeacher(teacher) {
  if (evaluatedIds.value.includes(teacher.userId)) {
    uni.showToast({ title: '您已对该教师完成本学期评价', icon: 'none' }); return
  }
  const canRes = await studentApi.canEvaluate(teacher.userId, selectedSemester.value)
  if (!canRes.data) {
    uni.showToast({ title: '无法重复评价', icon: 'none' }); return
  }
  uni.navigateTo({
    url: `/pages/student/eval-form?teacherId=${teacher.userId}&teacherName=${encodeURIComponent(teacher.realName)}&semesterId=${selectedSemester.value}`
  })
}

async function viewRecord(teacher) {
  detailVisible.value = true; recordDetail.value = null
  try {
    const res = await studentApi.evalRecordDetail(teacher.userId, selectedSemester.value)
    recordDetail.value = res.data
  } catch {}
}

async function viewRecordFromHistory(r) {
  detailVisible.value = true; recordDetail.value = null
  try {
    const res = await studentApi.evalRecordDetail(r.teacherId, selectedSemester.value)
    recordDetail.value = res.data
  } catch {}
}

onMounted(async () => {
  const semRes = await studentApi.allSemesters()
  semesters.value = semRes.data || []
  const cur = semesters.value.findIndex(s => s.isCurrent === 1)
  semIndex.value = cur >= 0 ? cur : semesters.value.length - 1
  selectedSemester.value = semesters.value[semIndex.value]?.id
  if (isCurrentSemester.value) loadTeachers()
  else loadHistory()
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content   { padding: 24rpx; }

.sem-row { display: flex; justify-content: space-between; align-items: center; }
.sem-label { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }

.search-card { padding: 16rpx 28rpx; }
.search-wrap { display: flex; align-items: center; gap: 12rpx; background: #f5f7fa; border-radius: 40rpx; padding: 16rpx 24rpx; }
.search-icon  { font-size: 28rpx; }
.search-input { flex: 1; font-size: 26rpx; color: #303133; background: transparent; }
.ph { color: #c0c4cc; font-size: 26rpx; }
.clear-btn { font-size: 24rpx; color: #c0c4cc; padding: 4rpx; }

.warn-banner {
  background: #fdf6ec; border: 2rpx solid #fde9a0;
  border-radius: 12rpx; padding: 20rpx 24rpx; margin-bottom: 20rpx;
  font-size: 26rpx; color: #b8660a;
}
.teacher-card {
  background: #fff; border-radius: 20rpx; padding: 28rpx;
  margin-bottom: 20rpx; display: flex; gap: 20rpx; align-items: flex-start;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
}
.teacher-card.evaluated { opacity: 0.85; }
.tc-left { flex-shrink: 0; }
.tc-body { flex: 1; overflow: hidden; }
.tc-name { display: block; font-size: 32rpx; font-weight: 600; color: #303133; margin-bottom: 6rpx; }
.tc-dept { display: block; font-size: 24rpx; color: #909399; margin-bottom: 10rpx; }
.tc-courses { display: flex; flex-wrap: wrap; gap: 8rpx; }
.course-tag { background: #ecf5ff; color: #409eff; font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
.tc-right { display: flex; flex-direction: column; align-items: center; gap: 6rpx; flex-shrink: 0; }
.score-big  { font-size: 36rpx; font-weight: 700; color: #67c23a; }
.score-lbl  { font-size: 20rpx; color: #909399; }
.view-link  { font-size: 22rpx; color: #409eff; }
.eval-btn {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff; border-radius: 12rpx; padding: 14rpx 24rpx; font-size: 24rpx;
}

.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box  { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; max-height: 80vh; display: flex; flex-direction: column; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.dl { font-size: 26rpx; color: #909399; }
.dv { font-size: 26rpx; color: #303133; font-weight: 500; }
.section-title { font-size: 28rpx; font-weight: 600; color: #303133; margin: 24rpx 0 16rpx; }
.answer-row { padding: 16rpx 0; border-bottom: 2rpx dashed #f0f0f0; }
.ar-q { display: block; font-size: 24rpx; color: #606266; margin-bottom: 10rpx; }
.ar-bottom { display: flex; align-items: center; gap: 16rpx; }
.ar-score { font-size: 24rpx; color: #67c23a; font-weight: 600; }
.essay-box { background: #f5f7fa; border-radius: 12rpx; padding: 20rpx; font-size: 26rpx; color: #303133; line-height: 1.8; }
</style>



