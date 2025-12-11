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
      // 默认首页
      currentPage: 'home'
    }
  },
  methods: {
    changePage (page) {
      this.currentPage = page
    },
    handleLogout () {
      // 1. 清掉当前登录用户的信息
      localStorage.removeItem('ssrmsUser')
      // 如果之后你有 token 之类，也可以一起删：
      // localStorage.removeItem('ssrmsToken')

      // 2. 回到登录页，用 replace 避免浏览器“后退”再回到 /user
      this.$router.replace('/login')
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
  padding: 8px 24px 20px 24px; /* 底部 padding 比较小一点 */
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