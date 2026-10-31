<template>
  <view class="container" style="padding-bottom: 40rpx;">
    <view v-if="loading" class="loading-box"><text>加载中...</text></view>
    <template v-else-if="detail">
      <view class="detail-header card">
        <view v-if="detail.topFlag" class="top-tag">置顶</view>
        <text class="detail-title">{{ detail.title }}</text>
        <view class="detail-meta">
          <text class="meta-item">发布人：{{ detail.publisher }}</text>
          <text class="meta-item">{{ detail.createTime?.slice(0, 10) }}</text>
        </view>
      </view>

      <view class="detail-content card">
        <text class="content-text">{{ detail.content }}</text>
      </view>
    </template>
    <view v-else class="empty-box"><text>公告不存在</text></view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { announcementApi } from '@/api/index'

const detail  = ref(null)
const loading = ref(false)

const route = (() => {
  const pages = getCurrentPages()
  const page  = pages[pages.length - 1]
  return page?.options || {}
})()

onMounted(async () => {
  const id = route.id
  if (!id) return
  loading.value = true
  try {
    const res = await announcementApi.getById(id)
    detail.value = res.data
    if (detail.value?.title) uni.setNavigationBarTitle({ title: detail.value.title })
  } finally { loading.value = false }
})
</script>

<style scoped>
.detail-header { margin-bottom: 0; }
.top-tag {
  display: inline-block;
  background: #fdf6ec; color: #e6a23c;
  font-size: 22rpx; padding: 4rpx 16rpx;
  border-radius: 6rpx; margin-bottom: 16rpx;
}
.detail-title { display: block; font-size: 36rpx; font-weight: 700; color: #303133; line-height: 1.5; margin-bottom: 20rpx; }
.detail-meta  { display: flex; gap: 24rpx; }
.meta-item    { font-size: 24rpx; color: #909399; }

.content-text {
  font-size: 28rpx; color: #606266; line-height: 2;
  display: block; white-space: pre-wrap;
}
</style>
