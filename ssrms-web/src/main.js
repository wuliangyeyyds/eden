// src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './assets/global.css'
import axios from 'axios'
import router from './router'

axios.defaults.baseURL = 'http://localhost:8090'

const app = createApp(App)

// 全局挂载 axios，在组件里用 this.$axios 调
app.config.globalProperties.$axios = axios

app.use(router)
app.use(ElementPlus)

app.mount('#app')