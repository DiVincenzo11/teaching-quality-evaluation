<template>
  <view class="tab-bar" :style="{ paddingBottom: safeAreaBottom + 'px' }">
    <view
      v-for="item in tabs"
      :key="item.path"
      class="tab-item"
      :class="{ active: currentPath === item.path }"
      @click="switchTab(item)"
    >
      <text class="tab-icon">{{ item.icon }}</text>
      <text class="tab-text">{{ item.text }}</text>
      <view v-if="currentPath === item.path" class="active-dot" />
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const safeAreaBottom = ref(0)

const systemInfo = uni.getSystemInfoSync()
if (systemInfo.safeAreaInsets) {
  safeAreaBottom.value = systemInfo.safeAreaInsets.bottom || 0
}

const props = defineProps({
  currentPath: { type: String, default: '' }
})

const studentTabs = [
  { text: '首页', icon: '🏠', path: 'pages/student/index'    },
  { text: '评价', icon: '✏️', path: 'pages/student/evaluate'  },
  { text: '历史', icon: '📋', path: 'pages/student/history'   },
  { text: '反馈', icon: '💬', path: 'pages/student/feedback'  },
  { text: '我的', icon: '👤', path: 'pages/profile/index'     },
]

const teacherTabs = [
  { text: '首页', icon: '🏠', path: 'pages/teacher/index'     },
  { text: '评价', icon: '📊', path: 'pages/teacher/eval-view' },
  { text: '反思', icon: '✍️', path: 'pages/teacher/feedback'  },
  { text: '我的', icon: '👤', path: 'pages/profile/index'     },
]

const adminTabs = [
  { text: '概览', icon: '📈', path: 'pages/admin/index'    },
  { text: '分析', icon: '🔬', path: 'pages/admin/analysis' },
  { text: '我的', icon: '👤', path: 'pages/profile/index'  },
]

const tabs = computed(() => {
  if (userStore.isAdmin)   return adminTabs
  if (userStore.isTeacher) return teacherTabs
  return studentTabs
})

function switchTab(item) {
  if (props.currentPath === item.path) return
  uni.switchTab({ url: '/' + item.path })
}
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  display: flex;
  align-items: flex-start;
  padding-top: 16rpx;
  box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.08);
  z-index: 999;
  border-top: 2rpx solid #f0f0f0;
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 0 8rpx;
  position: relative;
  cursor: pointer;
  transition: all 0.2s;
}
.tab-icon { font-size: 40rpx; line-height: 1; }
.tab-text { font-size: 20rpx; color: #888; line-height: 1; }
.tab-item.active .tab-text { color: #1a2a6c; font-weight: 600; }
.active-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #1a2a6c;
  position: absolute;
  bottom: -2rpx;
}
</style>
