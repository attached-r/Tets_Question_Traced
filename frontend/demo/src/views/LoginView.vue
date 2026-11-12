<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('xli')
const password = ref('123456')
const loading = ref(false)

const demoAccounts = [
  { username: 'xli', name: '小李', role: '员工' },
  { username: 'xchen', name: '小陈', role: '员工' },
  { username: 'support01', name: '张工', role: '技术支持' },
]

function pick(account: string) {
  username.value = account
  password.value = '123456'
}

async function submit() {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || (auth.isSupport ? '/board' : '/submit'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-card">
      <h2>企业内部问题登记小助手</h2>
      <p class="muted">员工提交反馈并跟踪进度，技术支持查询与处理。</p>

      <el-form label-width="64px" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="username" placeholder="登录账号" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="密码" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo">
        <div class="muted">演示账号（密码都是 123456）：</div>
        <div class="demo-list">
          <el-button
            v-for="account in demoAccounts"
            :key="account.username"
            size="small"
            @click="pick(account.username)"
          >
            {{ account.username }} / {{ account.name }} / {{ account.role }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 420px;
  padding: 28px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.login-card h2 {
  margin: 0 0 6px;
  font-size: 18px;
}

.demo {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.demo-list .el-button {
  margin-left: 0;
}
</style>
