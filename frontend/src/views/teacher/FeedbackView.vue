<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">反馈与改进</span>
          <div style="display:flex; gap:8px; align-items:center">
            <el-select v-model="selectedSemester" placeholder="选择学期" style="width:220px" @change="loadFeedback">
              <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
            </el-select>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading">
        <el-form-item label="教学反思" prop="reflection">
          <el-input
            v-model="form.reflection"
            type="textarea"
            :rows="6"
            placeholder="请填写本学期的教学反思，包括教学方法、课堂互动、学生反馈等方面的思考..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="改进计划" prop="improvementPlan">
          <el-input
            v-model="form.improvementPlan"
            type="textarea"
            :rows="6"
            placeholder="请填写下一步的改进计划，包括具体的改进措施、时间安排等..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="submitFeedback">
            <el-icon><Check /></el-icon> 提交反馈
          </el-button>
          <el-text type="info" style="margin-left:12px; font-size:12px">
            每个学期的反馈可多次修改，以最后提交的为准
          </el-text>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史反馈 -->
    <el-card shadow="never" style="margin-top:16px" v-if="historyList.length">
      <template #header><span class="title">历史反馈记录</span></template>
      <el-collapse>
        <el-collapse-item v-for="fb in historyList" :key="fb.id"
                          :title="`学期ID: ${fb.semesterId} · 提交时间: ${fb.updateTime}`">
          <div class="history-item">
            <div class="history-section">
              <div class="section-label">教学反思</div>
              <div class="section-content">{{ fb.reflection || '（未填写）' }}</div>
            </div>
            <div class="history-section">
              <div class="section-label">改进计划</div>
              <div class="section-content">{{ fb.improvementPlan || '（未填写）' }}</div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { teacherApi } from '@/api/index'
import { ElMessage } from 'element-plus'

const semesters       = ref([])
const selectedSemester= ref(null)
const loading         = ref(false)
const submitting      = ref(false)
const formRef         = ref()
const historyList     = ref([])
const form = reactive({ reflection: '', improvementPlan: '' })
const rules = {
  reflection:     [{ required: true, message: '请填写教学反思' }],
  improvementPlan:[{ required: true, message: '请填写改进计划' }],
}

async function loadFeedback() {
  if (!selectedSemester.value) return
  loading.value = true
  try {
    const res = await teacherApi.myFeedback()
    historyList.value = res.data || []
    const cur = historyList.value.find(f => f.semesterId === selectedSemester.value)
    form.reflection      = cur?.reflection      || ''
    form.improvementPlan = cur?.improvementPlan || ''
  } finally { loading.value = false }
}

async function submitFeedback() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await teacherApi.saveFeedback({ semesterId: selectedSemester.value, ...form })
    ElMessage.success('反馈提交成功')
    loadFeedback()
  } finally { submitting.value = false }
}

onMounted(async () => {
  const semRes = await teacherApi.semesters()
  semesters.value = semRes.data || []
  const current = semesters.value.find(s => s.isCurrent === 1) || semesters.value[semesters.value.length - 1]
  selectedSemester.value = current?.id || semesters.value[0]?.id
  loadFeedback()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.history-item { padding: 8px 0; }
.history-section { margin-bottom: 12px; }
.section-label { font-weight: 600; color: #606266; font-size: 13px; margin-bottom: 4px; }
.section-content { color: #303133; font-size: 14px; line-height: 1.7; white-space: pre-wrap; }
</style>
