import axios from 'axios'

// 如果用 CORS 方式，后端开在 8081，就写完整后端地址：
const service = axios.create({
    baseURL: 'http://localhost:8081', // 你的后端地址（按你实际端口改）
    timeout: 5000,
})

// 请求拦截器（可选）
service.interceptors.request.use(
    (config) => {
        // 比如以后要加 token，就在这里加
        // config.headers['Authorization'] = 'Bearer xxx'
        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器（这里直接返回 data）
service.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
)

export default service