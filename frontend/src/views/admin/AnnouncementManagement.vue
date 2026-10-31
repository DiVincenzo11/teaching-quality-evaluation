<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">通知公告管理</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon> 发布公告
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <el-icon v-if="row.topFlag" color="#e6a23c" style="vertical-align:middle;margin-right:4px"><StarFilled /></el-icon>
            {{ row.title }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="165" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" size="small"
                       @click="togglePublish(row)">
              {{ row.status === 1 ? '撤回' : '发布' }}
            </el-button>
            <el-button text type="danger" size="small" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">草稿</el-radio>
                <el-radio :value="1">立即发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置顶">
              <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { announcementApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData     = ref([])
const total         = ref(0)
const loading       = ref(false)
const dialogVisible = ref(false)
const submitting    = ref(false)
const formRef       = ref()
const query = reactive({ page: 1, size: 10 })
const form  = reactive({ id: null, title: '', content: '', status: 1, topFlag: 0 })
const rules = {
  title:   [{ required: true, message: '请输入标题' }],
  content: [{ required: true, message: '请输入内容' }],
}

async function loadData(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const res = await announcementApi.list(query.page, query.size)
    tableData.value = res.data?.records || []
    total.value     = res.data?.total   || 0
  } finally { loading.value = false }
}

function openDialog(row) {
  Object.assign(form, row
    ? { id: row.id, title: row.title, content: row.content, status: row.status, topFlag: row.topFlag }
    : { id: null, title: '', content: '', status: 1, topFlag: 0 })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) { await announcementApi.update(form.id, form); ElMessage.success('修改成功') }
    else         { await announcementApi.create(form);           ElMessage.success('发布成功') }
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function togglePublish(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await announcementApi.update(row.id, { ...row, status: newStatus })
  row.status = newStatus
  ElMessage.success(newStatus === 1 ? '已发布' : '已撤回')
}

async function deleteRow(row) {
  await ElMessageBox.confirm(`确认删除公告"${row.title}"？`, '提示', { type: 'warning' })
  await announcementApi.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
