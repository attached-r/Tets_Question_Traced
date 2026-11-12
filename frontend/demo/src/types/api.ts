/** 与后端 Result<T> 一一对应；HTTP 恒为 200，业务结果看 code */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 与后端 PageResult<T> 一一对应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 字典项 */
export interface Option {
  code: string
  label: string
}

export interface UserVO {
  id: number
  username: string
  realName: string
  roleCode: string
  roleLabel: string
}

export interface LoginResult {
  token: string
  /** 有效期，单位秒 */
  expiresIn: number
  user: UserVO
}

export interface DictResult {
  types: Option[]
  statuses: Option[]
}
