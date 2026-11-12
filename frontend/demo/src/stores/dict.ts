import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { metaApi } from '@/api/meta'
import type { Option } from '@/types/api'

/**
 * 类型/状态字典。
 * 文案由后端下发，前端不硬编码中文，避免两边改得不一致。
 */
export const useDictStore = defineStore('dict', () => {
  const types = ref<Option[]>([])
  const statuses = ref<Option[]>([])

  const loaded = computed(() => types.value.length > 0)

  async function load() {
    if (loaded.value) return
    const dict = await metaApi.dict()
    types.value = dict.types
    statuses.value = dict.statuses
  }

  return { types, statuses, loaded, load }
})
