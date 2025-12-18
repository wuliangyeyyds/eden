import axios from 'axios'

// Vue CLI：用 process.env，生产默认走 /api（配 Nginx 反代）
// 开发想直连后端：VUE_APP_API_BASE_URL=http://localhost:8090
const baseURL =
    process.env.VUE_APP_API_BASE_URL ||
    (process.env.NODE_ENV === 'development' ? 'http://localhost:8090' : '/api')

const service = axios.create({
    baseURL,
    timeout: 10000
})

service.interceptors.request.use(
    (config) => config,
    (error) => Promise.reject(error)
)

service.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
)

export default service