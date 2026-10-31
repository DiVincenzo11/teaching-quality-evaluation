<template>
  <view class="container" style="padding-bottom: 40rpx;">
    <!-- 操作栏 -->
    <view class="action-bar">
      <button class="btn-add" @click="openCreate">+ 新增题目</button>
      <button class="btn-rebalance" @click="rebalance">⚖ 自动平衡权重</button>
    </view>

    <view v-if="loading" class="loading-box"><text>加载中...</text></view>
    <view v-else-if="!list.length" class="empty-box">
      <text class="empty-icon">📝</text>
      <text>暂无题目</text>
    </view>

    <!-- 单选题 -->
    <view v-if="choiceList.length" class="group-header">
      <text>单选题（Likert量表）</text>
      <view class="group-badge">{{ choiceList.length }}题</view>
    </view>
    <view v-for="q in choiceList" :key="q.id" class="q-card">
      <view class="q-card-header">
        <text class="q-no">Q{{ q.questionNo }}</text>
        <view class="type-badge choice">单选</view>
        <text class="q-score">满分 {{ q.maxScore }} 分</text>
      </view>
      <text class="q-content">{{ q.questionContent }}</text>
      <view class="q-actions">
        <text class="action-btn edit-btn" @click="openEdit(q)">编辑</text>
        <text class="action-btn del-btn" @click="deleteQ(q)">删除</text>
      </view>
    </view>

    <!-- 简答题 -->
    <view v-if="essayList.length" class="group-header">
      <text>简答题（综合评语）</text>
      <view class="group-badge">{{ essayList.length }}题</view>
    </view>
    <view v-for="q in essayList" :key="q.id" class="q-card">
      <view class="q-card-header">
        <text class="q-no">Q{{ q.questionNo }}</text>
        <view class="type-badge essay">简答</view>
        <text class="q-score">满分 {{ q.maxScore }} 分</text>
      </view>
      <text class="q-content">{{ q.questionContent }}</text>
      <view class="q-actions">
        <text class="action-btn edit-btn" @click="openEdit(q)">编辑</text>
        <text class="action-btn del-btn" @click="deleteQ(q)">删除</text>
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view v-if="dialogVisible" class="modal-mask">
      <view class="modal-overlay" @click="dialogVisible = false" />
      <view class="modal-box">
        <view class="modal-header">
          <text class="modal-title">{{ editId ? '编辑题目' : '新增题目' }}</text>
          <text class="modal-close" @click="dialogVisible = false">✕</text>
        </view>
        <view style="padding: 24rpx 40rpx;">
          <view class="form-row">
            <text class="form-label">题目编号</text>
            <input v-model.number="form.questionNo" class="form-input" placeholder="请输入编号" type="number" />
          </view>
          <view class="form-row">
            <text class="form-label">题目类型</text>
            <picker mode="selector" :range="['单选题','简答题']" :value="typeIndex" @change="onTypeChange">
              <view class="picker-val">
                <text>{{ ['单选题','简答题'][typeIndex] }}</text>
                <text class="arrow">›</text>
              </view>
            </picker>
          </view>
          <view class="form-row">
            <text class="form-label">满分分值</text>
            <input v-model.number="form.maxScore" class="form-input" placeholder="如：7.5" type="digit" />
          </view>
          <view class="form-row-block">
            <text class="form-label-block">题目内容</text>
            <textarea v-model="form.questionContent" class="form-textarea" placeholder="请输入题目内容..." />
          </view>
        </view>
        <view class="modal-footer">
          <button class="btn-cancel-m" @click="dialogVisible = false">取消</button>
          <button class="btn-confirm-m" :disabled="saving" @click="saveQ">
            {{ saving ? '保存中...' : '确认保存' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminQuestionApi } from '@/api/index'

const list    = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId  = ref(null)
const saving  = ref(false)
const typeIndex = ref(0)

const form = reactive({ questionNo: '', questionType: 1, questionContent: '', maxScore: '' })

const choiceList = computed(() => list.value.filter(q => q.questionType === 1).sort((a, b) => a.questionNo - b.questionNo))
const essayList  = computed(() => list.value.filter(q => q.questionType === 2).sort((a, b) => a.questionNo - b.questionNo))

async function loadList() {
  loading.value = true
  try {
    const res = await adminQuestionApi.list()
    list.value = res.data || []
  } finally { loading.value = false }
}

function openCreate() {
  editId.value = null; typeIndex.value = 0
  Object.assign(form, { questionNo: '', questionType: 1, questionContent: '', maxScore: '' })
  dialogVisible.value = true
}

function openEdit(q) {
  editId.value = q.id
  typeIndex.value = q.questionType === 2 ? 1 : 0
  Object.assign(form, { questionNo: q.questionNo, questionType: q.questionType, questionContent: q.questionContent, maxScore: q.maxScore })
  dialogVisible.value = true
}

function onTypeChange(e) {
  typeIndex.value = e.detail.value
  form.questionType = typeIndex.value === 1 ? 2 : 1
}

async function saveQ() {
  if (!form.questionContent.trim()) { uni.showToast({ title: '请输入题目内容', icon: 'none' }); return }
  saving.value = true
  try {
    const payload = {
      questionNo:      Number(form.questionNo),
      questionType:    form.questionType,
      questionContent: form.questionContent.trim(),
      maxScore:        Number(form.maxScore) || 0,
    }
    if (editId.value) {
      await adminQuestionApi.update(editId.value, payload)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await adminQuestionApi.create(payload)
      uni.showToast({ title: '创建成功', icon: 'success' })
    }
    dialogVisible.value = false
    loadList()
  } finally { saving.value = false }
}

async function deleteQ(q) {
  uni.showModal({ title: '确认删除', content: `确认删除题目 Q${q.questionNo}？`, success: async ({ confirm }) => {
    if (!confirm) return
    await adminQuestionApi.delete(q.id)
    uni.showToast({ title: '删除成功', icon: 'success' })
    loadList()
  }})
}

async function rebalance() {
  uni.showModal({ title: '自动平衡', content: '将使用熵权法重新计算各题权重，确认执行？', success: async ({ confirm }) => {
    if (!confirm) return
    await adminQuestionApi.rebalance()
    uni.showToast({ title: '权重已更新', icon: 'success' })
    loadList()
  }})
}

onMounted(loadList)
</script>

<style scoped>
.action-bar { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.btn-add      { flex: 1; background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; border-radius: 12rpx; height: 80rpx; font-size: 26rpx; border: none; }
.btn-rebalance { flex: 1; background: linear-gradient(135deg, #e6a23c, #f5a623); color: #fff; border-radius: 12rpx; height: 80rpx; font-size: 26rpx; border: none; }

.group-header { display: flex; align-items: center; gap: 12rpx; padding: 16rpx 0 12rpx; font-size: 28rpx; font-weight: 600; color: #303133; }
.group-badge  { background: #ecf5ff; color: #409eff; font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 12rpx; }

.q-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.q-card-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.q-no  { font-size: 24rpx; font-weight: 700; color: #1a2a6c; }
.type-badge { font-size: 20rpx; padding: 3rpx 12rpx; border-radius: 4rpx; }
.choice { background: #ecf5ff; color: #409eff; }
.essay  { background: #f0f9eb; color: #67c23a; }
.q-score { font-size: 22rpx; color: #909399; margin-left: auto; }
.q-content { display: block; font-size: 28rpx; color: #303133; line-height: 1.6; margin-bottom: 16rpx; }
.q-actions { display: flex; gap: 16rpx; }
.action-btn  { font-size: 24rpx; padding: 8rpx 20rpx; border-radius: 8rpx; }
.edit-btn { color: #409eff; background: #ecf5ff; }
.del-btn  { color: #f56c6c; background: #fef0f0; }

.modal-mask   { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: flex-end; z-index: 1000; }
.modal-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); }
.modal-box    { position: relative; z-index: 1; background: #fff; border-radius: 32rpx 32rpx 0 0; width: 100%; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 32rpx 40rpx; border-bottom: 2rpx solid #f0f0f0; }
.modal-title  { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close  { font-size: 36rpx; color: #c0c4cc; }
.modal-footer { display: flex; gap: 16rpx; padding: 24rpx 40rpx; border-top: 2rpx solid #f0f0f0; }
.modal-footer button { flex: 1; border-radius: 12rpx; height: 88rpx; font-size: 28rpx; border: none; }
.btn-cancel-m  { background: #f5f7fa; color: #606266; }
.btn-confirm-m { background: linear-gradient(135deg, #1a2a6c, #2d6a9f); color: #fff; }
.btn-confirm-m:disabled { opacity: 0.6; }
.form-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 2rpx solid #f5f5f5; gap: 16rpx; }
.form-row:last-child { border-bottom: none; }
.form-label  { font-size: 28rpx; color: #606266; width: 160rpx; flex-shrink: 0; }
.form-input  { flex: 1; font-size: 28rpx; color: #303133; text-align: right; }
.picker-val  { display: flex; align-items: center; gap: 8rpx; font-size: 28rpx; color: #303133; }
.arrow { color: #c0c4cc; }
.form-row-block { padding: 20rpx 0; }
.form-label-block { display: block; font-size: 28rpx; color: #606266; margin-bottom: 12rpx; }
.form-textarea { width: 100%; background: #f5f7fa; border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; color: #303133; min-height: 120rpx; box-sizing: border-box; }
</style>

