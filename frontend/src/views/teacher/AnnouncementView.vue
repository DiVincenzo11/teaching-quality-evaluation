<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><span class="title">通知公告</span></template>
      <div v-loading="loading">
        <div v-for="item in announcements" :key="item.id" class="announce-item" @click="showDetail(item)">
          <el-icon v-if="item.topFlag" color="#e6a23c"><StarFilled /></el-icon>
          <div class="announce-title">{{ item.title }}</div>
          <div class="announce-time">{{ item.createTime }}</div>
        </div>
        <el-empty v-if="!loading && !announcements.length" description="暂无公告" />
      </div>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                       layout="prev, pager, next" @change="loadData" />
      </div>
    </el-card>
    <el-dialog v-model="detailVisible" :title="current?.title" width="600px">
      <div class="content" v-if="current">{{ current.content }}</div>
      <div class="create-time">发布时间：{{ current?.createTime }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { announcementApi } from '@/api/index'

const announcements = ref([])
const total         = ref(0)
const page          = ref(1)
const loading       = ref(false)
const detailVisible = ref(false)
const current       = ref(null)

async function loadData(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await announcementApi.list(page.value, 10)
    announcements.value = res.data?.records || []
    total.value         = res.data?.total   || 0
  } finally { loading.value = false }
}

function showDetail(item) { current.value = item; detailVisible.value = true }
onMounted(loadData)
</script>

<style scoped>
.title { font-size: 16px; font-weight: 600; }
.announce-item {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 16px; border-radius: 8px; cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}
.announce-item:hover { background: #f5f7fa; }
.announce-title { flex: 1; font-size: 14px; color: #303133; }
.announce-time  { font-size: 12px; color: #8c9ab0; white-space: nowrap; }
.content { white-space: pre-wrap; line-height: 1.8; color: #303133; font-size: 14px; }
.create-time { margin-top: 16px; font-size: 12px; color: #8c9ab0; text-align: right; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }
</style>
