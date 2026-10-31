import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token    = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn  = computed(() => !!token.value)
  const isAdmin     = computed(() => userInfo.value?.roleType === 3)
  const isTeacher   = computed(() => userInfo.value?.roleType === 2)
  const isStudent   = computed(() => userInfo.value?.roleType === 1)
  const roleType    = computed(() => userInfo.value?.roleType)

  async function login(username, password, roleType) {
    const res = await request.post('/auth/login', { username, password, roleType })
    token.value    = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token',    res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    return res.data
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } finally {
      token.value    = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  return { token, userInfo, isLoggedIn, isAdmin, isTeacher, isStudent, roleType, login, logout, setToken }
})
