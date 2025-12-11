<template>
  <aside class="side">
    <div class="sidebar-header">
      <div class="logo-icon admin-logo">
        <span>🛡</span>
      </div>
      <div class="logo-text">
        <div class="system-title">自习室预约系统</div>
        <div class="system-subtitle">管理员后台</div>
      </div>
    </div>

    <nav class="menu">
      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-home' }"
          @click="emitChange('admin-home')"
      >
        <span class="menu-icon">📊</span>
        <span>后台首页</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-reservations' }"
          @click="emitChange('admin-reservations')"
      >
        <span class="menu-icon">📅</span>
        <span>预约管理</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-users' }"
          @click="emitChange('admin-users')"
      >
        <span class="menu-icon">👥</span>
        <span>用户管理</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-seats' }"
          @click="emitChange('admin-seats')"
      >
        <span class="menu-icon">🪑</span>
        <span>座位管理</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-complaints' }"
          @click="emitChange('admin-complaints')"
      >
        <span class="menu-icon">📢</span>
        <span>投诉处理</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'admin-logs' }"
          @click="emitChange('admin-logs')"
      >
        <span class="menu-icon">📑</span>
        <span>日志统计</span>
      </button>
    </nav>
  </aside>
</template>

<script>
export default {
  name: 'AdminAside',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    }
  }
}
</script>

<style scoped>
/* 复用 UserAside 的样式 */
.side {
  width: 260px;
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  box-sizing: border-box;
  z-index: 10;
}

.sidebar-header {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 20px;
}
/* 管理员图标特定颜色 */
.admin-logo {
  background-color: #eff6ff; /* 蓝色系背景 */
  color: #1d4ed8;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.system-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.system-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  border: none;
  background: transparent;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.1s ease;
  text-align: left;
}

.menu-item .menu-icon {
  font-size: 16px;
}

/* 激活状态 */
.menu-item.active {
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.menu-item:hover {
  background-color: #f3f4f6;
  transform: translateX(1px);
}

@media (max-width: 900px) {
  .side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    padding: 16px;
  }

  .sidebar-header {
    margin-bottom: 0;
    margin-right: 24px;
  }

  .menu {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .menu-item {
    flex: 1 0 auto;
  }
}
</style>