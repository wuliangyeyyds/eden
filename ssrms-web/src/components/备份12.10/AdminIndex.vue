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
    handleLogout () {
      this.$router.push('/login')
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