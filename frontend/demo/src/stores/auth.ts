import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/api/http'
import type { UserVO } from '@/types/api'

function readCachedUser(): UserVO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserVO
  } catch {
    return null
  }
}

/**
 * 登录状态。
 * token 与用户信息都落 localStorage：刷新页面不用重新登录，
 * 真正的权限判断仍在后端（前端只做体验层拦截）。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<UserVO | null>(readCachedUser())

  const role = computed(() => user.value?.roleCode ?? '')
  const isSupport = computed(() => role.value === 'SUPPORT' || role.value === 'ADMIN')
  const isLogin = computed(() => token.value !== '')

  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    token.value = result.token
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  function clear() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /** 用 /api/auth/me 校准本地缓存的角色，避免缓存过期导致菜单不对 */
  async function refreshMe() {
    const me = await authApi.me()
    user.value = me
    localStorage.setItem(USER_KEY, JSON.stringify(me))
  }

  return { token, user, role, isSupport, isLogin, login, clear, refreshMe }
})
