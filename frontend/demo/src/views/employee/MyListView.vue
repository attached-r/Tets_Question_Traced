<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { feedbackApi } from '@/api/feedback'
import { useDictStore } from '@/stores/dict'
import FeedbackDetailDrawer from '@/components/FeedbackDetailDrawer.vue'
import StatusTag from '@/components/StatusTag.vue'
import TypeTag from '@/components/TypeTag.vue'
import type { FeedbackListItem } from '@/types/feedback'

const dict = useDictStore()

const rows = ref<FeedbackListItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref<number | undefined>(undefined)
const loading = ref(false)

const drawerVisible = ref(false)
const activeId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await feedbackApi.myList({
      page: page.value,
      size: size.value,
      status: status.value,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function openRow(row: FeedbackListItem) {
  activeId.value = row.id
  drawerVisible.value = true
}

function onStatusChange() {
  page.value = 1
  void load()
}

function onPageChange(next: number) {
  page.value = next
  void load()
}

onMounted(async () => {
  await dict.load()
  await load()
})
</script>

<template>
  <div class="card">
    <div class="toolbar">
      <span class="title">我的反馈</span>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="onStatusChange">
        <el-option v-for="item in dict.statuses" :key="item.code" :label="item.label" :value="Number(item.code)" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <span class="muted">共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="rows" border size="small" @row-click="openRow">
      <el-table-column prop="requestId" label="提交编号" width="170" />
      <el-table-column prop="contentPreview" label="内容摘要" min-width="260" show-overflow-tooltip />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <TypeTag :label="row.typeLabel" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :status="row.status" :label="row.statusLabel" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="还没有提交过反馈" />

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <FeedbackDetailDrawer
      v-model="drawerVisible"
      :feedback-id="activeId"
      :can-operate="false"
      @changed="load"
    />
  </div>
</template>

<style scoped>
.title {
  font-weight: 600;
  margin-right: 8px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
