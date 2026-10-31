<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><span class="title">通知公告</span></template>
      <el-input v-model="keyword" placeholder="搜索公告标题..." clearable prefix-icon="Search"
                style="margin-bottom:16px; max-width:300px" @input="loadData(1)" />
      <div v-loading="loading">
        <div v-for="item in announcements" :key="item.id" class="announce-item" @click="showDetail(item)">
          <el-icon v-if="item.topFlag" color="#e6a23c" style="flex-shrink:0"><StarFilled /></el-icon>
          <div class="a-content">
            <div class="a-title">{{ item.title }}</div>
            <div class="a-preview">{{ item.content?.slice(0, 60) }}...</div>
          </div>
          <div class="a-time">{{ item.createTime?.slice(0, 10) }}</div>
        </div>
        <el-empty v-if="!loading && !announcements.length" description="暂无公告" />
      </div>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :total="total" :page-size="10"
                       layout="prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="current?.title" width="640px">
      <div class="detail-content" v-if="current">{{ current.content }}</div>
      <div class="detail-time">发布时间：{{ current?.createTime }}</div>
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
const keyword       = ref('')
const detailVisible = ref(false)
const current       = ref(null)

async function loadData(p) {
  if (p) page.value = p
  loading.value = true
  try {
    const res = await announcementApi.list(page.value, 10, keyword.value)
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
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 12px; border-radius: 8px; cursor: pointer;
  border-bottom: 1px solid #f0f0f0; transition: background 0.15s;
}
.announce-item:hover { background: #f5f7fa; }
.a-content { flex: 1; }
.a-title   { font-size: 15px; font-weight: 500; color: #303133; margin-bottom: 4px; }
.a-preview { font-size: 12px; color: #8c9ab0; }
.a-time    { font-size: 12px; color: #c0c4cc; white-space: nowrap; flex-shrink: 0; margin-top: 2px; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }
.detail-content { white-space: pre-wrap; line-height: 1.8; color: #303133; font-size: 14px; min-height: 100px; }
.detail-time { margin-top: 16px; font-size: 12px; color: #8c9ab0; text-align: right; }
</style>
