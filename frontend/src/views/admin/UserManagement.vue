<template>
  <div class="um-wrap">
    <!-- 三栏导航卡片 -->
    <div class="tab-nav">
      <router-link
        v-for="tab in tabs"
        :key="tab.to"
        :to="tab.to"
        custom
        v-slot="{ navigate, isActive, isExactActive }"
      >
        <div
          :class="['tab-card', { active: isActive || isExactActive }]"
          @click="navigate"
        >
          <div class="tab-icon-wrap">
            <el-icon size="28"><component :is="tab.icon" /></el-icon>
          </div>
          <div class="tab-body">
            <span class="tab-label">{{ tab.label }}</span>
            <span class="tab-desc">{{ tab.desc }}</span>
          </div>
          <el-icon class="tab-arrow"><ArrowRight /></el-icon>
        </div>
      </router-link>
    </div>

    <!-- 子页面内容 -->
    <router-view />
  </div>
</template>

<script setup>
const tabs = [
  { to: '/admin/users/students', label: '学生管理', desc: '管理学生账号、班级、学号信息', icon: 'User' },
  { to: '/admin/users/teachers', label: '教师管理', desc: '管理教师账号、院系、职称信息', icon: 'Avatar' },
  { to: '/admin/users/admins',   label: '管理员管理', desc: '管理系统管理员账号与权限', icon: 'Setting' },
]
</script>

<style scoped>
.um-wrap { display: flex; flex-direction: column; gap: 20px; }

.tab-nav {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.tab-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.tab-card:hover {
  border-color: #4f7ef8;
  box-shadow: 0 4px 16px rgba(79, 126, 248, 0.15);
  transform: translateY(-2px);
}
.tab-card.active {
  border-color: #4f7ef8;
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f0fe 100%);
  box-shadow: 0 4px 16px rgba(79, 126, 248, 0.2);
}

.tab-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: #f0f4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #4f7ef8;
  transition: background 0.2s;
}
.tab-card.active .tab-icon-wrap {
  background: #4f7ef8;
  color: #fff;
}

.tab-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tab-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.tab-desc {
  font-size: 12px;
  color: #909399;
}
.tab-card.active .tab-label { color: #1a2a6c; }

.tab-arrow {
  color: #c0c4cc;
  flex-shrink: 0;
  transition: color 0.2s;
}
.tab-card.active .tab-arrow,
.tab-card:hover .tab-arrow { color: #4f7ef8; }
</style>
