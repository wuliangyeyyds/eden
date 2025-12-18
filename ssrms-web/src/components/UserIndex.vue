<template>
  <div class="app-wrapper">
    <!-- 左侧导航 -->
    <AppAside
        :current-page="currentPage"
        @change-page="changePage"
    />

    <!-- 右侧区域：标题栏 + 主内容 -->
    <div class="main-area">
      <AppHeader
          :current-page="currentPage"
          @logout="handleLogout"
      />
      <div class="content-area">
        <AppHome
            :current-page="currentPage"
            @change-page="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessageBox, ElMessage } from 'element-plus'
import UserHeader from './UserHeader.vue'
import UserAside from './UserAside.vue'
import UserHome from './UserHome.vue'

export default {
  name: 'MainIndex',
  components: {
    AppHeader: UserHeader,
    AppAside: UserAside,
    AppHome: UserHome
  },
  data () {
    return {
      currentPage: 'home'
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

        // 1) 清掉登录信息（路由守卫用 ssrmsUser 的话，这个必须删）
        localStorage.removeItem('ssrmsUser')
        // 可选：如果你存过 token，也一起删
        localStorage.removeItem('ssrmsToken')
        localStorage.removeItem('token')
        sessionStorage.removeItem('ssrmsUser')
        sessionStorage.removeItem('ssrmsToken')
        sessionStorage.removeItem('token')

        // 2) 跳回登录页（replace 防止后退回到 /user）
        ElMessage.success('已退出登录')
        this.$router.replace('/login')
      } catch (e) {
        // 用户点了“取消”就啥也不做
      }
    }
  }
}
</script>

<style scoped>
/* 整体：左侧导航 + 右侧主区域 */
.app-wrapper {
  min-height: 100vh;
  display: flex;
  background-color: #f5f7fb;
}

/* 右侧主区域：上标题栏 + 下内容 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 右侧主内容区域，留一点内边距，让卡片更贴近底部 */
.content-area {
  flex: 1;
  display: flex;
  padding: 8px 24px 20px 24px;
  box-sizing: border-box;
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