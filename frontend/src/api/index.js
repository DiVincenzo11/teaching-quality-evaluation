import request from '@/utils/request'

// ===== 认证 API =====
export const authApi = {
  register: (data) => request.post('/auth/register', data),
}

// ===== 通用 API（所有已登录用户可用）=====
export const commonApi = {
  semesters: () => request.get('/common/semesters'),
}

// ===== 教师 API =====
export const teacherApi = {
  list: (keyword, department, semesterId) => request.get('/teacher/list', { params: { keyword, department, semesterId } }),
  listForStudent: (semesterId, keyword)   => request.get('/teacher/list/for-student', { params: { semesterId, keyword } }),
  evalStats:(semesterId)          => request.get('/teacher/eval/stats', { params: { semesterId } }),
  semesters:()                    => request.get('/teacher/eval/semesters'),
  info: (semesterId)              => request.get('/teacher/info', { params: { semesterId } }),
  saveFeedback: (data)            => request.post('/teacher/feedback', data),
  myFeedback:   ()                => request.get('/teacher/feedback'),
  report:       (semesterId)      => request.get('/teacher/report', {
    params: semesterId ? { semesterId } : {},
    timeout: 30000,
  }),
}

// ===== 学生 API =====
export const studentApi = {
  submit:            (data)                  => request.post('/student/eval/submit', data),
  canEvaluate:       (teacherId, semesterId) => request.get('/student/eval/can-evaluate', { params: { teacherId, semesterId } }),
  history:           ()                      => request.get('/student/eval/history'),
  historyBySemester: (semesterId)            => request.get('/student/eval/history/by-semester', { params: { semesterId } }),
  historyAll:        ()                     => request.get('/student/eval/history/all'),
  evaluatedSemesters: ()                     => request.get('/student/eval/history/semesters'),
  evalRecordDetail:   (teacherId, semesterId) => request.get('/student/eval/record/detail', { params: { teacherId, semesterId } }),
  feedbackList:      (semesterId)            => request.get('/student/feedback/list', { params: { semesterId } }),
  allSemesters:      ()                      => request.get('/student/semesters/current'),
}

// ===== 问卷题目 API =====
export const questionApi = {
  list: () => request.get('/questions'),
}

// ===== 公告 API =====
export const announcementApi = {
  list:        (page, size, keyword) => request.get('/announcements',        { params: { page, size, keyword } }),
  publicList:  (page, size)          => request.get('/announcements/public', { params: { page, size } }),
  getById:     (id)                  => request.get(`/announcements/${id}`),
  create:      (data)                => request.post('/admin/announcements', data),
  update:      (id, data)            => request.put(`/admin/announcements/${id}`, data),
  delete:      (id)                  => request.delete(`/admin/announcements/${id}`),
}

// ===== 管理员用户 API =====
export const adminUserApi = {
  page:        (params)   => request.get('/admin/users', { params }),
  create:      (data)     => request.post('/admin/users', data),
  update:      (id, data) => request.put(`/admin/users/${id}`, data),
  delete:      (id)       => request.delete(`/admin/users/${id}`),
  updateStatus:(id, status) => request.put(`/admin/users/${id}/status`, { status }),
  resetPwd:    (id, password) => request.put(`/admin/users/${id}/reset-password`, { password }),
}

// ===== 管理员评价题目 API =====
export const adminQuestionApi = {
  list:      ()              => request.get('/admin/questions'),
  create:    (data)          => request.post('/admin/questions', data),
  update:    (id, data)      => request.put(`/admin/questions/${id}`, data),
  delete:    (id)            => request.delete(`/admin/questions/${id}`),
  rebalance: (essayScore)    => request.put('/admin/questions/rebalance', { essayScore }),
  getPreConfig:    ()        => request.get('/admin/pre-question-config'),
  savePreConfig:   (list)    => request.put('/admin/pre-question-config', list),
}

// ===== 管理员关键词 API =====
export const adminKeywordApi = {
  list:   (wordType) => request.get('/admin/keywords', { params: { wordType } }),
  create: (data)     => request.post('/admin/keywords', data),
  update: (id, data) => request.put(`/admin/keywords/${id}`, data),
  delete: (id)       => request.delete(`/admin/keywords/${id}`),
}

// ===== 管理员学期 API =====
export const adminSemesterApi = {
  list:       ()         => request.get('/admin/semesters'),
  create:     (data)     => request.post('/admin/semesters', data),
  update:     (id, data) => request.put(`/admin/semesters/${id}`, data),
  delete:     (id)       => request.delete(`/admin/semesters/${id}`),
  setCurrent: (id)       => request.put(`/admin/semesters/${id}/set-current`),
}

// ===== 管理员分析 API =====
export const adminAnalysisApi = {
  teacherStats:        (semesterId)            => request.get('/admin/analysis/teacher-stats', { params: { semesterId } }),
  teacherDetail:       (teacherId, semesterId) => request.get(`/admin/analysis/teacher/${teacherId}`, { params: { semesterId } }),
  teacherReport:       (teacherId, semesterId) => request.get(`/admin/analysis/teacher/${teacherId}/report`, { params: { semesterId } }),
  dynamicWeights:      (semesterId, courseType)=> request.get('/admin/analysis/dynamic-weights', { params: { semesterId, courseType } }),
  teacherClusters:     (semesterId)            => request.get('/admin/analysis/teacher-clusters', { params: { semesterId } }),
  recalculateWeights:  (semesterId)            => request.post('/admin/analysis/recalculate-weights', null, { params: { semesterId } }),
  engagementStats:     (semesterId)            => request.get('/admin/analysis/engagement-stats', { params: { semesterId } }),
}

// ===== 管理员反馈 API =====
export const adminFeedbackApi = {
  list: (params) => request.get('/admin/feedback', { params }),
}

// ===== 管理员概览 API =====
export const adminDashboardApi = {
  overview: () => request.get('/admin/dashboard/overview'),
}

// ===== 个人中心 API =====
export const profileApi = {
  getInfo:      ()     => request.get('/profile'),
  update:       (data) => request.put('/profile', data),
  changePwd:    (data) => request.put('/profile/password', data),
  getPhoneLimit:()     => request.get('/profile/phone-limit'),
  uploadAvatar: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/profile/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
}
