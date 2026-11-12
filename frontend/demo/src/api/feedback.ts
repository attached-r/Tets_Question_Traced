import { http } from './http'
import type { PageResult } from '@/types/api'
import type { CreatedFeedback, FeedbackDetail, FeedbackListItem } from '@/types/feedback'

export interface CreateFeedbackPayload {
  requestId: string
  typeCode: string
  content: string
}

export interface ListQuery {
  page?: number
  size?: number
  status?: number
  typeCode?: string
  keyword?: string
}

export const feedbackApi = {
  create: (payload: CreateFeedbackPayload) =>
    http.post<CreatedFeedback>('/feedback', payload),

  /** 员工端：只有自己的 */
  myList: (query: ListQuery) =>
    http.get<PageResult<FeedbackListItem>>('/feedback/my', query),

  /** 支持端：全部，可按状态/类型/关键字筛选 */
  board: (query: ListQuery) =>
    http.get<PageResult<FeedbackListItem>>('/feedback', query),

  detail: (id: number) => http.get<FeedbackDetail>(`/feedback/${id}`),

  /** 按提交编号回查（重复提交后的补偿入口） */
  byRequestId: (requestId: string) =>
    http.get<FeedbackDetail>(`/feedback/by-request/${encodeURIComponent(requestId)}`),

  updateStatus: (id: number, payload: { targetStatus: number; version: number; remark?: string }) =>
    http.patch<FeedbackDetail>(`/feedback/${id}/status`, payload),

  updateType: (id: number, payload: { typeCode: string; version: number }) =>
    http.patch<FeedbackDetail>(`/feedback/${id}/type`, payload),

  updateNote: (id: number, payload: { note: string; version: number }) =>
    http.patch<FeedbackDetail>(`/feedback/${id}/note`, payload),
}
