<template>
  <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="nav-inner" :style="navInnerStyle">
      <!-- 左侧：有返回键时显示并占位，无时宽度为 0 -->
      <view class="nav-left" :class="{ 'nav-left-placeholder': canGoBack }">
        <view v-if="canGoBack" class="nav-back" @click="handleBack">
          <text class="nav-back-icon">‹</text>
          <text class="nav-back-text">返回</text>
        </view>
      </view>

      <!-- 中间：标题 + 副标题 -->
      <view class="nav-center">
        <text class="nav-title">{{ title }}</text>
        <text v-if="subtitle" class="nav-subtitle">{{ subtitle }}</text>
      </view>

      <!-- 右侧插槽：内容出现在胶囊按钮左边 -->
      <view class="nav-right">
        <slot name="right" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  title:    { type: String, default: '' },
  subtitle: { type: String, default: '' },
  backUrl:  { type: String, default: '' },
})

const statusBarHeight = ref(0)
const hasHistory      = ref(false)

// 胶囊按钮数据（px）
const menuBtnTop    = ref(0)
const menuBtnLeft   = ref(0)
const menuBtnHeight = ref(32)
const screenWidth   = ref(375)

const canGoBack = computed(() => hasHistory.value || !!props.backUrl)

/**
 * 导航内容区高度 = 胶囊按钮上下对称高度
 * = (menuBtnTop - statusBar) * 2 + menuBtnHeight
 */
const navContentHeight = computed(() => {
  const gap = menuBtnTop.value - statusBarHeight.value
  return Math.max(gap * 2 + menuBtnHeight.value, 44)
})

/**
 * nav-inner 右侧 padding = 屏幕宽度 - 胶囊按钮左边缘 + 8px 安全间隙
 * 确保所有内容不会伸入胶囊按钮区域
 */
const safeRightPadding = computed(() => {
  return Math.max(screenWidth.value - menuBtnLeft.value + 8, 20)
})

const navInnerStyle = computed(() => ({
  height:       navContentHeight.value + 'px',
  paddingLeft:  '24rpx',
  paddingRight: safeRightPadding.value + 'px',
}))

onMounted(() => {
  try {
    const info = uni.getWindowInfo()
    statusBarHeight.value = info.statusBarHeight || 20
    screenWidth.value     = info.windowWidth    || 375
  } catch {
    statusBarHeight.value = 20
    screenWidth.value     = 375
  }

  try {
    const btn = uni.getMenuButtonBoundingClientRect()
    if (btn && btn.top > 0) {
      menuBtnTop.value    = btn.top
      menuBtnLeft.value   = btn.left
      menuBtnHeight.value = btn.height || 32
    } else {
      throw new Error('invalid btn rect')
    }
  } catch {
    // 降级：按常见机型估算（状态栏下方 8px，胶囊宽 87px 距右 7px）
    menuBtnTop.value    = statusBarHeight.value + 8
    menuBtnLeft.value   = screenWidth.value - 87 - 7
    menuBtnHeight.value = 32
  }

  hasHistory.value = getCurrentPages().length > 1
})

function handleBack() {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
  } else if (props.backUrl) {
    uni.switchTab({ url: props.backUrl })
  }
}
</script>

<style scoped>
.nav-bar {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-inner {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 100%;
}
.nav-left {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.nav-left-placeholder {
  min-width: 130rpx;
}
.nav-back {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
}
.nav-back-icon {
  font-size: 52rpx;
  color: #fff;
  line-height: 1;
  margin-right: 2rpx;
}
.nav-back-text {
  font-size: 28rpx;
  color: #fff;
}
.nav-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.nav-subtitle {
  font-size: 22rpx;
  color: rgba(255,255,255,0.8);
  margin-top: 4rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.nav-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
}
</style>
