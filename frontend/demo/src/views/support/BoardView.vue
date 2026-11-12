<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
const typeCode = ref<string | undefined>(undefined)
const keyword = ref('')
const loading = ref(false)

const drawerVisible = ref(false)
const activeId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await feedbackApi.board({
      page: page.value,
      size: size.value,
      status: status.value,
      typeCode: typeCode.value || undefined,
      keyword: keyword.value.trim() || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void load()
}

function reset() {
  status.value = undefined
  typeCode.value = undefined
  keyword.value = ''
  page.value = 1
  void load()
}

function openRow(row: FeedbackListItem) {
  activeId.value = row.id
  drawerVisible.value = true
}

function onPageChange(next: number) {
  page.value = next
  void load()
}

/** 行内快捷操作：列表 VO 不带 version，所以先取详情再带版本提交 */
async function quickChange(row: FeedbackListItem, target: number) {
  try {
    const detail = await feedbackApi.detail(row.id)
    await feedbackApi.updateStatus(row.id, { targetStatus: target, version: detail.version })
    ElMessage.success(target === 1 ? '已开始处理' : '已标记完成')
    await load()
  } catch {
    // 业务错误（如版本冲突）已由拦截器提示，这里只保证不再往外抛
  }
}

onMounted(async () => {
  await dict.load()
  await load()
})
</script>

<template>
  <div class="card">
    <div class="toolbar">
      <span class="title">反馈看板</span>

      <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px">
        <el-option v-for="item in dict.statuses" :key="item.code" :label="item.label" :value="Number(item.code)" />
      </el-select>

      <el-select v-model="typeCode" placeholder="全部类型" clearable style="width: 140px">
        <el-option v-for="item in dict.types" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>

      <el-input
        v-model="keyword"
        placeholder="搜索原文或编号"
        style="width: 200px"
        clearable
        @keyup.enter="search"
      />

      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="muted">共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="rows" border size="small" @row-click="openRow">
      <el-table-column prop="requestId" label="编号" width="150" />
      <el-table-column prop="contentPreview" label="内容摘要" min-width="240" show-overflow-tooltip />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <TypeTag :label="row.typeLabel" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <StatusTag :status="row.status" :label="row.statusLabel" />
        </template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="90" />
      <el-table-column prop="handlerName" label="处理人" width="90" />
      <el-table-column prop="createTime" label="提交时间" width="160" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            link
            type="primary"
            @click.stop="quickChange(row, 1)"
          >
            开始处理
          </el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="success"
            @click.stop="quickChange(row, 2)"
          >
            完成
          </el-button>
          <span v-if="row.status === 2" class="muted">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="没有符合条件的反馈" />

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
      :can-operate="true"
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
