// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import LoginRegister from '@/components/LoginRegister.vue' // ✅ 统一登录/注册页面
import UserIndex from '@/components/UserIndex.vue'         // 学生端
import AdminIndex from '@/components/AdminIndex.vue'       // 管理员端

const routes = [
    { path: '/', redirect: '/login' },

    // ✅ 登录页直接使用 LoginRegister
    { path: '/login', name: 'Login', component: LoginRegister },

    // 学生端，需要登录，role=user
    {
        path: '/user',
        name: 'UserHome',
        component: UserIndex,
        meta: {
            requiresAuth: true,
            role: 'user'
        }
    },

    // 管理员端，需要登录，role=admin
    {
        path: '/admin',
        name: 'AdminHome',
        component: AdminIndex,
        meta: {
            requiresAuth: true,
            role: 'admin'
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 读取登录用户（更稳：JSON 解析失败就当未登录）
function getLoginUser () {
    const raw = localStorage.getItem('ssrmsUser')
    if (!raw) return null
    try {
        return JSON.parse(raw)
    } catch (e) {
        localStorage.removeItem('ssrmsUser')
        return null
    }
}

router.beforeEach((to, from, next) => {
    const user = getLoginUser()

    // 1) 需要登录的页面
    if (to.meta && to.meta.requiresAuth) {
        if (!user) {
            return next({
                path: '/login',
                query: { redirect: to.fullPath }
            })
        }

        // 2) 角色检查：roleId 0=管理员, 1=学生
        //    如果角色不匹配，直接送回“他该去的首页”（体验更好）
        if (to.meta.role === 'admin' && user.roleId !== 0) {
            return next('/user')
        }
        if (to.meta.role === 'user' && user.roleId !== 1) {
            return next('/admin')
        }
    }

    // 3) 已登录又去 /login，就直接回对应首页
    if (to.path === '/login' && user) {
        return next(user.roleId === 0 ? '/admin' : '/user')
    }

    next()
})

export default router