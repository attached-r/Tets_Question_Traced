import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 先装 pinia：路由守卫里要读 auth store，守卫在 app.use(router) 的首次导航时就会执行
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
