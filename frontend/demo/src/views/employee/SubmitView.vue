<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { feedbackApi } from '@/api/feedback'
import { metaApi } from '@/api/meta'
import { BizError } from '@/api/http'
import { useDictStore } from '@/stores/dict'
import { genRequestId } from '@/utils/requestId'
import type { TypeSuggest } from '@/types/feedback'

const router = useRouter()
const dict = useDictStore()

const requestId = ref(genRequestId())
const typeCode = ref('')
const content = ref('')
const submitting = ref(false)
const suggesting = ref(false)
const suggest = ref<TypeSuggest | null>(null)

function newRequestId() {
  requestId.value = genRequestId()
}

onMounted(() => {
  void dict.load()
})

async function runSuggest() {
  if (content.value.trim().length < 6) {
    ElMessage.warning('先输入反馈内容再做识别')
    return
  }

  suggesting.value = true
  try {
    suggest.value = await metaApi.suggestType(content.value)
    if (suggest.value.confident) {
      // 建议只回填，用户仍然可以改；落库的是用户确认过的值
      typeCode.value = suggest.value.typeCode
      ElMessage.success(`建议类型：${suggest.value.typeLabel}（已填入，可修改）`)
    } else {
      ElMessage.info('信息不足，系统不猜测，建议选择"待确认"由技术支持复核')
    }
  } finally {
    suggesting.value = false
  }
}

async function submit() {
  if (!requestId.value.trim()) {
    ElMessage.warning('提交编号不能为空')
    return
  }
  if (!typeCode.value) {
    ElMessage.warning('请选择反馈类型')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('反馈内容不能为空白')
    return
  }

  submitting.value = true
  try {
    const created = await feedbackApi.create({
      requestId: requestId.value.trim(),
      typeCode: typeCode.value,
      content: content.value,
    })
    ElMessage.success(`提交成功，编号 ${created.requestId}`)

    requestId.value = genRequestId()
    typeCode.value = ''
    content.value = ''
    suggest.value = null

    await router.push({ name: 'my' })
  } catch (error) {
    // 40901 = 相同提交编号已存在；后端不会覆盖原文，这里给一个回查入口
    if (error instanceof BizError && error.code === 40901) {
      try {
        await ElMessageBox.confirm('该提交编号已存在，是否去"我的反馈"查看？', '重复提交', {
          confirmButtonText: '去查看',
          cancelButtonText: '留在本页',
          type: 'warning',
        })
        await router.push({ name: 'my' })
      } catch {
        // 用户选择留在本页
      }
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="card">
    <h3 class="title">提交反馈</h3>
    <p class="muted">
      原文按你填写的内容原样保存，不做改写；提交编号是幂等键，重复提交不会产生第二条记录。
    </p>

    <el-form label-width="80px" @submit.prevent>
      <el-form-item label="提交编号">
        <div class="row">
          <el-input v-model="requestId" style="width: 280px" maxlength="64" />
          <el-button @click="newRequestId">重新生成</el-button>
        </div>
      </el-form-item>

      <el-form-item label="反馈类型">
        <el-radio-group v-model="typeCode">
          <el-radio v-for="item in dict.types" :key="item.code" :value="item.code" border>
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="智能识别">
        <div class="row">
          <el-button :loading="suggesting" @click="runSuggest">识别类型</el-button>
          <span v-if="suggest" class="muted">
            {{ suggest.typeLabel }} / 置信度 {{ suggest.confidence }}
            <template v-if="suggest.matchedKeywords.length">
              · 命中：{{ suggest.matchedKeywords.join('、') }}
            </template>
          </span>
          <span v-else class="muted">仅给建议，最终类型由你选择</span>
        </div>
      </el-form-item>

      <el-form-item label="反馈原文">
        <el-input
          v-model="content"
          type="textarea"
          :rows="6"
          maxlength="2000"
          show-word-limit
          placeholder="把聊天里的原话贴进来即可，不要先改写"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.title {
  margin: 0 0 4px;
  font-size: 16px;
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
