<template>
  <div class="page">
    <!-- 总览卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-num">{{ choiceQuestions.length }}</div>
          <div class="stat-label">单选题数量</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange">
          <div class="stat-num">{{ essayScore }}</div>
          <div class="stat-label">简答题分值</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" :class="totalBudgetClass">
          <div class="stat-num">{{ totalDisplay }}</div>
          <div class="stat-label">当前总分（目标 100 分）</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card blue">
          <div class="stat-num">{{ remainBudget.toFixed(2) }}</div>
          <div class="stat-label">单选题剩余可分配</div>
        </div>
      </el-col>
    </el-row>

    <!-- 分值预算进度条 -->
    <el-card shadow="never" style="margin-bottom:16px; padding:4px 0">
      <div style="display:flex;align-items:center;gap:12px">
        <span style="font-size:13px;color:#606266;white-space:nowrap">分值总预算</span>
        <el-progress
          :percentage="Math.min(Number(totalDisplay), 100)"
          :color="budgetBarColor"
          :stroke-width="14"
          style="flex:1"
          :format="() => `${totalDisplay} / 100`"
        />
        <el-tag :type="budgetTagType" size="small">
          {{ Number(totalDisplay) === 100 ? '✓ 满分' : Number(totalDisplay) > 100 ? '超出预算' : '未满100分' }}
        </el-tag>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">评价指标体系管理</span>
          <div class="header-actions">
            <el-button type="success" :icon="Plus" @click="showAddDialog = true">新增单选题</el-button>
            <el-button type="warning" :icon="Setting" @click="showRebalanceDialog = true">一键均分</el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>
          <b>计分规则：</b>
          单选题各题分值可<b>独立设置</b>（双击分值或点击编辑图标），总和建议为 {{ 100 - essayScore }} 分；
          简答题 {{ essayScore }} 分，基于 BERT 语义情感分析评分。
          （1→0%  2→25%  3→50%  4→75%  5→100%）
        </template>
      </el-alert>

      <!-- ── 零、前置问题权重配置 ── -->
      <div class="section-title pre-section-title">零、前置问题权重配置（不计入总分）</div>
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:14px">
        <template #title>
          以下权重决定每位学生本次评价在加权平均中的<b>参与度权重</b>（0.00 ~ 1.00）。
          修改后将在下次「重新计算权重」时生效，且历史评价权重会同步更新。
        </template>
      </el-alert>

      <el-row :gutter="24" style="margin-bottom:28px">
        <!-- 出勤率配置 -->
        <el-col :span="12">
          <div class="pre-config-panel">
            <div class="pre-config-title">前置问题 1 — 本学期上课出勤率</div>
            <el-table :data="attendanceConfig" border size="small">
              <el-table-column label="选项值" prop="optionValue" width="70" align="center">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.optionValue }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="选项描述" prop="label" />
              <el-table-column label="权重（0~1）" width="150" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.weight"
                    :min="0" :max="1" :step="0.05" :precision="3"
                    size="small" style="width:120px"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>

        <!-- 课外投入配置 -->
        <el-col :span="12">
          <div class="pre-config-panel">
            <div class="pre-config-title">前置问题 2 — 课外投入时间</div>
            <el-table :data="studyConfig" border size="small">
              <el-table-column label="选项值" prop="optionValue" width="70" align="center">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.optionValue }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="选项描述" prop="label" />
              <el-table-column label="权重（0~1）" width="150" align="center">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.weight"
                    :min="0" :max="1" :step="0.05" :precision="3"
                    size="small" style="width:120px"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
      <div style="text-align:right;margin-bottom:28px">
        <el-button type="primary" :loading="savingPreConfig" @click="savePreConfig">
          保存前置问题权重配置
        </el-button>
      </div>

      <!-- ── 单选题（按分类，分值可单独编辑）── -->
      <template v-for="(cat, catIdx) in categorizedQuestions" :key="cat.title">
        <div class="section-title">{{ ['一', '二', '三'][catIdx] }}、{{ cat.title }}（{{ cat.questions.length }} 题）</div>
        <el-table :data="cat.questions" v-loading="loading" border stripe style="margin-bottom:24px">
          <el-table-column prop="questionNo" label="编号" width="70" align="center" />
          <el-table-column label="题目内容">
            <template #default="{ row }">
              <el-input
                v-if="editingId === row.id"
                v-model="editContent"
                size="small"
                @blur="saveContent(row)"
                @keyup.enter="saveContent(row)"
                ref="contentInputRef"
              />
              <span v-else class="content-text" @dblclick="startEdit(row)">
                {{ row.questionContent }}
                <el-icon class="edit-icon" @click="startEdit(row)"><Edit /></el-icon>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="170" align="center">
            <template #default="{ row }">
              <div v-if="editingScoreId === row.id" style="display:flex;align-items:center;gap:4px">
                <el-input-number
                  v-model="editScore"
                  :min="0.01" :max="100" :precision="2" :step="1"
                  size="small" style="width:110px"
                  ref="scoreInputRef"
                  @blur="saveScore(row)"
                  @keyup.enter="saveScore(row)"
                />
              </div>
              <span v-else class="score-text" @dblclick="startEditScore(row)">
                <el-tag :type="scoreTagType(row)" size="small">
                  {{ row.maxScore?.toFixed ? row.maxScore.toFixed(2) : row.maxScore }} 分
                </el-tag>
                <el-icon class="edit-icon" @click="startEditScore(row)"><Edit /></el-icon>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该题目？" @confirm="deleteQuestion(row)">
                <template #reference>
                  <el-button text type="danger" size="small" :icon="Delete" />
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 简答题 -->
      <div class="section-title">四、简答题（主观评语）</div>
      <el-table :data="essayQuestions" border stripe>
        <el-table-column prop="questionNo" label="编号" width="70" align="center" />
        <el-table-column label="题目内容">
          <template #default="{ row }">
            <el-input
              v-if="editingId === row.id"
              v-model="editContent"
              type="textarea"
              :rows="2"
              size="small"
              @blur="saveContent(row)"
              ref="contentInputRef"
            />
            <span v-else class="content-text" @dblclick="startEdit(row)">
              {{ row.questionContent }}
              <el-icon class="edit-icon" @click="startEdit(row)"><Edit /></el-icon>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="分值" width="170" align="center">
          <template #default="{ row }">
            <div v-if="editingScoreId === row.id" style="display:flex;align-items:center;gap:4px">
              <el-input-number
                v-model="editScore"
                :min="0" :max="90" :precision="2" :step="5"
                size="small" style="width:110px"
                ref="scoreInputRef"
                @blur="saveScore(row)"
                @keyup.enter="saveScore(row)"
              />
            </div>
            <span v-else class="score-text" @dblclick="startEditScore(row)">
              <el-tag type="warning">
                {{ row.maxScore?.toFixed ? row.maxScore.toFixed(2) : row.maxScore }} 分
              </el-tag>
              <el-icon class="edit-icon" @click="startEditScore(row)"><Edit /></el-icon>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评分方式" width="160" align="center">
          <template #default>
            <el-tag type="info" size="small">BERT情感分析</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增单选题弹窗 -->
    <el-dialog v-model="showAddDialog" title="新增单选题" width="540px">
      <el-form :model="addForm" label-width="90px">
        <el-form-item label="题目内容" required>
          <el-input v-model="addForm.questionContent" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <el-form-item label="初始分值">
          <el-input-number v-model="addForm.maxScore" :min="0.01" :max="100" :precision="2" :step="1" />
          <span style="margin-left:8px;color:#909399;font-size:12px">
            （当前单选题总分 {{ choiceScoreTotal.toFixed(2) }}，留余 {{ remainBudget.toFixed(2) }} 分）
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="addQuestion">确认新增</el-button>
      </template>
    </el-dialog>

    <!-- 一键均分弹窗 -->
    <el-dialog v-model="showRebalanceDialog" title="一键均分（将覆盖手动设置）" width="440px">
      <div class="rebalance-info">
        <p>当前单选题：<b>{{ choiceQuestions.length }}</b> 题</p>
        <el-form label-width="110px" style="margin-top:16px">
          <el-form-item label="简答题分值">
            <el-input-number v-model="rebalanceEssay" :min="0" :max="90" :step="5" />
            <span style="margin-left:8px; color:#909399">分</span>
          </el-form-item>
          <el-form-item label="单选题总分">
            <span class="calc-result">{{ 100 - rebalanceEssay }} 分</span>
          </el-form-item>
          <el-form-item label="每题分值">
            <span class="calc-result" :class="{ ok: choiceQuestions.length > 0 }">
              {{ choiceQuestions.length > 0 ? ((100 - rebalanceEssay) / choiceQuestions.length).toFixed(2) : '-' }} 分
            </span>
          </el-form-item>
        </el-form>
        <el-alert type="warning" :closable="false" show-icon style="margin-top:8px">
          <template #title>此操作将把所有单选题分值重置为均等，覆盖手动设置</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showRebalanceDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRebalance">确认均分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { adminQuestionApi } from '@/api/index'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Setting, Edit } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'

