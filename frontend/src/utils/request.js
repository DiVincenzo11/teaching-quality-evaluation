import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：自动附加 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('无权限访问')
      } else {
        ElMessage.error(data?.message || '服务器错误，请稍后重试')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  },
)

export default request
