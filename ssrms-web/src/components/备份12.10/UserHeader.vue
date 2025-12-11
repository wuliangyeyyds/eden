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
  name: 'AppHeader',
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
        home: '首页',
        'user-reserve': '我要预约',
        'user-reservations': '我的预约',
        'user-violations': '违规记录',
        'user-feedback': '评价与投诉',
        'user-profile': '个人中心'
      }
      return map[this.currentPage] || '自习室预约管理系统'
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
  margin: 16px 24px 8px 24px;        /* 左右都留 24px，与内容区对齐 */
  border-radius: 14px;               /* 做成一块“卡片” */
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

/* 标题 */
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

/* 右侧只剩一个“退出登录”按钮 */
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