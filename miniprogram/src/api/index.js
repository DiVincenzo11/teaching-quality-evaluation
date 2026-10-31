import http from '@/utils/request'

// ===== 认证 =====
export const authApi = {
  login:    (data) => http.post('/auth/login', data),
  logout:   ()     => http.post('/auth/logout'),
  register: (data) => http.post('/auth/register', data),
}

// ===== 公告 =====
export const announcementApi = {
  list:       (page, size, keyword) => http.get('/announcements', { page, size, keyword }),
  adminList:  (page, size, keyword) => http.get('/admin/announcements', { page, size, keyword }),
  publicList: (page, size)          => http.get('/announcements/public', { page, size }),
  getById:    (id)                  => http.get(`/announcements/${id}`),
  create:     (data)                => http.post('/admin/announcements', data),
  update:     (id, data)            => http.put(`/admin/announcements/${id}`, data),
  delete:     (id)                  => http.delete(`/admin/announcements/${id}`),
}

// ===== 教师 =====
export const teacherApi = {
  listForStudent: (semesterId, keyword) => http.get('/teacher/list/for-student', { semesterId, keyword }),
  evalStats:      (semesterId)          => http.get('/teacher/eval/stats', { semesterId }),
  semesters:      ()                    => http.get('/teacher/eval/semesters'),
  info:           (semesterId)          => http.get('/teacher/info', { semesterId }),
  saveFeedback:   (data)                => http.post('/teacher/feedback', data),
  myFeedback:     ()                    => http.get('/teacher/feedback'),
  report:         (semesterId)          => http.get('/teacher/report', semesterId ? { semesterId } : {}),
}

// ===== 学生 =====
export const studentApi = {
  submit:           (data)                   => http.post('/student/eval/submit', data),
  canEvaluate:      (teacherId, semesterId)  => http.get('/student/eval/can-evaluate', { teacherId, semesterId }),
  history:          ()                       => http.get('/student/eval/history'),
  historyBySemester:(semesterId)             => http.get('/student/eval/history/by-semester', { semesterId }),
  historyAll:       ()                       => http.get('/student/eval/history/all'),
  evalRecordDetail: (teacherId, semesterId)  => http.get('/student/eval/record/detail', { teacherId, semesterId }),
  feedbackList:     (semesterId)             => http.get('/student/feedback/list', { semesterId }),
  allSemesters:     ()                       => http.get('/student/semesters/current'),
  currentSemesters: ()                       => http.get('/student/semesters/current'),
}

// ===== 问卷题目 =====
export const questionApi = {
  list: () => http.get('/questions'),
}

// ===== 管理员：题目 =====
export const adminQuestionApi = {
  list:       ()           => http.get('/admin/questions'),
  create:     (data)       => http.post('/admin/questions', data),
  update:     (id, data)   => http.put(`/admin/questions/${id}`, data),
  delete:     (id)         => http.delete(`/admin/questions/${id}`),
  rebalance:  ()           => http.put('/admin/questions/rebalance'),
}

// ===== 管理员：关键词 =====
export const adminKeywordApi = {
  list:   (params)       => http.get('/admin/keywords', params),
  create: (data)         => http.post('/admin/keywords', data),
  update: (id, data)     => http.put(`/admin/keywords/${id}`, data),
  delete: (id)           => http.delete(`/admin/keywords/${id}`),
}

// ===== 管理员：用户 =====
export const adminUserApi = {
  page:         (params)         => http.get('/admin/users', params),
  create:       (data)           => http.post('/admin/users', data),
  update:       (id, data)       => http.put(`/admin/users/${id}`, data),
  delete:       (id)             => http.delete(`/admin/users/${id}`),
  updateStatus: (id, status)     => http.put(`/admin/users/${id}/status`, { status }),
  resetPwd:     (id, password)   => http.put(`/admin/users/${id}/reset-password`, { password }),
}

// ===== 管理员：学期 =====
export const adminSemesterApi = {
  list:       ()         => http.get('/admin/semesters'),
  create:     (data)     => http.post('/admin/semesters', data),
  update:     (id, data) => http.put(`/admin/semesters/${id}`, data),
  delete:     (id)       => http.delete(`/admin/semesters/${id}`),
  setCurrent: (id)       => http.put(`/admin/semesters/${id}/set-current`),
}

// ===== 管理员：分析 =====
export const adminAnalysisApi = {
  teacherStats:       (semesterId)             => http.get('/admin/analysis/teacher-stats', { semesterId }),
  teacherDetail:      (teacherId, semesterId)  => http.get(`/admin/analysis/teacher/${teacherId}`, { semesterId }),
  teacherReport:      (teacherId, semesterId)  => http.get(`/admin/analysis/teacher/${teacherId}/report`, { semesterId }),
  dynamicWeights:     (semesterId, courseType) => http.get('/admin/analysis/dynamic-weights', { semesterId, courseType }),
  teacherClusters:    (semesterId)             => http.get('/admin/analysis/teacher-clusters', { semesterId }),
  engagementStats:    (semesterId)             => http.get('/admin/analysis/engagement-stats', { semesterId }),
  recalculateWeights: (semesterId)             => http.post(`/admin/analysis/recalculate-weights${semesterId ? '?semesterId=' + semesterId : ''}`, null),
}

// ===== 管理员：反馈 =====
export const adminFeedbackApi = {
  list: (params) => http.get('/admin/feedback', params),
}

// ===== 管理员：概览 =====
export const adminDashboardApi = {
  overview:   ()            => http.get('/admin/dashboard/overview'),
  scoreDist:  (semesterId)  => http.get('/admin/dashboard/score-dist', semesterId ? { semesterId } : {}),
}

// ===== 个人中心 =====
export const profileApi = {
  getInfo:   ()     => http.get('/profile'),
  update:    (data) => http.put('/profile', data),
  changePwd: (data) => http.put('/profile/password', data),
}
