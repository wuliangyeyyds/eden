<template>
  <header class="topbar">
    <div class="topbar-left">
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>
    <div class="topbar-right">
      <button class="logout-btn" type="button" @click="onLogout">
        退出登录
      </button>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AdminHeader',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['logout'],
  computed: {
    pageTitle () {
      const map = {
        'admin-home': '后台首页',
        'admin-reservations': '预约管理',
        'admin-users': '用户管理',
        'admin-seats': '座位管理',
        'admin-complaints': '投诉处理',
        'admin-logs': '日志统计'
      }
      return map[this.currentPage] || '管理员后台'
    }
  },
  methods: {
    onLogout () {
      this.$emit('logout')
    }
  }
}
</script>

<style scoped>
.topbar {
  height: 56px;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  margin: 16px 24px 8px 24px;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.topbar-right {
  display: flex;
  align-items: center;
}

.logout-btn {
  border: none;
  background: transparent;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 999px;
}

.logout-btn:hover {
  background-color: #f3f4f6;
  color: #111827;
}

@media (max-width: 600px) {
  .topbar {
    margin: 10px 12px 4px 12px;
    padding: 0 16px;
  }
  .page-title {
    font-size: 16px;
  }
}
</style>