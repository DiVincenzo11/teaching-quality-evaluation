<template>
  <el-card shadow="never" class="mgmt-card">
    <template #header>
      <div class="card-header">
        <span class="title">学生管理</span>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增学生
        </el-button>
      </div>
    </template>

    <!-- 搜索栏 -->
    <el-form :inline="true" class="search-bar">
      <el-form-item label="学期">
        <el-select v-model="query.semesterId" placeholder="选择学期查看评教进度" clearable style="width:220px" @change="loadData(1)">
          <el-option
            v-for="s in semesterList"
            :key="s.id"
            :label="s.semesterName"
            :value="s.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="学号 / 姓名" clearable style="width:200px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.className" placeholder="班级" clearable style="width:130px" @change="loadData(1)">
          <el-option v-for="n in 8" :key="n" :label="`软件工程${n}班`" :value="`软件工程${n}班`" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData(1)"><el-icon><Search /></el-icon> 搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" stripe border
              :default-sort="{ prop: 'username', order: 'ascending' }"
              @sort-change="handleSortChange">
      <el-table-column prop="id"         label="ID"   width="60"  align="center" />
      <el-table-column prop="username"   label="学号" width="145" sortable="custom" />
      <el-table-column prop="realName"   label="姓名" width="90" />
      <el-table-column prop="className"  label="班级" width="130" show-overflow-tooltip />
      <el-table-column prop="major"      label="专业" width="130" show-overflow-tooltip />
      <el-table-column prop="grade"      label="年级" width="70"  align="center" />
      <el-table-column prop="phone"      label="手机" width="125" />
      <el-table-column prop="status"     label="状态" width="70"  align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="评教进度" width="130" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.evalCompleted" type="success" size="small">
            已完成 {{ row.evalCount }}/{{ row.teacherTotal }}
          </el-tag>
          <el-tag v-else type="warning" size="small">
            未完成 {{ row.evalCount ?? 0 }}/{{ row.teacherTotal ?? 0 }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
          <el-button text type="danger"  size="small" @click="deleteRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </div>
  </el-card>

  <!-- 新增/编辑弹窗 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学生' : '新增学生'" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="学号" prop="username">
            <el-input v-model="form.username" :disabled="!!form.id" placeholder="即登录账号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="初始密码">
            <el-input v-model="form.password" placeholder="默认 123456" show-password />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="年级">
            <el-input v-model="form.grade" placeholder="如 2022" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="专业">
            <el-input v-model="form.major" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="班级">
            <el-input v-model="form.className" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminUserApi, commonApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total     = ref(0)
const loading   = ref(false)
const submitting= ref(false)
const dialogVisible = ref(false)
const formRef   = ref()
const semesterList = ref([])

const query = reactive({ page: 1, size: 10, keyword: '', roleType: 1, sortOrder: 'asc', className: '', semesterId: null })
const form  = reactive({})

const rules = {
  username: [{ required: true, message: '请输入学号' }],
  realName: [{ required: true, message: '请输入姓名' }],
}

async function loadData(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const res = await adminUserApi.page({ ...query })
    tableData.value = res.data.records || []
    total.value     = res.data.total   || 0
  } finally { loading.value = false }
}

function resetQuery() { query.keyword = ''; query.sortOrder = 'asc'; query.className = ''; query.semesterId = null; loadData(1) }

function handleSortChange({ prop, order }) {
  if (prop === 'username') query.sortOrder = order === 'descending' ? 'desc' : 'asc'
  loadData(1)
}

function openDialog(row) {
  Object.assign(form, {
    id: null, username: '', password: '', realName: '', email: '', phone: '',
    roleType: 1, status: 1, studentNo: '', className: '', major: '', grade: '',
    ...(row || {}),
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await adminUserApi.update(form.id, form); ElMessage.success('修改成功')
    } else {
      form.studentNo = form.username
      await adminUserApi.create(form); ElMessage.success('新增成功')
    }
    dialogVisible.value = false; loadData()
  } finally { submitting.value = false }
}

async function toggleStatus(row) {
  const s = row.status === 1 ? 0 : 1
  await adminUserApi.updateStatus(row.id, s)
  row.status = s; ElMessage.success(s === 1 ? '已启用' : '已禁用')
}

async function resetPwd(row) {
  await ElMessageBox.confirm(`确认重置 ${row.realName} 的密码为 123456？`, '提示', { type: 'warning' })
  await adminUserApi.resetPwd(row.id, '123456'); ElMessage.success('密码已重置为 123456')
}

async function deleteRow(row) {
  await ElMessageBox.confirm(`确认删除学生 ${row.realName}？`, '提示', { type: 'warning' })
  await adminUserApi.delete(row.id); ElMessage.success('删除成功'); loadData()
}

async function fetchSemesters() {
  try {
    const res = await commonApi.semesters()
    semesterList.value = res.data || []
  } catch (_) { semesterList.value = [] }
}

onMounted(async () => {
  await fetchSemesters()
  loadData()
})
</script>

<style scoped>
.mgmt-card { border-radius: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.search-bar { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