// ── 题目数据 ──
const questions       = ref([])
const loading         = ref(false)
const saving          = ref(false)

// 内容内联编辑
const editingId       = ref(null)
const editContent     = ref('')
const contentInputRef = ref()

// 分值内联编辑
const editingScoreId  = ref(null)
const editScore       = ref(0)
const scoreInputRef   = ref()

// 弹窗
const showAddDialog       = ref(false)
const showRebalanceDialog = ref(false)
const addForm             = ref({ questionContent: '', maxScore: 10 })
const rebalanceEssay      = ref(20)

// ── 前置问题权重配置 ──
const preConfigAll    = ref([])
const savingPreConfig = ref(false)

const attendanceConfig = computed(() =>
  preConfigAll.value.filter(c => c.questionKey === 'attendance')
)
const studyConfig = computed(() =>
  preConfigAll.value.filter(c => c.questionKey === 'study_time')
)

async function loadPreConfig() {
  const res = await adminQuestionApi.getPreConfig()
  preConfigAll.value = res.data || []
}

async function savePreConfig() {
  savingPreConfig.value = true
  try {
    await adminQuestionApi.savePreConfig(preConfigAll.value)
    ElMessage.success('前置问题权重配置已保存')
  } finally { savingPreConfig.value = false }
}

// ── 题目计算属性 ──
const choiceQuestions = computed(() => questions.value.filter(q => q.questionType === 1))
const essayQuestions  = computed(() => questions.value.filter(q => q.questionType === 2))

