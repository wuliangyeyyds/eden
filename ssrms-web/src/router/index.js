// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '@/components/LoginPage.vue'
import UserIndex from '@/components/UserIndex.vue'      // 学生端
import AdminIndex from '@/components/AdminIndex.vue'    // 管理员端

const routes = [
    { path: '/', redirect: '/login' },

    { path: '/login', name: 'Login', component: LoginPage },

    // 学生端，需要登录，角色=student
    {
        path: '/user',
        name: 'UserHome',
        component: UserIndex,
        meta: {
            requiresAuth: true,
            role: 'user'   // 用字符串描述一下角色，方便判断
        }
    },

    // 管理员端，需要登录，角色=admin
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

// 🌟 路由守卫：这里是真正拦住“直接敲地址”的地方
router.beforeEach((to, from, next) => {
    const raw = localStorage.getItem('ssrmsUser')
    const user = raw ? JSON.parse(raw) : null

    // 1）需要登录的页面
    if (to.meta.requiresAuth) {
        if (!user) {
            // 没登录，跳回登录页，还可以带一个 redirect 回来用
            return next({
                path: '/login',
                query: { redirect: to.fullPath }
            })
        }

        // 2）按角色检查：roleId 0=管理员,1=学生
        if (to.meta.role === 'admin' && user.roleId !== 0) {
            return next('/login')
        }
        if (to.meta.role === 'user' && user.roleId !== 1) {
            return next('/login')
        }
    }

    // 3）已经登录了又去 /login，就直接丢回对应首页
    if (to.path === '/login' && user) {
        return next(user.roleId === 0 ? '/admin' : '/user')
    }

    // 其它情况正常放行
    next()
})

export default router