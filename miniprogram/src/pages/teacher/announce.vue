<template>
  <view class="container" style="padding-bottom: 40rpx;">
    <view class="search-card card">
      <view class="search-wrap">
        <text class="search-icon">🔍</text>
        <input v-model="keyword" class="search-input" placeholder="搜索公告标题"
               placeholder-class="ph" @input="onSearch" />
        <text v-if="keyword" class="clear-btn" @click="clearSearch">✕</text>
      </view>
    </view>

    <view v-if="loading" class="loading-box"><text>加载中...</text></view>
    <view v-else-if="!list.length" class="empty-box">
      <text class="empty-icon">📢</text>
      <text>暂无公告</text>
    </view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="announce-card" @click="goDetail(item.id)">
        <view class="ac-header">
          <view v-if="item.topFlag" class="top-tag">置顶</view>
          <text class="ac-title">{{ item.title }}</text>
        </view>
        <view class="ac-footer">
          <text class="ac-publisher">{{ item.publisher }}</text>
          <text class="ac-time">{{ item.createTime?.slice(0, 10) }}</text>
        </view>
      </view>
      <view v-if="hasMore" class="load-more" @click="loadMore">加载更多</view>
      <view v-else-if="list.length > 5" class="no-more">已加载全部</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { announcementApi } from '@/api/index'

const list    = ref([])
const loading = ref(false)
const keyword = ref('')
const page    = ref(1)
const size    = 10
const total   = ref(0)
const hasMore = ref(false)

async function loadList(reset = false) {
  if (reset) { page.value = 1; list.value = [] }
  loading.value = true
  try {
    const res = await announcementApi.list(page.value, size, keyword.value || undefined)
    const records = res.data?.records || []
    total.value = res.data?.total || 0
    if (reset) list.value = records; else list.value.push(...records)
    hasMore.value = list.value.length < total.value
  } finally { loading.value = false }
}

function onSearch() { loadList(true) }
function clearSearch() { keyword.value = ''; loadList(true) }
async function loadMore() { page.value++; await loadList() }
function goDetail(id) { uni.navigateTo({ url: `/pages/student/announce-detail?id=${id}` }) }

onMounted(() => loadList(true))
</script>

<style scoped>
.search-card { padding: 16rpx 24rpx; margin-bottom: 0; }
.search-wrap { display: flex; align-items: center; gap: 12rpx; background: #f5f7fa; border-radius: 40rpx; padding: 16rpx 24rpx; }
.search-icon  { font-size: 28rpx; }
.search-input { flex: 1; font-size: 26rpx; color: #303133; background: transparent; }
.ph { color: #c0c4cc; font-size: 26rpx; }
.clear-btn { font-size: 24rpx; color: #c0c4cc; padding: 4rpx; }
.announce-card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05); }
.ac-header { display: flex; align-items: flex-start; gap: 12rpx; margin-bottom: 16rpx; }
.top-tag { background: #fdf6ec; color: #e6a23c; font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 4rpx; white-space: nowrap; flex-shrink: 0; }
.ac-title { font-size: 30rpx; font-weight: 600; color: #303133; line-height: 1.5; flex: 1; }
.ac-footer { display: flex; justify-content: space-between; align-items: center; }
.ac-publisher { font-size: 24rpx; color: #909399; }
.ac-time      { font-size: 24rpx; color: #c0c4cc; }
.load-more { text-align: center; padding: 24rpx; font-size: 26rpx; color: #409eff; }
.no-more   { text-align: center; padding: 24rpx; font-size: 24rpx; color: #c0c4cc; }
</style>
