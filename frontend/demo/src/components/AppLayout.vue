<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.clear()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand">问题登记小助手</div>
      <nav class="nav">
        <router-link to="/submit">提交反馈</router-link>
        <router-link to="/my">我的反馈</router-link>
        <router-link v-if="auth.isSupport" to="/board">反馈看板</router-link>
      </nav>
      <div class="user">
        <span>{{ auth.user?.realName }}（{{ auth.user?.roleLabel }}）</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </header>

    <main class="page">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 52px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.brand {
  font-weight: 600;
}

.nav {
  display: flex;
  gap: 16px;
  flex: 1;
}

.nav a {
  text-decoration: none;
  color: #606266;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
}

.nav a.router-link-active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.page {
  flex: 1;
  padding: 16px 20px 40px;
}
</style>
