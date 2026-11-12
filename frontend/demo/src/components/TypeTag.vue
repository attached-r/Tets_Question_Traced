<script setup lang="ts">
defineProps<{ label: string; source?: number }>()
</script>

<template>
  <el-tag type="info" effect="plain" size="small" disable-transitions>
    {{ label }}
    <span v-if="source === 2" class="muted">（复判）</span>
  </el-tag>
</template>
