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
import AdminAside from './AdminAside.vue'
import AdminHeader from './AdminHeader.vue'
import AdminHome from './AdminHome.vue'

export default {
  name: 'AdminIndex',
  components: {
    AdminAside,
    AdminHeader,
    AdminHome
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
    // ⭐ 管理员退出登录
    handleLogout () {
      // 1. 清空本地保存的登录信息（按你自己的 key 来）
      // 学生端我们用的是 ssrmsUser，这里直接一起清掉
      localStorage.removeItem('ssrmsUser')
      // 如果你单独给管理员存了一个 key，比如 ssrmsAdmin，可以顺带删掉：
      localStorage.removeItem('ssrmsAdmin')
      // 如果还有 token 之类的，也可以一起删：
      localStorage.removeItem('ssrmsToken')

      // 也可以图省事直接：
      // localStorage.clear()

      // 2. 跳回登录页
      this.$router.replace('/login')
      // this.$router.push('/login') 也可以，只是 replace 不会留下历史记录
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