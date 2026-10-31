<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">学期管理</span>
          <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新增学期</el-button>
        </div>
      </template>
      <el-table :data="semesters" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="semesterName" label="学期名称" min-width="200" />
        <el-table-column prop="startDate"    label="开始日期" width="130" />
        <el-table-column prop="endDate"      label="结束日期" width="130" />
        <el-table-column label="当前学期" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isCurrent === 1" type="success" size="small">当前</el-tag>
            <el-button v-else text type="primary" size="small" @click="setCurrent(row)">设为当前</el-button>
          </template>
        </el-table-column>
        <el-table-column label="允许评价" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.allowEvaluation === 1"
              @change="toggleEval(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学期' : '新增学期'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学期名称" prop="semesterName">
          <el-input v-model="form.semesterName" placeholder="如 2025-2026学年第一学期" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="允许评价">
          <el-switch v-model="form.allowEvaluation" :active-value="1" :inactive-value="0" />
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
import { adminSemesterApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const semesters     = ref([])
const loading       = ref(false)
const dialogVisible = ref(false)
const submitting    = ref(false)
const formRef       = ref()
const form = reactive({ id: null, semesterName: '', startDate: '', endDate: '', allowEvaluation: 1 })
const rules = { semesterName: [{ required: true, message: '请输入学期名称' }] }

async function loadData() {
  loading.value = true
  try {
    const res = await adminSemesterApi.list()
    semesters.value = res.data || []
  } finally { loading.value = false }
}

function openDialog(row) {
  Object.assign(form, row ? { ...row } : { id: null, semesterName: '', startDate: '', endDate: '', allowEvaluation: 1 })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) { await adminSemesterApi.update(form.id, form); ElMessage.success('修改成功') }
    else         { await adminSemesterApi.create(form);           ElMessage.success('新增成功') }
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function setCurrent(row) {
  await adminSemesterApi.setCurrent(row.id)
  ElMessage.success('设置成功')
  loadData()
}

async function toggleEval(row) {
  const val = row.allowEvaluation === 1 ? 0 : 1
  await adminSemesterApi.update(row.id, { ...row, allowEvaluation: val })
  row.allowEvaluation = val
}

async function deleteRow(row) {
  await ElMessageBox.confirm(`确认删除学期"${row.semesterName}"？`, '提示', { type: 'warning' })
  await adminSemesterApi.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
</style>
