<template>
  <view class="page-bg">
    <nav-bar title="首页" />
    <view class="welcome-banner">
      <view>
        <text class="wb-title">{{ userStore.userInfo?.realName || '同学' }}，欢迎您 👋</text>
        <text class="wb-sub">请认真参与本学期的教学质量评价</text>
      </view>
      <view class="wb-btn" @click="goEvaluate">立即评价</view>
    </view>

    <view class="main-content">
      <view class="stats-row">
        <view class="stat-card green">
          <text class="stat-num">{{ historyCount }}</text>
          <text class="stat-lbl">已评价教师</text>
        </view>
        <view class="stat-card blue" @click="goAnnounce">
          <text class="stat-num">{{ announces.length }}</text>
          <text class="stat-lbl">最新公告</text>
        </view>
        <view class="stat-card orange">
          <text class="stat-num">{{ currentSemesterShort }}</text>
          <text class="stat-lbl">当前学期</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title">快捷功能</view>
        <view class="quick-grid">
          <view class="quick-item" @click="goEvaluate">
            <view class="quick-icon" style="background: linear-gradient(135deg,#11998e,#38ef7d)">✏️</view>
            <text class="quick-text">评价教师</text>
          </view>
          <view class="quick-item" @click="goHistory">
            <view class="quick-icon" style="background: linear-gradient(135deg,#1a2a6c,#2d6a9f)">📋</view>
            <text class="quick-text">评价历史</text>
          </view>
          <view class="quick-item" @click="goFeedback">
            <view class="quick-icon" style="background: linear-gradient(135deg,#f093fb,#f5576c)">💬</view>
            <text class="quick-text">教师反馈</text>
          </view>
          <view class="quick-item" @click="goAnnounce">
            <view class="quick-icon" style="background: linear-gradient(135deg,#4facfe,#00f2fe)">📢</view>
            <text class="quick-text">通知公告</text>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-header-row">
          <text class="card-title" style="margin-bottom:0">最新公告</text>
          <text class="more-link" @click="goAnnounce">查看全部 ›</text>
        </view>
        <view v-if="loading" class="loading-box"><text>加载中...</text></view>
        <view v-else-if="!announces.length" class="empty-box"><text>暂无公告</text></view>
        <view
          v-for="item in announces"
          :key="item.id"
          class="announce-item"
          @click="goAnnounceDetail(item)"
        >
          <view v-if="item.topFlag" class="top-badge">置顶</view>
          <text class="a-title">{{ item.title }}</text>
          <text class="a-time">{{ item.createTime?.slice(0,10) }}</text>
        </view>
      </view>
    </view>

    <custom-tab-bar current-path="pages/student/index" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { announcementApi, studentApi } from '@/api/index'
import CustomTabBar from '@/components/custom-tab-bar/index.vue'

const userStore            = useUserStore()
const announces            = ref([])
const historyCount         = ref(0)
const currentSemesterShort = ref('-')
const loading              = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [aRes, hRes, sRes] = await Promise.allSettled([
      announcementApi.list(1, 5),
      studentApi.history(),
      studentApi.currentSemesters(),
    ])
    if (aRes.status === 'fulfilled') announces.value = aRes.value.data?.records || []
    if (hRes.status === 'fulfilled') historyCount.value = hRes.value.data?.length || 0
    if (sRes.status === 'fulfilled') {
      const name = sRes.value.data?.[0]?.semesterName || '-'
      currentSemesterShort.value = name.length > 8 ? name.slice(0, 8) + '…' : name
    }
  } finally { loading.value = false }
})

function goEvaluate()            { uni.switchTab({ url: '/pages/student/evaluate' }) }
function goHistory()             { uni.switchTab({ url: '/pages/student/history' }) }
function goFeedback()            { uni.switchTab({ url: '/pages/student/feedback' }) }
function goAnnounce()            { uni.navigateTo({ url: '/pages/student/announce' }) }
function goAnnounceDetail(item)  { uni.navigateTo({ url: `/pages/student/announce-detail?id=${item.id}` }) }
</script>

<style scoped>
.page-bg { min-height: 100vh; background: #f5f6fa; padding-bottom: 140rpx; }
.welcome-banner {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  padding: 60rpx 40rpx 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wb-title { display: block; font-size: 36rpx; font-weight: 700; color: #fff; margin-bottom: 8rpx; }
.wb-sub   { display: block; font-size: 24rpx; color: rgba(255,255,255,0.85); }
.wb-btn {
  background: rgba(255,255,255,0.25);
  color: #fff;
  border-radius: 40rpx;
  padding: 16rpx 32rpx;
  font-size: 26rpx;
  font-weight: 600;
  border: 2rpx solid rgba(255,255,255,0.5);
}
.main-content { padding: 24rpx; margin-top: -40rpx; }
.stats-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.stat-card {
  flex: 1; background: #fff; border-radius: 20rpx;
  padding: 28rpx 16rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.06);
}
.stat-num { display: block; font-size: 40rpx; font-weight: 700; margin-bottom: 8rpx; }
.stat-lbl { display: block; font-size: 20rpx; color: #909399; }
.green .stat-num  { color: #11998e; }
.blue  .stat-num  { color: #1a2a6c; }
.orange .stat-num { color: #e6a23c; font-size: 28rpx; }

.card-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.more-link { font-size: 24rpx; color: #409eff; }
.quick-grid { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 16rpx; }
.quick-item { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.quick-icon {
  width: 96rpx; height: 96rpx; border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.15);
}
.quick-text { font-size: 22rpx; color: #606266; }

.announce-item {
  display: flex; align-items: center; gap: 12rpx;
  padding: 24rpx 0; border-bottom: 2rpx solid #f5f5f5;
}
.announce-item:last-child { border-bottom: none; }
.top-badge {
  background: #fdf6ec; color: #e6a23c;
  font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 4rpx; flex-shrink: 0;
}
.a-title {
  flex: 1; font-size: 26rpx; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.a-time { font-size: 22rpx; color: #c0c4cc; white-space: nowrap; }
</style>
