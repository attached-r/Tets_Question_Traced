import { http } from './http'
import type { LoginResult, UserVO } from '@/types/api'

export const authApi = {
  login: (payload: { username: string; password: string }) =>
    http.post<LoginResult>('/auth/login', payload),

  me: () => http.get<UserVO>('/auth/me'),
}
