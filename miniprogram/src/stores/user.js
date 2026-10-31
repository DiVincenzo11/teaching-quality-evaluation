import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token    = ref(uni.getStorageSync('token') || '')
  const userInfo = ref((() => {
    try { return JSON.parse(uni.getStorageSync('userInfo') || 'null') } catch { return null }
  })())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin    = computed(() => userInfo.value?.roleType === 3)
  const isTeacher  = computed(() => userInfo.value?.roleType === 2)
  const isStudent  = computed(() => userInfo.value?.roleType === 1)
  const roleType   = computed(() => userInfo.value?.roleType)

  async function login(username, password, rType) {
    const res = await http.post('/auth/login', { username, password, roleType: rType })
    token.value    = res.data.token
    userInfo.value = res.data
    uni.setStorageSync('token',    res.data.token)
    uni.setStorageSync('userInfo', JSON.stringify(res.data))
    return res.data
  }

  async function logout() {
    try { await http.post('/auth/logout') } catch {}
    token.value    = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  return { token, userInfo, isLoggedIn, isAdmin, isTeacher, isStudent, roleType, login, logout }
})
