<template>
  <div class="dashboard-page">
    <!-- 顶部欢迎 + 今日概览 -->
    <div class="card welcome-card">
      <div class="welcome-left">
        <div class="welcome-title-row">
          <h2 class="welcome-title">自习室预约系统 · 管理后台</h2>
          <el-tag size="small" type="success">运行正常</el-tag>
        </div>
        <p class="welcome-subtitle">
          欢迎回来，管理员。同一时间可以在这里总览预约情况、签到率和异常提醒，快速进入各个管理模块。
        </p>

        <div class="welcome-meta">
          <span class="meta-item">
            今天：{{ todayText }}
          </span>
          <span class="meta-separator">·</span>
          <span class="meta-item">
            最近登录 IP：192.168.1.23（示例）
          </span>
        </div>
      </div>

      <div class="welcome-right">
        <div class="metric-grid">
          <div class="metric-item">
            <div class="metric-label">今日预约总数</div>
            <div class="metric-value">{{ metrics.todayReservations }}</div>
            <div class="metric-desc">较昨日 {{ metrics.todayTrend }}%</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">今日签到率</div>
            <div class="metric-value">
              {{ metrics.todaySignRate }}<span class="metric-unit">%</span>
            </div>
            <el-progress
                :percentage="metrics.todaySignRate"
                :stroke-width="6"
                status="success"
                class="metric-progress"
            />
          </div>
          <div class="metric-item">
            <div class="metric-label">当前开放自习室</div>
            <div class="metric-value">{{ metrics.openRooms }}</div>
            <div class="metric-desc">覆盖 {{ metrics.totalSeats }} 个座位</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">待处理事项</div>
            <div class="metric-value warning">{{ metrics.pendingItems }}</div>
            <div class="metric-desc">投诉 / 异常 / 审核 等</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间两列：趋势预览 + 滚动公告 -->
    <div class="middle-row">
      <!-- 趋势预览 -->
      <div class="card trend-card">
        <div class="card-header-row">
          <div>
            <h3 class="card-title">本周运行概览</h3>
            <p class="card-subtitle">通过简单的进度条快速感知本周整体情况。</p>
          </div>
          <div class="trend-extra">
            <span class="trend-label">自动刷新</span>
            <el-switch v-model="autoRefresh" size="small" />
          </div>
        </div>

        <div class="trend-list">
          <div class="trend-item" v-for="item in trendItems" :key="item.key">
            <div class="trend-item-header">
              <span class="trend-item-title">{{ item.title }}</span>
              <span class="trend-item-value">{{ item.value }}</span>
            </div>
            <el-progress
                :percentage="item.percent"
                :stroke-width="8"
                :status="item.status || 'success'"
                class="trend-progress"
            />
            <div class="trend-item-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>

      <!-- 系统公告 & 滚动提醒 -->
      <div class="card notice-card">
        <div class="card-header-row">
          <div>
            <h3 class="card-title">系统公告与提醒</h3>
            <p class="card-subtitle">
              支持滚动展示最近的系统公告、维护通知和重要提醒。
            </p>
          </div>
          <el-button size="small" type="primary" plain>发布新公告</el-button>
        </div>

        <!-- 滚动公告（垂直轮播） -->
        <el-carousel
            height="96px"
            direction="vertical"
            :autoplay="true"
            indicator-position="none"
            :interval="3800"
            class="notice-carousel"
        >
          <el-carousel-item
              v-for="notice in notices"
              :key="notice.id"
              class="notice-item"
          >
            <div class="notice-tag-wrapper">
              <el-tag
                  size="small"
                  :type="notice.level === 'warning' ? 'warning' : (notice.level === 'danger' ? 'danger' : 'info')"
              >
                {{ notice.tag }}
              </el-tag>
              <span class="notice-time">{{ notice.time }}</span>
            </div>
            <div class="notice-title">{{ notice.title }}</div>
            <div class="notice-desc">{{ notice.desc }}</div>
          </el-carousel-item>
        </el-carousel>

        <!-- 快捷提示小条 -->
        <div class="notice-footer">
          <span class="notice-dot" />
          <span class="notice-footer-text">
            温馨提示：可在左侧导航或下方待办事项中快速进入对应处理页面。
          </span>
        </div>
      </div>
    </div>

    <!-- 底部：待办事项 / 快速入口 -->
    <div class="card todo-card">
      <div class="card-header-row">
        <div>
          <h3 class="card-title">待办事项</h3>
          <p class="card-subtitle">
            这里汇总了你近期需要处理的关键事项，比如投诉、异常记录和规则配置。
          </p>
        </div>
        <el-button size="small" @click="handleMarkAllDone">
          一键标记已读（示例）
        </el-button>
      </div>

      <el-table
          :data="todos"
          size="small"
          class="todo-table"
          border
          stripe
      >
        <el-table-column prop="type" label="类型" min-width="110">
          <template #default="scope">
            <el-tag
                size="small"
                :type="getTodoTagType(scope.row.type)"
            >
              {{ getTodoTypeText(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="内容" min-width="260" />

        <el-table-column prop="from" label="来源" min-width="120" />

        <el-table-column prop="time" label="创建时间" min-width="150" />

        <el-table-column prop="priority" label="优先级" min-width="80">
          <template #default="scope">
            <el-tag
                size="small"
                :type="scope.row.priority === 'high' ? 'danger' : (scope.row.priority === 'medium' ? 'warning' : 'info')"
                effect="plain"
            >
              {{ getPriorityText(scope.row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="scope">
            <div class="todo-actions">
              <el-button
                  size="small"
                  type="primary"
                  link
                  @click="handleTodoGo(scope.row)"
              >
                前往处理
              </el-button>
              <el-button
                  size="small"
                  type="success"
                  link
                  @click="handleTodoDone(scope.row)"
              >
                标记完成
              </el-button>
              <el-tooltip
                  content="只是示例，不会真正修改后端数据"
                  placement="top"
              >
                <el-button
                    size="small"
                    type="info"
                    link
                >
                  详情
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminDashboard',
  data () {
    return {
      autoRefresh: false,
      metrics: {
        todayReservations: 120,
        todayTrend: 12,
        todaySignRate: 82,
        openRooms: 12,
        totalSeats: 680,
        pendingItems: 5
      },
      trendItems: [
        {
          key: 'weekReservation',
          title: '本周预约完成率',
          value: '86%',
          percent: 86,
          status: 'success',
          desc: '预约完成率保持在较高水平，可继续观察周末波动。'
        },
        {
          key: 'weekCheckin',
          title: '本周平均签到率',
          value: '79%',
          percent: 79,
          status: 'success',
          desc: '建议针对低年级同学开展一次到馆提醒活动。'
        },
        {
          key: 'weekViolation',
          title: '本周违约率',
          value: '6%',
          percent: 6,
          status: 'warning',
          desc: '有 8 位同学出现多次违约，可结合信用分规则重点关注。'
        }
      ],
      notices: [
        {
          id: 1,
          tag: '系统维护',
          level: 'info',
          time: '今天 22:30-23:00',
          title: '今晚将进行短暂系统升级维护',
          desc: '维护期间将暂停新预约与取消操作，已生效预约不受影响。'
        },
        {
          id: 2,
          tag: '使用建议',
          level: 'warning',
          time: '本周内有效',
          title: '建议管理员检查假期预约规则配置',
          desc: '如遇校历调整，请及时修改假期开放时间与预约限制。'
        },
        {
          id: 3,
          tag: '异常提醒',
          level: 'danger',
          time: '刚刚',
          title: '图书馆 401 有 3 个座位多次违约',
          desc: '可考虑对对应学生进行提醒或暂时禁用相关座位。'
        }
      ],
      todos: [
        {
          id: 1,
          type: 'complaint',
          content: '处理 2 条关于自习室噪音的投诉记录。',
          from: '投诉处理',
          time: '2025-12-10 09:20',
          priority: 'high'
        },
        {
          id: 2,
          type: 'violation',
          content: '检查本周连续违约超过 3 次的学生名单。',
          from: '日志统计',
          time: '2025-12-10 08:50',
          priority: 'medium'
        },
        {
          id: 3,
          type: 'rule',
          content: '确认期末考试周自习室开放时间与预约上限。',
          from: '座位管理',
          time: '2025-12-09 16:10',
          priority: 'medium'
        },
        {
          id: 4,
          type: 'system',
          content: '导出本月预约与签到报表存档。',
          from: '日志统计',
          time: '2025-12-09 10:25',
          priority: 'low'
        }
      ]
    }
  },
  computed: {
    todayText () {
      const d = new Date()
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
  },
  methods: {
    getTodoTagType (type) {
      if (type === 'complaint') return 'danger'
      if (type === 'violation') return 'warning'
      if (type === 'rule') return 'success'
      return 'info'
    },
    getTodoTypeText (type) {
      if (type === 'complaint') return '投诉'
      if (type === 'violation') return '违约'
      if (type === 'rule') return '规则配置'
      if (type === 'system') return '系统'
      return '其他'
    },
    getPriorityText (p) {
      if (p === 'high') return '高'
      if (p === 'medium') return '中'
      return '低'
    },
    handleTodoGo (row) {
      console.log('前往对应模块处理：', row)
      // 这里可以通过 $emit 通知父组件切换到对应菜单
    },
    handleTodoDone (row) {
      console.log('标记完成（示例，不落库）：', row)
      this.todos = this.todos.filter(t => t.id !== row.id)
    },
    handleMarkAllDone () {
      console.log('一键标记已读（示例）')
      this.todos = []
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 通用卡片 */
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}

/* 顶部欢迎卡片 */
.welcome-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
}

.welcome-left {
  flex: 1.1;
}

.welcome-right {
  flex: 1.1;
}

.welcome-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.welcome-subtitle {
  margin: 6px 0 8px;
  font-size: 13px;
  opacity: 0.95;
}

.welcome-meta {
  font-size: 12px;
  opacity: 0.95;
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-item {
  white-space: nowrap;
}

.meta-separator {
  opacity: 0.7;
}

/* 指标小卡片 */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  background: rgba(15, 23, 42, 0.16);
  border-radius: 12px;
  padding: 10px 12px;
}

.metric-label {
  font-size: 12px;
  opacity: 0.95;
}

.metric-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
}

.metric-value.warning {
  color: #fee2e2;
}

.metric-unit {
  font-size: 12px;
  margin-left: 2px;
}

.metric-desc {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.9;
}

.metric-progress {
  margin-top: 4px;
}

/* 中间两列 */
.middle-row {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.trend-card {
  flex: 1.2;
}

.notice-card {
  flex: 1;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.card-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.trend-extra {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

/* 趋势列表 */
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-item {
  padding: 6px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.trend-item:last-child {
  border-bottom: none;
}

.trend-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-item-title {
  font-size: 13px;
  color: #111827;
}

.trend-item-value {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.trend-progress {
  margin: 4px 0;
}

.trend-item-desc {
  font-size: 12px;
  color: #6b7280;
}

/* 公告轮播 */
.notice-carousel {
  margin-top: 4px;
}

.notice-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.notice-tag-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-time {
  font-size: 12px;
  color: #9ca3af;
}

.notice-title {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.notice-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #4b5563;
}

.notice-footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.notice-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background-color: #22c55e;
}

/* 待办事项 */
.todo-card {
  margin-bottom: 4px;
}

.todo-table {
  margin-top: 4px;
}

.todo-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .welcome-card {
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .middle-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .card-header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
