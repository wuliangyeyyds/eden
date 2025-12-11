<template>
  <div class="card user-card">
    <!-- 顶部标题 -->
    <div class="card-header user-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">
          查询学生基本信息、信用状态与违约记录，支持筛选、批量黑名单与预警管理。
        </p>
      </div>
      <div class="user-header-right">
        <div class="auto-refresh">
          <span class="auto-refresh-label">自动刷新</span>
          <el-switch v-model="userAutoRefresh" size="small" />
        </div>
        <el-button type="primary" plain size="small">
          新增管理员账号
        </el-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-input
          v-model="userFilters.keyword"
          placeholder="按姓名 / 学号 / 专业搜索"
          clearable
          class="filter-item filter-input"
      >
        <template #prefix>
          <span class="input-prefix-icon">🔍</span>
        </template>
      </el-input>

      <el-select
          v-model="userFilters.role"
          placeholder="用户角色"
          clearable
          class="filter-item filter-select"
      >
        <el-option
            v-for="role in userRoleOptions"
            :key="role.value"
            :label="role.label"
            :value="role.value"
        />
      </el-select>

      <el-select
          v-model="userFilters.status"
          placeholder="信用状态"
          clearable
          class="filter-item filter-select"
      >
        <el-option
            v-for="item in userStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        />
      </el-select>

      <el-checkbox v-model="userFilters.onlyAbnormal" class="filter-item">
        仅查看预警 / 黑名单
      </el-checkbox>

      <div class="filter-actions">
        <el-button type="primary" size="small" @click="handleUserSearch">
          查询
        </el-button>
        <el-button size="small" @click="resetUserFilters">
          重置
        </el-button>
      </div>
    </div>

    <!-- 统计区域 -->
    <div class="reservation-stats user-stats">
      <div class="stat-card">
        <div class="stat-label">当前用户数</div>
        <div class="stat-value">{{ userStats.total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">正常</div>
        <div class="stat-value">{{ userStats.normal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">预警</div>
        <div class="stat-value">{{ userStats.warning }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">黑名单</div>
        <div class="stat-value">{{ userStats.blacklist }}</div>
      </div>
    </div>

    <!-- 工具栏：批量操作 -->
    <div class="reservation-toolbar user-toolbar">
      <div class="toolbar-left">
        <el-button
            type="warning"
            size="small"
            plain
            :disabled="!userSelection.length"
            @click="handleBatchLock"
        >
          批量加入黑名单
        </el-button>
        <el-button
            type="success"
            size="small"
            plain
            :disabled="!userSelection.length"
            @click="handleBatchUnlock"
        >
          批量解除黑名单
        </el-button>
        <span v-if="userSelection.length" class="toolbar-tip">
          已选中 {{ userSelection.length }} 位用户
        </span>
      </div>
      <div class="toolbar-right">
        <span class="toolbar-tip">
          说明：本页示例数据仅用于前端展示，后续可对接学生信息与统一身份认证。
        </span>
      </div>
    </div>

    <!-- 用户表格 -->
    <el-table
        :data="paginatedUsers"
        border
        stripe
        size="small"
        @selection-change="handleUserSelectionChange"
        class="user-table"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="studentNo" label="学号 / 工号" min-width="120" />
      <el-table-column prop="name" label="姓名" min-width="120">
        <template #default="scope">
          <div class="user-name-cell">
            <div class="user-name-main">
              {{ scope.row.name }}
              <span class="user-role-tag" v-if="scope.row.role === 'student'">学生</span>
              <span class="user-role-tag teacher" v-else-if="scope.row.role === 'teacher'">
                教师
              </span>
              <span class="user-role-tag admin" v-else-if="scope.row.role === 'admin'">
                管理员
              </span>
            </div>
            <div class="user-extra">
              {{ scope.row.major }} · {{ scope.row.clazz }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="creditScore" label="信用分" min-width="90">
        <template #default="scope">
          <span
              :class="[
              'credit-score',
              scope.row.creditScore <= 60 ? 'credit-low' : '',
              scope.row.creditScore >= 95 ? 'credit-high' : ''
            ]"
          >
            {{ scope.row.creditScore }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="violationCount" label="违约次数" min-width="90" />
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="scope">
          <el-tag :type="getUserStatusTagType(scope.row.status)" size="small">
            {{ getUserStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最近登录" min-width="140" />
      <el-table-column label="联系方式" min-width="130">
        <template #default="scope">
          <span class="user-extra">
            {{ scope.row.phone || '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" min-width="230">
        <template #default="scope">
          <div class="action-buttons">
            <el-button
                type="primary"
                link
                size="small"
                @click="handleResetPassword(scope.row)"
            >
              重置密码
            </el-button>
            <el-button
                v-if="scope.row.status !== 'blacklist'"
                type="danger"
                link
                size="small"
                @click="handleLockUser(scope.row)"
            >
              加入黑名单
            </el-button>
            <el-button
                v-else
                type="success"
                link
                size="small"
                @click="handleUnlockUser(scope.row)"
            >
              解除黑名单
            </el-button>

            <el-button
                type="info"
                link
                size="small"
                @click="handleViewUserDetail(scope.row)"
            >
              详情
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="table-footer">
      <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="filteredUsers.length"
          :page-sizes="[5, 10, 20]"
          :page-size="userPageSize"
          :current-page="userPage"
          @size-change="handleUserPageSizeChange"
          @current-change="handleUserPageChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminUserManage',
  data () {
    return {
      userFilters: {
        keyword: '',
        role: '',
        status: '',
        onlyAbnormal: false
      },
      userAutoRefresh: false,
      userRoleOptions: [
        { label: '学生', value: 'student' },
        { label: '教师', value: 'teacher' },
        { label: '管理员', value: 'admin' }
      ],
      userStatusOptions: [
        { label: '正常', value: 'normal' },
        { label: '预警', value: 'warning' },
        { label: '黑名单', value: 'blacklist' }
      ],
      // 示例用户数据，后续可以改成接口返回
      users: [
        {
          id: 1,
          studentNo: '20210001',
          name: '张三',
          role: 'student',
          major: '计算机科学与技术',
          clazz: '3班',
          creditScore: 100,
          violationCount: 0,
          status: 'normal',
          lastLoginAt: '2025-12-05 08:10',
          phone: '138****0001'
        },
        {
          id: 2,
          studentNo: '20210002',
          name: '李四',
          role: 'student',
          major: '软件工程',
          clazz: '2班',
          creditScore: 78,
          violationCount: 1,
          status: 'warning',
          lastLoginAt: '2025-12-05 09:05',
          phone: '138****0002'
        },
        {
          id: 3,
          studentNo: '20210003',
          name: '王五',
          role: 'student',
          major: '人工智能',
          clazz: '1班',
          creditScore: 55,
          violationCount: 3,
          status: 'blacklist',
          lastLoginAt: '2025-12-03 19:20',
          phone: '138****0003'
        },
        {
          id: 4,
          studentNo: 'T00001',
          name: '赵老师',
          role: 'teacher',
          major: '信息学院',
          clazz: '辅导员',
          creditScore: 100,
          violationCount: 0,
          status: 'normal',
          lastLoginAt: '2025-12-05 07:40',
          phone: '139****8888'
        },
        {
          id: 5,
          studentNo: 'A00001',
          name: '系统管理员',
          role: 'admin',
          major: '信息中心',
          clazz: '后台管理',
          creditScore: 100,
          violationCount: 0,
          status: 'normal',
          lastLoginAt: '2025-12-05 08:00',
          phone: '—'
        }
      ],
      userSelection: [],
      userPage: 1,
      userPageSize: 10
    }
  },
  computed: {
    filteredUsers () {
      const { keyword, role, status, onlyAbnormal } = this.userFilters
      return this.users.filter(user => {
        let ok = true
        if (keyword && keyword.trim()) {
          const k = keyword.trim()
          ok =
              ok &&
              (user.name.includes(k) ||
                  user.studentNo.includes(k) ||
                  (user.major && user.major.includes(k)))
        }
        if (role) {
          ok = ok && user.role === role
        }
        if (status) {
          ok = ok && user.status === status
        }
        if (onlyAbnormal) {
          ok =
              ok && (user.status === 'warning' || user.status === 'blacklist')
        }
        return ok
      })
    },
    paginatedUsers () {
      const start = (this.userPage - 1) * this.userPageSize
      return this.filteredUsers.slice(start, start + this.userPageSize)
    },
    userStats () {
      const stats = {
        total: 0,
        normal: 0,
        warning: 0,
        blacklist: 0
      }
      stats.total = this.filteredUsers.length
      this.filteredUsers.forEach(user => {
        if (user.status === 'normal') stats.normal += 1
        else if (user.status === 'warning') stats.warning += 1
        else if (user.status === 'blacklist') stats.blacklist += 1
      })
      return stats
    }
  },
  methods: {
    handleUserSearch () {
      console.log('查询用户', this.userFilters)
      this.userPage = 1
    },
    resetUserFilters () {
      this.userFilters = {
        keyword: '',
        role: '',
        status: '',
        onlyAbnormal: false
      }
      this.userPage = 1
    },
    handleUserSelectionChange (val) {
      this.userSelection = val || []
    },
    handleUserPageChange (page) {
      this.userPage = page
    },
    handleUserPageSizeChange (size) {
      this.userPageSize = size
      this.userPage = 1
    },
    getUserStatusTagType (status) {
      if (status === 'normal') return 'success'
      if (status === 'warning') return 'warning'
      if (status === 'blacklist') return 'danger'
      return ''
    },
    getUserStatusText (status) {
      if (status === 'normal') return '正常'
      if (status === 'warning') return '预警'
      if (status === 'blacklist') return '黑名单'
      return status
    },
    handleLockUser (row) {
      console.log('将用户加入黑名单', row)
      row.status = 'blacklist'
      if (row.creditScore > 60) {
        row.creditScore = 60
      }
    },
    handleUnlockUser (row) {
      console.log('解除黑名单', row)
      row.status = 'normal'
      if (row.creditScore < 80) {
        row.creditScore = 80
      }
    },
    handleResetPassword (row) {
      console.log('重置密码', row)
      // 这里可以弹出对话框，调用后端接口
    },
    handleViewUserDetail (row) {
      console.log('查看用户详情', row)
      // 这里可以改成弹窗 / 抽屉展示详情
    },
    handleBatchLock () {
      console.log('批量加入黑名单', this.userSelection)
      this.userSelection.forEach(user => {
        user.status = 'blacklist'
        if (user.creditScore > 60) user.creditScore = 60
      })
      this.userSelection = []
    },
    handleBatchUnlock () {
      console.log('批量解除黑名单', this.userSelection)
      this.userSelection.forEach(user => {
        user.status = 'normal'
        if (user.creditScore < 80) user.creditScore = 80
      })
      this.userSelection = []
    }
  }
}
</script>

<style scoped>
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}

.user-card {
  margin-top: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.user-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

/* 筛选条 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-item {
  min-width: 180px;
}

.filter-input {
  flex: 2;
}

.filter-select {
  flex: 1;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-prefix-icon {
  font-size: 14px;
}

/* 统计区（复用预约的样式） */
.reservation-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 8px 0 12px;
}

.stat-card {
  flex: 1;
  min-width: 140px;
  border-radius: 12px;
  background-color: #f9fafb;
  padding: 10px 12px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

.stat-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

/* 工具栏 */
.reservation-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  font-size: 12px;
  color: #6b7280;
}

.toolbar-tip {
  font-size: 12px;
  color: #4b5563;
}

/* 表格样式 */
.user-table {
  width: 100%;
}

.user-name-cell {
  display: flex;
  flex-direction: column;
}

.user-name-main {
  font-size: 13px;
  color: #111827;
}

.user-role-tag {
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  background-color: #eff6ff;
  color: #1d4ed8;
}

.user-role-tag.teacher {
  background-color: #ecfdf3;
  color: #15803d;
}

.user-role-tag.admin {
  background-color: #fef3c7;
  color: #b45309;
}

.user-extra {
  font-size: 12px;
  color: #6b7280;
}

.credit-score {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.credit-low {
  color: #b91c1c;
}

.credit-high {
  color: #15803d;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 响应式 */
@media (max-width: 900px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .user-header-right {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-item,
  .filter-input,
  .filter-select {
    width: 100%;
  }

  .reservation-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-footer {
    justify-content: center;
  }
}
</style>
