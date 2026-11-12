import { http } from './http'
import type { DictResult } from '@/types/api'
import type { TypeSuggest } from '@/types/feedback'

export const metaApi = {
  dict: () => http.get<DictResult>('/meta/dict'),

  /** 类型建议：后端纯规则打分，不落库；没有模型 Key 也能用 */
  suggestType: (content: string) => http.post<TypeSuggest>('/meta/type-suggest', { content }),
}
