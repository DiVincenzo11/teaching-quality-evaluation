import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// 🌟 新增：安全获取用户信息的辅助函数（彻底杜绝 JSON.parse 报错导致的白屏）
const getUserInfo = () => {
  try {
    const infoStr = localStorage.getItem('userInfo')
    // 如果没有数据，或者存的是 'undefined' 字符串，都返回 null
    if (!infoStr || infoStr === 'undefined') return null 
    return JSON.parse(infoStr)
  } catch (error) {
    console.error('本地存储的用户数据格式错误，已自动清理', error)
    localStorage.removeItem('userInfo') // 清理掉引发崩溃的脏数据
    return null
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      
        path: '/',
        redirect: '/login' // 强制直接跳登录页
      
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true },
    },
    // ===== 管理员路由 =====
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { role: 3 },
      children: [
        { path: '',         redirect: '/admin/dashboard' },
        { path: 'dashboard',    name: 'AdminDashboard',    component: () => import('@/views/admin/Dashboard.vue') },
        {
          path: 'users',
          component: () => import('@/views/admin/UserManagement.vue'),
          children: [
            { path: '',         redirect: '/admin/users/students' },
            { path: 'students', name: 'StudentMgmt', component: () => import('@/views/admin/StudentMgmt.vue') },
            { path: 'teachers', name: 'TeacherMgmt', component: () => import('@/views/admin/TeacherMgmt.vue') },
            { path: 'admins',   name: 'AdminMgmt',   component: () => import('@/views/admin/AdminMgmt.vue') },
          ],
        },
        { path: 'questions',    name: 'QuestionManagement',component: () => import('@/views/admin/QuestionManagement.vue') },
        { path: 'keywords',     name: 'KeywordManagement', component: () => import('@/views/admin/KeywordManagement.vue') },
        { path: 'announcements',name: 'AnnounceMgmt',      component: () => import('@/views/admin/AnnouncementManagement.vue') },
        { path: 'semesters',    name: 'SemesterMgmt',      component: () => import('@/views/admin/SemesterManagement.vue') },
        { path: 'analysis',     name: 'AnalysisData',      component: () => import('@/views/admin/AnalysisData.vue') },
        { path: 'feedback',     name: 'AdminFeedback',     component: () => import('@/views/admin/FeedbackView.vue') },
      ],
    },
    // ===== 教师路由 =====
    {
      path: '/teacher',
      component: () => import('@/views/teacher/TeacherLayout.vue'),
      meta: { role: 2 },
      children: [
        { path: '',        redirect: '/teacher/dashboard' },
        { path: 'dashboard', name: 'TeacherDashboard', component: () => import('@/views/teacher/Dashboard.vue') },
        { path: 'eval',      name: 'TeacherEval',      component: () => import('@/views/teacher/EvaluationView.vue') },
        { path: 'report',    name: 'TeacherReport',    component: () => import('@/views/teacher/TeacherReport.vue') },
        { path: 'feedback',  name: 'TeacherFeedback',  component: () => import('@/views/teacher/FeedbackView.vue') },
        { path: 'announce',  name: 'TeacherAnnounce',  component: () => import('@/views/teacher/AnnouncementView.vue') },
        { path: 'profile',   name: 'TeacherProfile',   component: () => import('@/views/ProfileView.vue') },
      ],
    },
    // ===== 学生路由 =====
    {
      path: '/student',
      component: () => import('@/views/student/StudentLayout.vue'),
      meta: { role: 1 },
      children: [
        { path: '',         redirect: '/student/dashboard' },
        { path: 'dashboard', name: 'StudentDashboard', component: () => import('@/views/student/Dashboard.vue') },
        { path: 'evaluate',  name: 'EvaluateTeacher',  component: () => import('@/views/student/EvaluateTeacher.vue') },
        { path: 'history',   name: 'EvalHistory',      component: () => import('@/views/student/EvalHistory.vue') },
        { path: 'feedback',  name: 'TeacherFeedbackView', component: () => import('@/views/student/TeacherFeedbackView.vue') },
        { path: 'announce',  name: 'StudentAnnounce',  component: () => import('@/views/student/AnnouncementView.vue') },
        { path: 'profile',   name: 'StudentProfile',   component: () => import('@/views/ProfileView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

// 路由守卫
router.beforeEach((to) => {
  // 🌟 修改：使用安全获取函数
  const userInfo = getUserInfo()
  const token    = localStorage.getItem('token')

  // 1. 公开页面直接放行（比如 /login）
  if (to.meta.public) return true

  // 2. 没有 token 或信息，拦截回登录页
  if (!token || !userInfo) {
    ElMessage.warning('请先登录')
    return '/login'
  }

  // 3. 权限判断
  if (to.meta.role && userInfo.roleType !== to.meta.role && userInfo.roleType !== 3) {
    ElMessage.error('无权限访问')
    return '/' // 这里跳回根目录，根目录的 redirect 会自动把他送回该去的身份主页
  }

  return true
})

export default router