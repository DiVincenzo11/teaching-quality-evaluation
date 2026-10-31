const BASE_URL = 'http://localhost:8080/api'

function cleanParams(obj) {
  if (!obj || typeof obj !== 'object') return obj
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v != null && v !== '')
  )
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const rawData = options.data ?? options.params
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: cleanParams(rawData),
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
      },
      success(res) {
        if (res.statusCode === 401) {
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          setTimeout(() => uni.reLaunch({ url: '/pages/login/index' }), 1200)
          reject(new Error('Unauthorized'))
          return
        }
        if (res.statusCode === 403) {
          uni.showToast({ title: '无权限访问', icon: 'none' })
          reject(new Error('Forbidden'))
          return
        }
        if (res.statusCode !== 200) {
          const msg = res.data?.message || '服务器异常，请稍后重试'
          uni.showToast({ title: msg, icon: 'none', duration: 2500 })
          reject(new Error(msg))
          return
        }
        const data = res.data
        if (data.code === 200) {
          resolve(data)
        } else {
          uni.showToast({ title: data.message || '操作失败', icon: 'none', duration: 2000 })
          reject(new Error(data.message))
        }
      },
      fail(err) {
        uni.showToast({ title: '网络连接失败，请检查网络', icon: 'none' })
        reject(err)
      },
    })
  })
}

export const http = {
  get(url, params) {
    return request({ url, method: 'GET', params })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  },
  put(url, data) {
    return request({ url, method: 'PUT', data })
  },
  delete(url, params) {
    return request({ url, method: 'DELETE', params })
  },
}

export default http
