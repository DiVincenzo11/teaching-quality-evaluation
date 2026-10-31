<template>
  <div class="dashboard">
    <div class="welcome-banner">
      <div>
        <h2>{{ userStore.userInfo?.realName }} 同学，欢迎您 👋</h2>
        <p>请认真参与本学期的教学质量评价，您的评价是教师改进教学的重要依据</p>
      </div>
      <el-button type="primary" plain @click="$router.push('/student/evaluate')">
        立即评价
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>最新通知公告</span></template>
          <div v-for="item in announces" :key="item.id" class="announce-item"
               @click="$router.push('/student/announce')">
            <el-icon v-if="item.topFlag" color="#e6a23c"><StarFilled /></el-icon>
            <span class="a-title">{{ item.title }}</span>
            <span class="a-time">{{ item.createTime }}</span>
          </div>
          <el-empty v-if="!announces.length" description="暂无公告" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>我的评价概况</span></template>
          <div class="my-stats">
            <div class="stat-item">
              <div class="stat-num">{{ historyCount }}</div>
              <div class="stat-lbl">已评价教师数</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ currentSemesterName }}</div>
              <div class="stat-lbl">当前学期</div>
            </div>
          </div>
          <el-button type="primary" style="width:100%; margin-top:16px"
                     @click="$router.push('/student/history')">
            查看评价历史
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { announcementApi, studentApi, adminSemesterApi } from '@/api/index'

const userStore         = useUserStore()
const announces         = ref([])
const historyCount      = ref(0)
const currentSemesterName = ref('-')

onMounted(async () => {
  try {
    const aRes = await announcementApi.list(1, 5)
    announces.value = aRes.data?.records || []

    const hRes = await studentApi.history()
    historyCount.value = hRes.data?.length || 0

    const sRes = await studentApi.currentSemesters()
    const cur  = sRes.data?.[0]
    currentSemesterName.value = cur?.semesterName?.slice(0, 14) || '-'
  } catch {}
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  border-radius: 16px;
  padding: 28px 32px;
  color: white;
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.welcome-banner h2 { font-size: 20px; margin-bottom: 6px; }
.welcome-banner p  { opacity: 0.9; font-size: 14px; }
.announce-item {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 8px; border-bottom: 1px solid #f0f0f0;
  cursor: pointer; transition: background 0.15s; border-radius: 6px;
}
.announce-item:hover { background: #f5f7fa; }
.a-title { flex: 1; font-size: 14px; color: #303133; }
.a-time  { font-size: 12px; color: #8c9ab0; white-space: nowrap; }
.my-stats { display: flex; gap: 16px; }
.stat-item { flex: 1; text-align: center; padding: 16px; background: #f5f7fa; border-radius: 8px; }
.stat-num { font-size: 28px; font-weight: 700; color: #11998e; }
.stat-lbl { font-size: 12px; color: #8c9ab0; margin-top: 4px; }
</style>
