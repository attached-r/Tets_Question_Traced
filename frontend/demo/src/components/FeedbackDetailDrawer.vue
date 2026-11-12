<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { feedbackApi } from '@/api/feedback'
import { useDictStore } from '@/stores/dict'
import StatusTag from './StatusTag.vue'
import TypeTag from './TypeTag.vue'
import type { FeedbackDetail } from '@/types/feedback'

const props = defineProps<{
  modelValue: boolean
  feedbackId: number | null
  canOperate: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'changed'): void
}>()

const dict = useDictStore()

const detail = ref<FeedbackDetail | null>(null)
const loading = ref(false)
const saving = ref(false)
const typeDraft = ref('')
const noteDraft = ref('')
const remark = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

/** 用"是否打开 + 哪一条"合成一个 key，避免 watch 元组带来的类型噪音 */
const openKey = computed(() => (props.modelValue ? props.feedbackId ?? 0 : 0))

function applyDetail(next: FeedbackDetail) {
  detail.value = next
  typeDraft.value = next.typeCode
  noteDraft.value = next.handlerNote ?? ''
}

async function load() {
  if (!props.feedbackId) return
  loading.value = true
  try {
    applyDetail(await feedbackApi.detail(props.feedbackId))
    remark.value = ''
  } finally {
    loading.value = false
  }
}

watch(
  openKey,
  async (key) => {
    if (key > 0) {
      await dict.load()
      await load()
    }
  },
  { immediate: true },
)

async function changeStatus(target: number) {
  if (!detail.value) return
  saving.value = true
  try {
    applyDetail(
      await feedbackApi.updateStatus(detail.value.id, {
        targetStatus: target,
        version: detail.value.version,
        remark: remark.value.trim() || undefined,
      }),
    )
    remark.value = ''
    ElMessage.success('状态已更新')
    emit('changed')
  } catch {
    // 40902 非法流转 / 40903 版本冲突：提示已由拦截器给出，这里刷新到最新状态
    await load()
  } finally {
    saving.value = false
  }
}

async function saveType() {
  if (!detail.value) return
  saving.value = true
  try {
    applyDetail(
      await feedbackApi.updateType(detail.value.id, {
        typeCode: typeDraft.value,
        version: detail.value.version,
      }),
    )
    ElMessage.success('类型已更新')
    emit('changed')
  } catch {
    await load()
  } finally {
    saving.value = false
  }
}

async function saveNote() {
  if (!detail.value) return
  saving.value = true
  try {
    applyDetail(
      await feedbackApi.updateNote(detail.value.id, {
        note: noteDraft.value,
        version: detail.value.version,
      }),
    )
    ElMessage.success('备注已保存')
    emit('changed')
  } catch {
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" title="反馈详情" size="600px">
    <div v-if="detail" v-loading="loading">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="提交编号">{{ detail.requestId }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <StatusTag :status="detail.status" :label="detail.statusLabel" />
        </el-descriptions-item>
        <el-descriptions-item label="反馈类型">
          <TypeTag :label="detail.typeLabel" :source="detail.typeSource" />
        </el-descriptions-item>
        <el-descriptions-item label="提交人">{{ detail.submitterName }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handlerName ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.finishTime" label="完成时间">
          {{ detail.finishTime }}
        </el-descriptions-item>
      </el-descriptions>

      <h4>反馈原文（原样保存，未改写）</h4>
      <pre class="content-pre">{{ detail.content }}</pre>

      <template v-if="canOperate">
        <h4>推进状态</h4>
        <div class="row">
          <el-button
            v-if="detail.status === 0"
            type="primary"
            :loading="saving"
            @click="changeStatus(1)"
          >
            开始处理
          </el-button>
          <el-button
            v-if="detail.status === 0"
            type="success"
            :loading="saving"
            @click="changeStatus(2)"
          >
            直接完成（咨询类）
          </el-button>
          <el-button
            v-if="detail.status === 1"
            type="success"
            :loading="saving"
            @click="changeStatus(2)"
          >
            标记完成
          </el-button>
          <span v-if="detail.status === 2" class="muted">已完成，状态不可再变更</span>
        </div>
        <el-input
          v-model="remark"
          placeholder="操作说明（内部可见，可选）"
          maxlength="200"
          style="margin-top: 8px"
        />

        <h4>类型复判</h4>
        <div class="row">
          <el-select v-model="typeDraft" :disabled="detail.status === 2" style="width: 160px">
            <el-option v-for="item in dict.types" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
          <el-button
            :disabled="detail.status === 2 || typeDraft === detail.typeCode"
            :loading="saving"
            @click="saveType"
          >
            保存类型
          </el-button>
        </div>

        <h4>内部备注（仅技术支持可见）</h4>
        <el-input v-model="noteDraft" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        <div class="row" style="margin-top: 8px">
          <el-button :loading="saving" @click="saveNote">保存备注</el-button>
          <span class="muted">提交者看不到这个字段，响应里根本不会下发</span>
        </div>
      </template>

      <h4>处理时间线</h4>
      <el-timeline v-if="detail.logs.length">
        <el-timeline-item
          v-for="log in detail.logs"
          :key="log.id"
          :timestamp="log.createTime"
          placement="top"
        >
          <div>{{ log.operateTypeLabel }} · {{ log.operatorName ?? '—' }}</div>
          <div v-if="log.fromStatusLabel || log.toStatusLabel" class="muted">
            状态：{{ log.fromStatusLabel ?? '—' }} → {{ log.toStatusLabel ?? '—' }}
          </div>
          <div v-if="log.fromTypeLabel || log.toTypeLabel" class="muted">
            类型：{{ log.fromTypeLabel ?? '—' }} → {{ log.toTypeLabel ?? '—' }}
          </div>
          <div v-if="log.remark" class="muted">{{ log.remark }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作记录" :image-size="60" />
    </div>
  </el-drawer>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