const questionCategories = [
  { title: '教学内容与方法', nos: [1, 2, 3, 4] },
  { title: '教学态度与互动', nos: [5, 6, 7, 8] },
  { title: '教学效果与收获', nos: [9, 10, 11, 12] },
]
const categorizedQuestions = computed(() =>
  questionCategories.map(cat => ({
    ...cat,
    questions: cat.nos
      .map(no => choiceQuestions.value.find(q => Number(q.questionNo) === no))
      .filter(Boolean),
  }))
)

const essayScore = computed(() => {
  const eq = essayQuestions.value[0]
  return eq ? (Number(eq.maxScore) || 0) : 20
})
const choiceScoreTotal = computed(() =>
  choiceQuestions.value.reduce((s, q) => s + (Number(q.maxScore) || 0), 0)
)
const totalDisplay = computed(() => (choiceScoreTotal.value + essayScore.value).toFixed(2))
const remainBudget = computed(() => (100 - essayScore.value) - choiceScoreTotal.value)

const totalBudgetClass = computed(() => {
  const t = Number(totalDisplay.value)
  if (Math.abs(t - 100) < 0.01) return 'green'
  if (t > 100) return 'red'
  return 'blue'
})
const budgetBarColor = computed(() => {
  const t = Number(totalDisplay.value)
  if (Math.abs(t - 100) < 0.01) return '#67c23a'
  if (t > 100) return '#f56c6c'
  return '#409eff'
})
const budgetTagType = computed(() => {
  const t = Number(totalDisplay.value)
  if (Math.abs(t - 100) < 0.01) return 'success'
  if (t > 100) return 'danger'
  return 'primary'
})

// 根据分值高低给分值 tag 着色（相对于均等值）
function scoreTagType(row) {
  const avg = choiceQuestions.value.length
    ? (100 - essayScore.value) / choiceQuestions.value.length
    : 10
  const v = Number(row.maxScore)
  if (v > avg * 1.2) return 'danger'
  if (v > avg * 1.05) return 'warning'
  if (v < avg * 0.8) return 'info'
  return 'primary'
}

// ── 数据加载 ──
async function loadData() {
  loading.value = true
  try {
    const res = await adminQuestionApi.list()
    questions.value = res.data || []
    rebalanceEssay.value = essayScore.value
  } finally { loading.value = false }
}

