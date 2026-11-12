<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: number; label?: string }>()

const tagType = computed<'primary' | 'success' | 'warning'>(() => {
  if (props.status === 0) return 'warning'
  if (props.status === 1) return 'primary'
  return 'success'
})

const text = computed(() => {
  if (props.label) return props.label
  if (props.status === 0) return '待处理'
  if (props.status === 1) return '处理中'
  return '已完成'
})
</script>

<template>
  <el-tag :type="tagType" size="small" disable-transitions>{{ text }}</el-tag>
</template>
