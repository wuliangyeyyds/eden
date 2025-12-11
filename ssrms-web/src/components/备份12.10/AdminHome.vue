<template>
  <div class="main">
    <!-- 后台首页 -->
    <AdminDashboard v-if="currentPage === 'admin-home'" />

    <!-- 预约管理页面 -->
    <div v-else-if="currentPage === 'admin-reservations'">
      <div class="card reservation-card">
        <!-- 顶部标题 + 自动刷新开关 -->
        <div class="card-header reservation-header">
          <div>
            <h2 class="page-title">预约管理</h2>
            <p class="page-subtitle">
              查看和管理学生预约记录，支持筛选、批量操作、补录签到与标记违约。
            </p>
          </div>
          <div class="reservation-header-right">
            <div class="auto-refresh">
              <span class="auto-refresh-label">自动刷新</span>
              <el-switch v-model="reservationAutoRefresh" size="small" />
            </div>
            <el-button type="primary" plain size="small" @click="handleReservationSearch">
              刷新数据
            </el-button>
          </div>
        </div>

        <!-- 筛选区域 -->
        <div class="filter-bar">
          <el-input
              v-model="reservationFilters.keyword"
              placeholder="按姓名 / 学号 / 预约编号搜索"
              clearable
              class="filter-item filter-input"
          >
            <template #prefix>
              <span class="input-prefix-icon">🔍</span>
            </template>
          </el-input>

          <el-select
              v-model="reservationFilters.room"
              placeholder="选择自习室"
              clearable
              class="filter-item filter-select"
          >
            <el-option
                v-for="room in roomOptions"
                :key="room"
                :label="room"
                :value="room"
            />
          </el-select>

          <el-date-picker
              v-model="reservationFilters.date"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              clearable
              class="filter-item filter-date"
          />

          <el-select
              v-model="reservationFilters.status"
              placeholder="预约状态"
              clearable
              class="filter-item filter-select"
          >
            <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>

          <div class="filter-actions">
            <el-button type="primary" size="small" @click="handleReservationSearch">
              查询
            </el-button>
            <el-button size="small" @click="resetReservationFilters">
              重置
            </el-button>
          </div>
        </div>

        <!-- 统计区域 -->
        <div class="reservation-stats">
          <div class="stat-card">
            <div class="stat-label">当前记录</div>
            <div class="stat-value">{{ reservationStats.total }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">已预约</div>
            <div class="stat-value">{{ reservationStats.booked }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">已签到</div>
            <div class="stat-value">{{ reservationStats.signed }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">已取消 / 违约</div>
            <div class="stat-value">
              {{ reservationStats.canceled }} / {{ reservationStats.violation }}
            </div>
          </div>
        </div>

        <!-- 工具栏：批量操作 -->
        <div class="reservation-toolbar">
          <div class="toolbar-left">
            <el-button
                type="success"
                size="small"
                :disabled="!reservationSelection.length"
                @click="handleBatchSign"
            >
              批量补录签到
            </el-button>
            <el-button
                type="danger"
                size="small"
                plain
                :disabled="!reservationSelection.length"
                @click="handleBatchCancel"
            >
              批量取消预约
            </el-button>
            <span v-if="reservationSelection.length" class="toolbar-tip">
              已选中 {{ reservationSelection.length }} 条记录
            </span>
          </div>
          <div class="toolbar-right">
            <span class="toolbar-tip">当前为示例数据，后续可替换为后端接口返回的数据。</span>
          </div>
        </div>

        <!-- 数据表格 -->
        <el-table
            :data="paginatedReservations"
            border
            stripe
            size="small"
            @selection-change="handleSelectionChange"
            class="reservation-table"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="code" label="预约编号" min-width="120" />
          <el-table-column prop="studentName" label="学生" min-width="110">
            <template #default="scope">
              <div class="student-cell">
                <div class="student-name">{{ scope.row.studentName }}</div>
                <div class="student-no">{{ scope.row.studentNo }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="room" label="自习室" min-width="130" />
          <el-table-column prop="seatNo" label="座位" min-width="80" />
          <el-table-column prop="date" label="日期" min-width="110" />
          <el-table-column prop="timeRange" label="时间段" min-width="120" />
          <el-table-column prop="status" label="状态" min-width="90">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)" size="small">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160">
            <template #default="scope">
              <span class="remark-text">
                {{ scope.row.remark || '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="200">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                    v-if="scope.row.status === 'booked'"
                    type="primary"
                    link
                    size="small"
                    @click="handleSign(scope.row)"
                >
                  补录签到
                </el-button>
                <el-button
                    v-if="scope.row.status === 'booked'"
                    type="danger"
                    link
                    size="small"
                    @click="handleCancel(scope.row)"
                >
                  取消预约
                </el-button>

                <el-dropdown v-if="scope.row.status !== 'canceled'">
                  <span class="el-dropdown-link">更多</span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="handleMarkViolation(scope.row)">
                        标记违约
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleViewDetail(scope.row)">
                        查看详情
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <span v-else class="disabled-text">不可操作</span>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="table-footer">
          <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="filteredReservations.length"
              :page-sizes="[5, 10, 20]"
              :page-size="reservationPageSize"
              :current-page="reservationPage"
              @size-change="handlePageSizeChange"
              @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 用户管理 -->
    <AdminUserManage v-else-if="currentPage === 'admin-users'" />

    <!-- 座位管理 -->
    <AdminSeatManage v-else-if="currentPage === 'admin-seats'" />

    <!-- 投诉处理 -->
    <div v-else-if="currentPage === 'admin-complaints'" class="card simple-card">
      <h2 class="page-title">投诉处理</h2>
      <p class="page-subtitle">处理学生反馈与投诉记录，跟踪处理结果与反馈满意度。</p>
      <p class="simple-tip">
        后续可在此页面增加投诉列表、处理进度、标记已读、导出等功能。
      </p>
    </div>

    <!-- 日志统计 -->
    <div v-else-if="currentPage === 'admin-logs'" class="card simple-card">
      <h2 class="page-title">日志统计</h2>
      <p class="page-subtitle">查看系统操作日志与统计信息，辅助排查问题与追踪操作记录。</p>
      <p class="simple-tip">
        可以在此接入折线图/柱状图展示日预约量、签到率、违约率等统计数据。
      </p>
    </div>
  </div>
</template>

<script>
import AdminDashboard from './AdminDashboard.vue'
import AdminUserManage from './AdminUserManage.vue'
import AdminSeatManage from './AdminSeatManage.vue'

export default {
  name: 'AdminHome',
  components: {
    AdminDashboard,
    AdminUserManage,
    AdminSeatManage
  },
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  data () {
    return {
      /* ========= 预约管理相关 ========= */
      reservationFilters: {
        keyword: '',
        room: '',
        date: '',
        status: ''
      },
      reservationAutoRefresh: false,
      roomOptions: ['3 楼 301', '图书馆 401', '图书馆 501'],
      statusOptions: [
        { label: '已预约', value: 'booked' },
        { label: '已签到', value: 'signed' },
        { label: '已取消', value: 'canceled' },
        { label: '违约', value: 'violation' }
      ],
      reservations: [
        {
          id: 1,
          code: 'Y20251204001',
          studentNo: '20210001',
          studentName: '张三',
          room: '3 楼 301',
          seatNo: 'A-15',
          date: '2025-12-05',
          timeRange: '08:00-10:00',
          status: 'booked',
          remark: '迟到需人工确认'
        },
        {
          id: 2,
          code: 'Y20251204002',
          studentNo: '20210002',
          studentName: '李四',
          room: '图书馆 401',
          seatNo: 'B-08',
          date: '2025-12-05',
          timeRange: '10:00-12:00',
          status: 'signed',
          remark: '按时签到'
        },
        {
          id: 3,
          code: 'Y20251204003',
          studentNo: '20210003',
          studentName: '王五',
          room: '图书馆 401',
          seatNo: 'C-07',
          date: '2025-12-05',
          timeRange: '14:00-16:00',
          status: 'canceled',
          remark: '学生主动取消'
        },
        {
          id: 4,
          code: 'Y20251204004',
          studentNo: '20210004',
          studentName: '赵六',
          room: '3 楼 301',
          seatNo: 'A-01',
          date: '2025-12-04',
          timeRange: '18:00-20:00',
          status: 'violation',
          remark: '未到场且未取消'
        }
      ],
      reservationSelection: [],
      reservationPage: 1,
      reservationPageSize: 10
    }
  },
  computed: {
    filteredReservations () {
      const { keyword, room, date, status } = this.reservationFilters
      return this.reservations.filter(item => {
        let ok = true
        if (keyword && keyword.trim()) {
          const k = keyword.trim()
          ok =
              ok &&
              (item.studentName.includes(k) ||
                  item.studentNo.includes(k) ||
                  item.code.includes(k))
        }
        if (room) {
          ok = ok && item.room === room
        }
        if (date) {
          ok = ok && item.date === date
        }
        if (status) {
          ok = ok && item.status === status
        }
        return ok
      })
    },
    paginatedReservations () {
      const start = (this.reservationPage - 1) * this.reservationPageSize
      return this.filteredReservations.slice(
          start,
          start + this.reservationPageSize
      )
    },
    reservationStats () {
      const stats = {
        total: 0,
        booked: 0,
        signed: 0,
        canceled: 0,
        violation: 0
      }
      stats.total = this.filteredReservations.length
      this.filteredReservations.forEach(item => {
        if (item.status === 'booked') stats.booked += 1
        else if (item.status === 'signed') stats.signed += 1
        else if (item.status === 'canceled') stats.canceled += 1
        else if (item.status === 'violation') stats.violation += 1
      })
      return stats
    }
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    },
    /* ===== 预约管理方法 ===== */
    handleReservationSearch () {
      console.log('执行预约查询', this.reservationFilters)
      this.reservationPage = 1
    },
    resetReservationFilters () {
      this.reservationFilters = {
        keyword: '',
        room: '',
        date: '',
        status: ''
      }
      this.reservationPage = 1
    },
    handleSelectionChange (val) {
      this.reservationSelection = val || []
    },
    handlePageChange (page) {
      this.reservationPage = page
    },
    handlePageSizeChange (size) {
      this.reservationPageSize = size
      this.reservationPage = 1
    },
    getStatusTagType (status) {
      if (status === 'booked') return 'info'
      if (status === 'signed') return 'success'
      if (status === 'canceled') return 'warning'
      if (status === 'violation') return 'danger'
      return ''
    },
    getStatusText (status) {
      if (status === 'booked') return '已预约'
      if (status === 'signed') return '已签到'
      if (status === 'canceled') return '已取消'
      if (status === 'violation') return '违约'
      return status
    },
    handleSign (row) {
      console.log('补录签到', row)
      row.status = 'signed'
    },
    handleCancel (row) {
      console.log('取消预约', row)
      row.status = 'canceled'
    },
    handleMarkViolation (row) {
      console.log('标记违约', row)
      row.status = 'violation'
    },
    handleViewDetail (row) {
      console.log('查看预约详情', row)
    },
    handleBatchSign () {
      console.log('批量补录签到', this.reservationSelection)
      this.reservationSelection.forEach(item => {
        if (item.status === 'booked') item.status = 'signed'
      })
      this.reservationSelection = []
    },
    handleBatchCancel () {
      console.log('批量取消预约', this.reservationSelection)
      this.reservationSelection.forEach(item => {
        if (item.status === 'booked') item.status = 'canceled'
      })
      this.reservationSelection = []
    }
  }
}
</script>

<style scoped>
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 通用卡片样式 */
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}

/* 预约管理页面布局 */
.reservation-card {
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

.reservation-header-right {
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

.filter-select,
.filter-date {
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

/* 统计区 */
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
.reservation-table {
  width: 100%;
}

.student-cell {
  display: flex;
  flex-direction: column;
}

.student-name {
  font-size: 13px;
  color: #111827;
}

.student-no {
  font-size: 12px;
  color: #6b7280;
}

.remark-text {
  font-size: 12px;
  color: #4b5563;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.el-dropdown-link {
  font-size: 13px;
  color: #2563eb;
  cursor: pointer;
}

.disabled-text {
  font-size: 12px;
  color: #9ca3af;
}

/* 表格底部 */
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 简单占位页面样式 */
.simple-card {
  margin-top: 0;
}

.simple-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #4b5563;
}

/* 响应式 */
@media (max-width: 900px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .reservation-header-right {
    width: 100%;
    justify-content: flex-start;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-item,
  .filter-input,
  .filter-select,
  .filter-date {
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
