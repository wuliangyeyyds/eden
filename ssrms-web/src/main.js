// src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './assets/global.css'
import router from './router'

import request from './utils/request'  // ✅ 这里路径按你实际放置改，比如 ./request 或 ./api/request

const app = createApp(App)

// ✅ 全局挂载：组件里 this.$axios 就是带拦截器的 service
app.config.globalProperties.$axios = request

app.use(router)
app.use(ElementPlus)
app.mount('#app')