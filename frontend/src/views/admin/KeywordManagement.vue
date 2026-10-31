<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">语义分析关键词管理</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon> 新增关键词
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="全部" name="0" />
        <el-tab-pane label="优质评价词" name="1">
          <template #label>
            <span><el-icon color="#67c23a"><Check /></el-icon> 优质评价词</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="中性评价词" name="2">
          <template #label>
            <span><el-icon color="#e6a23c"><Remove /></el-icon> 中性评价词</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="负面评价词" name="3">
          <template #label>
            <span><el-icon color="#f56c6c"><Close /></el-icon> 负面评价词</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="keyword-tags" v-loading="loading">
        <template v-if="keywords.length === 0">
          <el-empty description="暂无关键词" />
        </template>
        <template v-else>
          <el-space wrap>
            <el-tag
              v-for="kw in keywords"
              :key="kw.id"
              :type="tagType(kw.wordType)"
              closable
              @close="deleteKeyword(kw)"
              size="large"
              style="cursor:pointer; font-size:14px; padding: 8px 14px;"
              @click="openDialog(kw)"
            >
              {{ kw.word }}
            </el-tag>
          </el-space>
        </template>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑关键词' : '新增关键词'" width="400px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="关键词" prop="word">
          <el-input v-model="form.word" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item label="类型" prop="wordType">
          <el-radio-group v-model="form.wordType">
            <el-radio :value="1"><el-tag type="success" size="small">优质</el-tag></el-radio>
            <el-radio :value="2"><el-tag type="warning" size="small">中性</el-tag></el-radio>
            <el-radio :value="3"><el-tag type="danger"  size="small">负面</el-tag></el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminKeywordApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const keywords      = ref([])
const loading       = ref(false)
const dialogVisible = ref(false)
const submitting    = ref(false)
const activeTab     = ref('0')
const formRef       = ref()
const form          = reactive({ id: null, word: '', wordType: 1 })

const rules = {
  word:     [{ required: true, message: '请输入关键词' }],
  wordType: [{ required: true, message: '请选择类型' }],
}

const tagType = (t) => ({ 1: 'success', 2: 'warning', 3: 'danger' })[t] || 'info'

async function loadData() {
  loading.value = true
  try {
    const type = activeTab.value === '0' ? null : Number(activeTab.value)
    const res = await adminKeywordApi.list(type)
    keywords.value = res.data || []
  } finally { loading.value = false }
}

function openDialog(row) {
  Object.assign(form, row ? { id: row.id, word: row.word, wordType: row.wordType } : { id: null, word: '', wordType: 1 })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await adminKeywordApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await adminKeywordApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function deleteKeyword(kw) {
  await ElMessageBox.confirm(`确认删除关键词"${kw.word}"？`, '提示', { type: 'warning' })
  await adminKeywordApi.delete(kw.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.keyword-tags { min-height: 120px; padding: 16px 0; }
</style>
