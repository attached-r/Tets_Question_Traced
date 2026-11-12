import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

export const TOKEN_KEY = 'ih_token'
export const USER_KEY = 'ih_user'

/** 业务错误：code 是后端 4xxxx 业务码，调用方可以据此分支处理（如 40901 重复编号） */
export class BizError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'BizError'
    this.code = code
  }
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body.code === 200) {
      return response
    }

    if (body.code === 401) {
      // 这里刻意不 import router / store：那会和 stores 形成循环依赖。
      // 清掉凭证后整页跳登录，状态重建比热切换更不容易出错。
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      ElMessage.error('登录已过期，请重新登录')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new BizError(body.code, body.message))
    }

    ElMessage.error(body.message)
    return Promise.reject(new BizError(body.code, body.message))
  },
  (error: unknown) => {
    ElMessage.error('网络异常，请确认后端已启动（http://localhost:8080）')
    return Promise.reject(error)
  },
)

/** 类型安全的薄封装：调用方直接拿到 T，而不是 AxiosResponse */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await instance.request<ApiResult<T>>(config)
  return response.data.data
}

export const http = {
  get: <T>(url: string, params?: object) => request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: unknown) => request<T>({ url, method: 'POST', data }),
  patch: <T>(url: string, data?: unknown) => request<T>({ url, method: 'PATCH', data }),
}
