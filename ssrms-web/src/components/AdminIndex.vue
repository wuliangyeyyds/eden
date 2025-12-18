<template>
  <div class="app-wrapper">
    <AdminAside
        :current-page="currentPage"
        @change-page="changePage"
    />

    <div class="main-area">
      <AdminHeader
          :current-page="currentPage"
          @logout="handleLogout"
      />
      <div class="content-area">
        <AdminHome
            :current-page="currentPage"
            @change-page="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessageBox, ElMessage } from 'element-plus'
import AdminHeader from './AdminHeader.vue'
import AdminAside from './AdminAside.vue'
import AdminHome from './AdminHome.vue'

export default {
  name: 'AdminIndex',
  components: {
    AdminHeader,
    AdminAside,
    AdminHome
  },
  data () {
    return {
      currentPage: 'admin-home'
    }
  },
  methods: {
    changePage (page) {
      this.currentPage = page
    },

    async handleLogout () {
      try {
        await ElMessageBox.confirm(
            '确定要退出登录吗？',
            '提示',
            {
              confirmButtonText: '退出',
              cancelButtonText: '取消',
              type: 'warning'
            }
        )

        // 关键：路由守卫用的是 ssrmsUser，所以必须删它
        localStorage.removeItem('ssrmsUser')

        // 如果你登录时还存过 token，也顺手清掉（不影响没存的情况）
        localStorage.removeItem('ssrmsToken')
        localStorage.removeItem('token')
        sessionStorage.removeItem('ssrmsUser')
        sessionStorage.removeItem('ssrmsToken')
        sessionStorage.removeItem('token')

        // 可选：如果你给 axios 设过默认 Authorization，这里顺手清
        if (this.$axios && this.$axios.defaults && this.$axios.defaults.headers) {
          delete this.$axios.defaults.headers.common.Authorization
        }

        ElMessage.success('已退出登录')
        this.$router.replace('/login')
      } catch (e) {
        // 点了取消就不做任何事
      }
    }
  }
}
</script>

<style scoped>
/* 整体布局与 UserIndex 保持一致 */
.app-wrapper {
  min-height: 100vh;
  display: flex;
  background-color: #f5f7fb; /* 统一背景色 */
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.content-area {
  flex: 1;
  display: flex;
  padding: 8px 24px 20px 24px;
  box-sizing: border-box;
  overflow-x: hidden;
}

@media (max-width: 900px) {
  .app-wrapper {
    flex-direction: column;
  }

  .content-area {
    padding: 8px 12px 16px 12px;
  }
}
</style>