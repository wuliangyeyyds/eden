<template>
  <aside class="side">
    <div class="sidebar-header">
      <div class="logo-icon">
        <span>📚</span>
      </div>
      <div class="logo-text">
        <div class="system-title">自习室预约系统</div>
        <div class="system-subtitle">学生端</div>
      </div>
    </div>

    <nav class="menu">
      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'home' }"
          @click="emitChange('home')"
      >
        <span class="menu-icon">🏠</span>
        <span>首页</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'user-reserve' }"
          @click="emitChange('user-reserve')"
      >
        <span class="menu-icon">🗓</span>
        <span>我要预约</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'user-reservations' }"
          @click="emitChange('user-reservations')"
      >
        <span class="menu-icon">📋</span>
        <span>我的预约</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'user-violations' }"
          @click="emitChange('user-violations')"
      >
        <span class="menu-icon">⚠</span>
        <span>违规记录</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'user-feedback' }"
          @click="emitChange('user-feedback')"
      >
        <span class="menu-icon">💬</span>
        <span>评价与投诉</span>
      </button>

      <button
          type="button"
          class="menu-item"
          :class="{ active: currentPage === 'user-profile' }"
          @click="emitChange('user-profile')"
      >
        <span class="menu-icon">👤</span>
        <span>个人中心</span>
      </button>
    </nav>
  </aside>
</template>

<script>
export default {
  name: 'AppAside',
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
.side {
  width: 260px;
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
  box-sizing: border-box;
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
  background-color: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 20px;
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
  align-items: center;      /* 垂直居中图标和文字 */
  gap: 10px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, transform 0.1s ease;
  text-align: left;
  height: 44px;            /* 固定整行高度，可选 */
  line-height: 1;          /* 避免行高把图标顶歪 */
}

/* 关键：给图标一个固定宽度的小格子 */
.menu-item .menu-icon {
  width: 20px;             /* 所有图标占同样宽度 */
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;          /* 不要被压缩 */
}

.menu-item.active {
  background-color: #eef2ff;
  color: #4f46e5;
  font-weight: 600;
}

.menu-item:hover {
  background-color: #f3f4ff;
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