// ── 内容编辑 ──
function startEdit(row) {
  editingScoreId.value = null
  editingId.value = row.id
  editContent.value = row.questionContent
  nextTick(() => contentInputRef.value?.focus?.())
}

const saveContent = useDebounceFn(async (row) => {
  if (editContent.value === row.questionContent) { editingId.value = null; return }
  if (!editContent.value.trim()) { editingId.value = null; return }
  await adminQuestionApi.update(row.id, { questionContent: editContent.value })
  row.questionContent = editContent.value
  editingId.value = null
  ElMessage.success('题目内容已更新')
}, 300)

// ── 分值编辑 ──
function startEditScore(row) {
  editingId.value = null
  editingScoreId.value = row.id
  editScore.value = Number(row.maxScore) || 0
  nextTick(() => scoreInputRef.value?.focus?.())
}

const saveScore = useDebounceFn(async (row) => {
  if (editScore.value === Number(row.maxScore)) { editingScoreId.value = null; return }
  if (!editScore.value || editScore.value <= 0) { editingScoreId.value = null; return }
  await adminQuestionApi.update(row.id, { maxScore: editScore.value })
  row.maxScore = editScore.value
  editingScoreId.value = null
  ElMessage.success('分值已更新')
}, 300)

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await adminQuestionApi.update(row.id, { status: newStatus })
  row.status = newStatus
}

// ── 新增 ──
async function addQuestion() {
  if (!addForm.value.questionContent.trim()) {
    ElMessage.warning('请输入题目内容')
    return
  }
  saving.value = true
  try {
    await adminQuestionApi.create({
      questionContent: addForm.value.questionContent,
      maxScore: addForm.value.maxScore,
    })
    ElMessage.success('新增成功')
    showAddDialog.value = false
    addForm.value = { questionContent: '', maxScore: 10 }
    await loadData()
  } finally { saving.value = false }
}

// ── 删除 ──
async function deleteQuestion(row) {
  saving.value = true
  try {
    await adminQuestionApi.delete(row.id)
    ElMessage.success('已删除')
    await loadData()
  } finally { saving.value = false }
}

// ── 一键均分 ──
async function doRebalance() {
  saving.value = true
  try {
    await adminQuestionApi.rebalance(rebalanceEssay.value)
    ElMessage.success('分数已均等分配')
    showRebalanceDialog.value = false
    await loadData()
  } finally { saving.value = false }
}

onMounted(async () => {
  await loadData()
  await loadPreConfig()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.header-actions { display: flex; gap: 8px; }
.title { font-size: 16px; font-weight: 600; }

.section-title {
  font-weight: 700; color: #1a2a6c; font-size: 15px;
  padding: 10px 0 8px; border-bottom: 2px solid #1a2a6c;
  margin: 4px 0 14px;
}
.pre-section-title { color: #b8660a; border-bottom-color: #e6a23c; }

.stat-card {
  background: linear-gradient(135deg, #f5f7ff, #eef1ff);
  border: 1px solid #dde1f7; border-radius: 12px;
  padding: 16px 20px; text-align: center;
}
.stat-card.blue  { background: linear-gradient(135deg, #f0f7ff, #e8f4ff); border-color: #c6e2ff; }
.stat-card.orange{ background: linear-gradient(135deg, #fff8f0, #fff3e8); border-color: #ffd591; }
.stat-card.green { background: linear-gradient(135deg, #f0fff4, #e8fff0); border-color: #b7eb8f; }
.stat-card.red   { background: linear-gradient(135deg, #fff0f0, #ffe8e8); border-color: #ffa8a8; }
.stat-num   { font-size: 28px; font-weight: 700; color: #1a2a6c; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }

.content-text { cursor: pointer; display: flex; align-items: center; gap: 6px; }
.score-text   { cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; }
.edit-icon { color: #c0c4cc; font-size: 14px; cursor: pointer; transition: color .2s; }
.edit-icon:hover { color: #409eff; }

.pre-config-panel {
  background: #fffbf0; border: 1px solid #fde9a0;
  border-radius: 10px; padding: 14px 16px;
}
.pre-config-title {
  font-weight: 700; color: #b8660a; font-size: 13px; margin-bottom: 10px;
}

.rebalance-info p { color: #606266; margin-bottom: 4px; }
.calc-result { font-size: 18px; font-weight: 700; color: #1a2a6c; }
.calc-result.ok { color: #67c23a; }
</style>
