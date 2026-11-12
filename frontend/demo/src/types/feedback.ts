/** 类型码与后端 FeedbackType 枚举一一对应 */
export const FEEDBACK_TYPES = [
  'SYSTEM_FAULT',
  'PERMISSION',
  'FEATURE',
  'CONSULT',
  'UNDETERMINED',
] as const

export type FeedbackTypeCode = (typeof FEEDBACK_TYPES)[number]

/** 状态码与后端一致：0 待处理 1 处理中 2 已完成 */
export type FeedbackStatusCode = 0 | 1 | 2

export interface FeedbackListItem {
  id: number
  requestId: string
  typeCode: string
  typeLabel: string
  status: number
  statusLabel: string
  submitterName: string
  handlerName?: string
  contentPreview: string
  createTime: string
}

export interface FeedbackLogItem {
  id: number
  operatorName?: string
  operatorRole?: string
  operateType: number
  operateTypeLabel: string
  fromStatusLabel?: string
  toStatusLabel?: string
  fromTypeLabel?: string
  toTypeLabel?: string
  /** 内部操作说明：后端对员工角色不下发该字段 */
  remark?: string
  createTime: string
}

export interface FeedbackDetail {
  id: number
  requestId: string
  typeCode: string
  typeLabel: string
  /** 1 提交者选择 2 处理人复判 */
  typeSource: number
  status: number
  statusLabel: string
  submitterName: string
  handlerName?: string
  /** 反馈原文，未做任何改写 */
  content: string
  /** 内部备注：仅技术支持角色的响应里才有这个字段 */
  handlerNote?: string
  finishTime?: string
  version: number
  createTime: string
  updateTime: string
  logs: FeedbackLogItem[]
}

export interface CreatedFeedback {
  id: number
  requestId: string
  typeCode: string
  typeLabel: string
  status: number
  statusLabel: string
  createTime: string
}

export interface TypeSuggest {
  typeCode: string
  typeLabel: string
  /** HIGH / MEDIUM / LOW */
  confidence: string
  confident: boolean
  score: number
  matchedKeywords: string[]
  tip: string
}
