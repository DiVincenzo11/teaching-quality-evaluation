<template>
  <view class="page-bg">
    <nav-bar title="教学反思" subtitle="记录本学期教学心得与改进计划" />
    <view class="content">
      <!-- 学期选择 -->
      <view class="card filter-row">
        <text class="fl">选择学期</text>
        <picker mode="selector" :range="semLabels" :value="semIndex" @change="onSemChange">
          <view class="picker-btn">
            <text>{{ semLabels[semIndex] || '请选择' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <!-- 当前分数 -->
      <view class="card score-card" v-if="statsData">
        <view class="sc-item">
          <text class="sc-val green">{{ statsData.avgScore ?? '--' }}</text>
          <text class="sc-lbl">本学期均分</text>
        </view>
        <view class="sc-divider" />
        <view class="sc-item">
          <text class="sc-val blue">{{ statsData.evalCount ?? '--' }}</text>
          <text class="sc-lbl">评价人次</text>
        </view>
        <view class="sc-divider" />
        <view class="sc-item">
          <text class="sc-val orange">{{ statsData.rankInDept != null ? '#' + statsData.rankInDept : '--' }}</text>
          <text class="sc-lbl">院系排名</text>
        </view>
      </view>

      <!-- 提交反思表单 -->
      <view class="card">
        <view class="card-title">{{ editMode ? '编辑反思' : '提交教学反思' }}</view>

        <view class="form-section">
          <text class="form-section-title">教学反思</text>
          <textarea
            v-model="feedbackForm.reflection"
            class="textarea-input"
            placeholder="请填写本学期的教学反思，包括教学方法、师生互动、改进点等..."
            maxlength="1000"
          />
          <text class="word-count">{{ feedbackForm.reflection?.length || 0 }}/1000</text>
        </view>

        <view class="form-section">
          <text class="form-section-title">改进计划</text>
          <textarea
            v-model="feedbackForm.improvementPlan"
            class="textarea-input"
            placeholder="请填写下学期的改进计划和目标..."
            maxlength="500"
          />
          <text class="word-count">{{ feedbackForm.improvementPlan?.length || 0 }}/500</text>
        </view>

        <button class="btn-submit" :disabled="submitting" @click="saveFeedback">
          {{ submitting ? '提交中...' : (editMode ? '更新反思' : '提交反思') }}
        </button>
      </view>

      <!-- 历史反思列表 -->
      <view class="card" v-if="historyList.length">
        <view class="card-title">历史反思记录</view>
        <view v-for="fb in historyList" :key="fb.id" class="history-item" @click="fillEdit(fb)">
          <view class="hi-header">
            <text class="hi-sem">{{ fb.semesterName }}</text>
            <view class="hi-badge" :class="fb.improvementPlan ? 'badge-complete' : 'badge-partial'">
              {{ fb.improvementPlan ? '完整' : '仅反思' }}
            </view>
          </view>
          <text class="hi-preview">{{ truncate(fb.reflection) }}</text>
          <text class="hi-time">{{ fb.updateTime?.slice(0, 10) }}</text>
        </view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/teacher/feedback" />
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { teacherApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const semesters   = ref([])
const semIndex    = ref(0)
const statsData   = ref(null)
const historyList = ref([])
const submitting  = ref(false)
const editMode    = ref(false)

const feedbackForm = reactive({ reflection: '', improvementPlan: '' })

const semLabels   = computed(() => semesters.value.map(s => s.semesterName))
const selectedSem = computed(() => semesters.value[semIndex.value]?.id)

function truncate(s) { if (!s) return '暂无内容'; return s.length > 60 ? s.slice(0, 60) + '...' : s }

async function onSemChange(e) {
  semIndex.value = e.detail.value
  editMode.value = false
  feedbackForm.reflection = ''
  feedbackForm.improvementPlan = ''
  await loadStats()
}

async function loadStats() {
  try {
    const res = await teacherApi.evalStats(selectedSem.value)
    statsData.value = res.data?.evalCount > 0 ? res.data : null
  } catch {}
}

function fillEdit(fb) {
  editMode.value = true
  feedbackForm.reflection     = fb.reflection || ''
  feedbackForm.improvementPlan = fb.improvementPlan || ''
  uni.showToast({ title: '已填入历史反思，可编辑后提交', icon: 'none', duration: 2000 })
}

async function saveFeedback() {
  if (!feedbackForm.reflection?.trim()) {
    uni.showToast({ title: '请填写教学反思内容', icon: 'none' }); return
  }
  submitting.value = true
  try {
    await teacherApi.saveFeedback({
      semesterId:      selectedSem.value,
      reflection:      feedbackForm.reflection.trim(),
      improvementPlan: feedbackForm.improvementPlan?.trim() || '',
    })
    uni.showToast({ title: '提交成功', icon: 'success' })
    editMode.value = false
    await loadHistory()
  } finally { submitting.value = false }
}

async function loadHistory() {
  try {
    const res = await teacherApi.myFeedback()
    historyList.value = res.data || []
  } catch {}
}

onMounted(async () => {
  try {
    const semRes = await teacherApi.semesters()
    semesters.value = semRes.data || []
    const cur = semesters.value.findIndex(s => s.isCurrent === 1)
    semIndex.value = cur >= 0 ? cur : 0
  } catch {}
  await Promise.allSettled([loadStats(), loadHistory()])
})
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.content   { padding: 24rpx; margin-top: -16rpx; }

.filter-row { display: flex; justify-content: space-between; align-items: center; }
.fl { font-size: 28rpx; color: #606266; }
.picker-btn { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #1a2a6c; font-weight: 500; }
.arrow { color: #c0c4cc; }

.score-card { display: flex; align-items: center; padding: 24rpx 32rpx; }
.sc-item    { flex: 1; text-align: center; }
.sc-val     { display: block; font-size: 36rpx; font-weight: 700; margin-bottom: 6rpx; }
.sc-lbl     { display: block; font-size: 22rpx; color: #909399; }
.green  { color: #11998e; }
.blue   { color: #1a2a6c; }
.orange { color: #e6a23c; }
.sc-divider { width: 2rpx; height: 60rpx; background: #f0f0f0; }

.form-section { margin-bottom: 24rpx; }
.form-section-title {
  display: block; font-size: 28rpx; font-weight: 600; color: #303133;
  margin-bottom: 12rpx;
}
.textarea-input {
  width: 100%; background: #f5f7fa; border-radius: 12rpx;
  padding: 20rpx; font-size: 28rpx; color: #303133;
  min-height: 160rpx; box-sizing: border-box; line-height: 1.7;
}
.word-count { display: block; font-size: 22rpx; color: #c0c4cc; text-align: right; margin-top: 6rpx; }

.btn-submit {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff; border-radius: 16rpx; height: 96rpx;
  font-size: 32rpx; font-weight: 600; border: none; width: 100%; margin-top: 8rpx;
}
.btn-submit:disabled { opacity: 0.6; }

.history-item {
  padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5;
}
.history-item:last-child { border-bottom: none; }
.hi-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.hi-sem    { font-size: 28rpx; font-weight: 600; color: #303133; flex: 1; }
.hi-badge  { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 4rpx; }
.badge-complete { background: #f0f9eb; color: #67c23a; }
.badge-partial  { background: #fdf6ec; color: #e6a23c; }
.hi-preview { display: block; font-size: 26rpx; color: #606266; line-height: 1.6; margin-bottom: 8rpx; }
.hi-time    { font-size: 22rpx; color: #c0c4cc; }
</style>
