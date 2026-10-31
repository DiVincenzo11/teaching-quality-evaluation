<template>
  <el-card shadow="never" class="mgmt-card">
    <template #header>
      <div class="card-header">
        <span class="title">教师管理</span>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 新增教师
        </el-button>
      </div>
    </template>

    <!-- 搜索栏 -->
    <el-form :inline="true" class="search-bar">
      <el-form-item label="学期">
        <el-select v-model="query.semesterId" placeholder="全部学期" clearable style="width:200px"
                   @change="loadData(1)">
          <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="工号 / 姓名 / 院系" clearable style="width:200px" />
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
      <el-table-column prop="username"   label="工号" width="110" sortable="custom" />
      <el-table-column prop="realName"   label="姓名" width="90" />
      <el-table-column prop="department" label="院系" width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag type="primary" size="small" effect="plain">{{ row.department || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title"      label="职称" width="90" />
      <el-table-column label="主授课程" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ parsedCourses(row.courses).names }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所教班级" min-width="200">
        <template #default="{ row }">
          <el-space wrap :size="4">
            <el-tag
              v-for="cls in parsedCourses(row.courses).classes"
              :key="cls"
              size="small"
              type="info"
              effect="plain"
            >{{ cls }}</el-tag>
            <span v-if="!parsedCourses(row.courses).classes.length" style="color:#aaa">—</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column prop="phone"      label="手机" width="125" />
      <el-table-column prop="status"     label="状态" width="70"  align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
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
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑教师' : '新增教师'" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="工号" prop="username">
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
          <el-form-item label="院系">
            <el-input v-model="form.department" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职称">
            <el-select v-model="form.title" style="width:100%" placeholder="请选择">
              <el-option label="助教" value="助教" />
              <el-option label="讲师" value="讲师" />
              <el-option label="副教授" value="副教授" />
              <el-option label="教授" value="教授" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="课程">
            <el-input v-model="form.courses" placeholder="多门以逗号分隔" />
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
import { adminUserApi, adminSemesterApi, teacherApi } from '@/api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total     = ref(0)
const loading   = ref(false)
const submitting= ref(false)
const dialogVisible = ref(false)
const formRef   = ref()
const semesters = ref([])

const query = reactive({ page: 1, size: 10, keyword: '', roleType: 2, sortOrder: 'asc', semesterId: null })
const form  = reactive({})

const rules = {
  username: [{ required: true, message: '请输入工号' }],
  realName: [{ required: true, message: '请输入姓名' }],
}

/**
 * 解析 courses 字段，返回：
 *   names   — 课程名列表，用顿号拼接（去掉班级部分）
 *   classes — 展开后的班级数组，如 ["22软工1班","22软工2班","22软工3班","22软工4班"]
 *
 * 支持格式：
 *   "高等数学AⅠ(1-4班),数据结构(1-3班)"
 *   "大学英语Ⅰ(1-3班)"
 *   "国家安全教育与军事理论(1-8班)"
 */
function parsedCourses(coursesStr) {
  if (!coursesStr) return { names: '—', classes: [] }
  const items = coursesStr.split(',')
  const nameList = []
  const classSet = new Set()
  items.forEach(item => {
    const m = item.trim().match(/^(.+?)\((\d+)-(\d+)班\)$/)
    if (m) {
      nameList.push(m[1].trim())
      for (let i = parseInt(m[2]); i <= parseInt(m[3]); i++) classSet.add('22软工' + i + '班')
    } else {
      nameList.push(item.trim().replace(/\(.*?\)/, '').trim())
    }
  })
  const sorted = [...classSet].sort((a, b) => {
    const na = parseInt((a.match(/(\d+)班$/) || [0, 0])[1]) || 0
    const nb = parseInt((b.match(/(\d+)班$/) || [0, 0])[1]) || 0
    return na - nb
  })
  return { names: nameList.join('、'), classes: sorted }
}

async function loadSemesters() {
  const res = await adminSemesterApi.list()
  semesters.value = (res.data || []).sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
}

async function loadData(p) {
  if (p) query.page = p
  loading.value = true
  try {
    if (query.semesterId) {
      // 按学期筛选：调用 teacher/list 接口（带 semesterId）
      const res = await teacherApi.list(query.keyword, null, query.semesterId)
      tableData.value = res.data || []
      total.value     = tableData.value.length
    } else {
      const res = await adminUserApi.page({ ...query })
      tableData.value = res.data.records || []
      total.value     = res.data.total   || 0
    }
  } finally { loading.value = false }
}

function resetQuery() { query.keyword = ''; query.semesterId = null; query.sortOrder = 'asc'; loadData(1) }

function handleSortChange({ prop, order }) {
  if (prop === 'username') query.sortOrder = order === 'descending' ? 'desc' : 'asc'
  loadData(1)
}

function openDialog(row) {
  Object.assign(form, {
    id: null, username: '', password: '', realName: '', email: '', phone: '',
    roleType: 2, status: 1, teacherNo: '', department: '', title: '讲师', courses: '',
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
      form.teacherNo = form.username
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
  await ElMessageBox.confirm(`确认删除教师 ${row.realName}？`, '提示', { type: 'warning' })
  await adminUserApi.delete(row.id); ElMessage.success('删除成功'); loadData()
}

onMounted(() => { loadSemesters(); loadData() })
</script>

<style scoped>
.mgmt-card { border-radius: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; }
.search-bar { margin-bottom: 16px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
