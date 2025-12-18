<template>
  <div class="main">
    <!-- ============ 后台首页（Dashboard 整合版） ============ -->
    <div v-if="currentPage === 'admin-home'" class="dashboard-page">
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
            <span class="meta-item">今天：{{ todayText }}</span>
            <span class="meta-separator">·</span>
            <span class="meta-item">最近登录 IP：192.168.1.23（示例）</span>
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

          <div class="notice-footer">
            <span class="notice-dot" />
            <span class="notice-footer-text">
              温馨提示：可在左侧导航或下方待办事项中快速进入对应处理页面。
            </span>
          </div>
        </div>
      </div>

      <!-- 补充一行：最近动态 + 快捷操作（来自你原 AdminHome 的功能） -->
      <div class="dashboard-row">
        <div class="card dashboard-card">
          <h3 class="card-title">最近预约动态</h3>
          <ul class="timeline-list">
            <li v-for="item in latestActivities" :key="item.id" class="timeline-item">
              <div class="timeline-main">
                <span class="timeline-text">{{ item.text }}</span>
                <span class="timeline-tag" :class="'tag-' + item.type">
                  {{ item.typeLabel }}
                </span>
              </div>
              <div class="timeline-time">{{ item.time }}</div>
            </li>
          </ul>
        </div>

        <div class="card dashboard-card">
          <h3 class="card-title">常用快捷操作</h3>
          <div class="quick-actions">
            <el-button type="primary" @click="emitChange('admin-reservations')">
              打开预约管理
            </el-button>
            <el-button @click="emitChange('admin-seats')">
              管理自习室与座位
            </el-button>
            <el-button @click="emitChange('admin-users')">
              查看学生信用情况
            </el-button>
            <el-button @click="emitChange('admin-logs')">
              查看系统日志
            </el-button>
          </div>
          <p class="quick-tip">
            提示：你可以在左侧导航随时切换不同管理模块。
          </p>
        </div>
      </div>

      <!-- 待办事项 / 快速入口 -->
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
              <el-tag size="small" :type="getTodoTagType(scope.row.type)">
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
                <el-button size="small" type="primary" link @click="handleTodoGo(scope.row)">
                  前往处理
                </el-button>
                <el-button size="small" type="success" link @click="handleTodoDone(scope.row)">
                  标记完成
                </el-button>
                <el-tooltip content="只是示例，不会真正修改后端数据" placement="top">
                  <el-button size="small" type="info" link>详情</el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ============ 预约管理（整合版） ============ -->
    <div v-else-if="currentPage === 'admin-reservations'" class="card reservation-card">
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
          <el-option v-for="room in roomOptions" :key="room" :label="room" :value="room" />
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
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
            <span class="remark-text">{{ scope.row.remark || '—' }}</span>
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

    <!-- ============ 用户管理（整合版） ============ -->
    <div v-else-if="currentPage === 'admin-users'" class="card user-card">
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
                <span class="user-role-tag teacher" v-else-if="scope.row.role === 'teacher'">教师</span>
                <span class="user-role-tag admin" v-else-if="scope.row.role === 'admin'">管理员</span>
              </div>
              <div class="user-extra">{{ scope.row.major }} · {{ scope.row.clazz }}</div>
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
            <span class="user-extra">{{ scope.row.phone || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" min-width="230">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleResetPassword(scope.row)">
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

              <el-button type="info" link size="small" @click="handleViewUserDetail(scope.row)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- ============ 座位管理（整合版） ============ -->
    <div v-else-if="currentPage === 'admin-seats'" class="seat-manage">
      <div class="card seat-top-card">
        <div class="seat-top-left">
          <h2 class="page-title">座位管理</h2>
          <p class="page-subtitle">
            按楼栋、自习室配置开放时间与座位布局，支持快速查看空余座位、禁用问题座位，以及设置预约规则。
          </p>
        </div>
        <div class="seat-top-right">
          <el-select v-model="currentCampus" size="small" class="top-select" placeholder="选择校区">
            <el-option
                v-for="item in campusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>

          <el-select v-model="currentBuild" size="small" class="top-select" placeholder="选择楼栋" clearable>
            <el-option
                v-for="item in buildOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>

          <div class="top-switch">
            <span class="top-switch-label">显示仅开放自习室</span>
            <el-switch v-model="onlyShowOpen" size="small" />
          </div>
        </div>
      </div>

      <div class="seat-layout">
        <div class="card seat-left-card">
          <div class="left-header">
            <div class="left-header-main">
              <h3 class="left-title">自习室列表</h3>
              <p class="left-subtitle">按条件筛选自习室，查看容量与开放状态。</p>
            </div>
            <el-button size="small" type="primary" plain>新建自习室</el-button>
          </div>

          <div class="left-filter">
            <el-input
                v-model="roomKeyword"
                placeholder="按名称 / 房间号搜索"
                clearable
                size="small"
                class="left-filter-input"
            >
              <template #prefix>
                <span class="input-prefix-icon">🔍</span>
              </template>
            </el-input>

            <el-radio-group v-model="roomCapacityFilter" size="small" class="capacity-radio">
              <el-radio-button label="all">全部容量</el-radio-button>
              <el-radio-button label="small">≤40 人</el-radio-button>
              <el-radio-button label="medium">40-80 人</el-radio-button>
              <el-radio-button label="large">≥80 人</el-radio-button>
            </el-radio-group>
          </div>

          <el-table
              :data="filteredRooms"
              highlight-current-row
              size="small"
              class="room-table"
              @row-click="handleSelectRoom"
              :current-row-key="selectedRoomId"
              row-key="id"
          >
            <el-table-column prop="name" label="自习室" min-width="140">
              <template #default="scope">
                <div class="room-name">
                  <div class="room-name-main">{{ scope.row.name }}</div>
                  <div class="room-name-sub">{{ scope.row.building }} · {{ scope.row.floor }}层</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="容量" min-width="80">
              <template #default="scope">
                <span class="room-capacity">{{ scope.row.capacity }}</span>
              </template>
            </el-table-column>

            <el-table-column label="已占用" min-width="90">
              <template #default="scope">
                <span class="room-used">{{ scope.row.usedSeats }}/{{ scope.row.capacity }}</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" min-width="90">
              <template #default="scope">
                <el-tag size="small" :type="scope.row.status === 'open' ? 'success' : 'info'">
                  {{ scope.row.status === 'open' ? '开放中' : '未开放' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" min-width="120" fixed="right">
              <template #default="scope">
                <el-button type="primary" link size="small" @click.stop="toggleRoomOpen(scope.row)">
                  {{ scope.row.status === 'open' ? '关闭自习室' : '开放自习室' }}
                </el-button>
                <el-button type="info" link size="small" @click.stop="handleEditRoom(scope.row)">
                  设置
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="seat-right">
          <div class="card seat-right-card">
            <div class="right-header">
              <div>
                <h3 class="right-title">{{ selectedRoom ? selectedRoom.name : '请选择左侧自习室' }}</h3>
                <p class="right-subtitle" v-if="selectedRoom">
                  {{ selectedRoom.building }} · {{ selectedRoom.floor }}层 · 共 {{ selectedRoom.capacity }} 个座位
                </p>
                <p class="right-subtitle" v-else>
                  在左侧选择一个自习室即可查看座位布局与规则。
                </p>
              </div>

              <div class="right-header-actions" v-if="selectedRoom">
                <div class="top-switch">
                  <span class="top-switch-label">当前自习室开放</span>
                  <el-switch
                      v-model="selectedRoom.statusModel"
                      size="small"
                      @change="syncRoomStatus"
                  />
                </div>
                <el-button size="small" plain>导出座位配置</el-button>
              </div>
            </div>

            <template v-if="selectedRoom">
              <div class="seat-stats">
                <div class="stat-card">
                  <div class="stat-label">可预约座位</div>
                  <div class="stat-value">{{ seatStats.free }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">已占用座位</div>
                  <div class="stat-value">{{ seatStats.occupied }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">禁用座位</div>
                  <div class="stat-value">{{ seatStats.disabled }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">封锁区域</div>
                  <div class="stat-value">{{ seatStats.blockedArea }}</div>
                </div>
              </div>

              <el-tabs v-model="activeTab" class="seat-tabs">
                <el-tab-pane label="座位布局" name="layout">
                  <div class="layout-toolbar">
                    <div class="layout-toolbar-left">
                      <div class="top-switch">
                        <span class="top-switch-label">显示座位编号</span>
                        <el-switch v-model="showSeatNo" size="small" />
                      </div>
                      <div class="top-switch">
                        <span class="top-switch-label">标记占用示意</span>
                        <el-switch v-model="showDemoOccupied" size="small" />
                      </div>
                    </div>

                    <div class="layout-toolbar-right">
                      <el-button size="small" plain @click="handleBatchDisable">批量禁用选中</el-button>
                      <el-button size="small" plain @click="handleClearSelection">清空选择</el-button>
                    </div>
                  </div>

                  <div class="seat-legend">
                    <div class="legend-item"><span class="legend-box legend-free" /> 可预约</div>
                    <div class="legend-item"><span class="legend-box legend-occupied" /> 已占用</div>
                    <div class="legend-item"><span class="legend-box legend-disabled" /> 禁用</div>
                    <div class="legend-item"><span class="legend-box legend-selected" /> 当前选择</div>
                  </div>

                  <div class="seat-grid">
                    <div v-for="row in seatGrid" :key="row.rowIndex" class="seat-row">
                      <div class="seat-row-label">第 {{ row.rowIndex + 1 }} 行</div>
                      <div class="seat-row-seats">
                        <div
                            v-for="seat in row.seats"
                            :key="seat.id"
                            class="seat-cell"
                            :class="seatCellClass(seat)"
                            @click="handleSeatClick(seat)"
                        >
                          <span v-if="showSeatNo" class="seat-no">{{ seat.label }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-tab-pane>

                <el-tab-pane label="预约规则与开放时间" name="rules">
                  <el-form
                      :model="seatRule"
                      label-width="150px"
                      size="small"
                      class="rule-form"
                  >
                    <el-form-item label="允许跨时段预约">
                      <el-switch v-model="seatRule.allowCrossTime" />
                    </el-form-item>

                    <el-form-item label="单次预约最长时长">
                      <el-input-number v-model="seatRule.maxHoursPerOrder" :min="1" :max="8" />
                      <span class="item-desc">小时</span>
                    </el-form-item>

                    <el-form-item label="每天最多预约次数">
                      <el-input-number v-model="seatRule.maxOrdersPerDay" :min="1" :max="5" />
                      <span class="item-desc">次/人/自习室</span>
                    </el-form-item>

                    <el-form-item label="自动释放未签到座位">
                      <div class="inline-group">
                        <el-switch v-model="seatRule.autoReleaseNoSign" />
                        <span class="item-desc">超过</span>
                        <el-input-number v-model="seatRule.releaseAfterMinutes" :min="5" :max="60" />
                        <span class="item-desc">分钟自动释放</span>
                      </div>
                    </el-form-item>

                    <el-form-item label="连续违约处理策略">
                      <el-select v-model="seatRule.violationStrategy" placeholder="选择策略">
                        <el-option label="仅提醒，不限制" value="tip" />
                        <el-option label="3 次违约当天禁止预约" value="day-ban" />
                        <el-option label="连续 5 次违约一周内禁止预约" value="week-ban" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="规则说明">
                      <el-input
                          v-model="seatRule.description"
                          type="textarea"
                          :rows="3"
                          placeholder="例如：本自习室严格保持安静，请提前 10 分钟到场签到，迟到超过 20 分钟视为违约等。"
                      />
                    </el-form-item>

                    <el-form-item>
                      <el-button type="primary" size="small">
                        保存当前自习室配置（示例）
                      </el-button>
                      <el-button size="small" @click="resetSeatRule">重置为默认</el-button>
                    </el-form-item>
                  </el-form>
                </el-tab-pane>
              </el-tabs>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 投诉处理（占位页，保留入口不丢功能） ============ -->
    <div v-else-if="currentPage === 'admin-complaints'" class="card simple-card">
      <h2 class="page-title">投诉处理</h2>
      <p class="page-subtitle">处理学生反馈与投诉记录，跟踪处理结果与反馈满意度。</p>
      <p class="simple-tip">
        后续可在此页面增加投诉列表、处理进度、标记已读、导出等功能。
      </p>
    </div>

    <!-- ============ 日志统计（占位页，保留入口不丢功能） ============ -->
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
export default {
  name: 'AdminHome',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  data () {
    return {
      /* timers */
      dashboardTimer: null,
      reservationTimer: null,
      userTimer: null,

      /* Dashboard */
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
      ],
      latestActivities: [
        { id: 1, text: '张三 预约了 图书馆 401 · 10:00-12:00', type: 'book', typeLabel: '新预约', time: '09:15' },
        { id: 2, text: '李四 在 本部 · 3 楼 301 完成签到', type: 'sign', typeLabel: '已签到', time: '08:05' },
        { id: 3, text: '王五 取消了 明日的预约（原因：课程调整）', type: 'cancel', typeLabel: '已取消', time: '07:48' }
      ],

      /* Reservation Manage */
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
      reservationPageSize: 10,

      /* User Manage */
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
      userPageSize: 10,

      /* Seat Manage */
      currentCampus: 'main',
      campusOptions: [
        { label: '本部校区', value: 'main' },
        { label: '东校区', value: 'east' }
      ],
      currentBuild: '',
      buildOptions: [
        { label: '3 号教学楼', value: '3F' },
        { label: '图书馆', value: 'LIB' }
      ],
      onlyShowOpen: true,

      roomKeyword: '',
      roomCapacityFilter: 'all',
      rooms: [
        {
          id: 1,
          name: '3 楼 301 自习室',
          campus: 'main',
          building: '3 号教学楼',
          buildingCode: '3F',
          floor: 3,
          capacity: 60,
          usedSeats: 32,
          status: 'open'
        },
        {
          id: 2,
          name: '3 楼 302 自习室',
          campus: 'main',
          building: '3 号教学楼',
          buildingCode: '3F',
          floor: 3,
          capacity: 40,
          usedSeats: 12,
          status: 'open'
        },
        {
          id: 3,
          name: '图书馆 401 自习区',
          campus: 'main',
          building: '图书馆',
          buildingCode: 'LIB',
          floor: 4,
          capacity: 90,
          usedSeats: 76,
          status: 'open'
        },
        {
          id: 4,
          name: '图书馆 501 研讨区',
          campus: 'main',
          building: '图书馆',
          buildingCode: 'LIB',
          floor: 5,
          capacity: 30,
          usedSeats: 5,
          status: 'closed'
        }
      ],
      selectedRoomId: 1,

      activeTab: 'layout',
      showSeatNo: true,
      showDemoOccupied: true,
      seatGrid: [],

      seatRule: {
        allowCrossTime: false,
        maxHoursPerOrder: 4,
        maxOrdersPerDay: 2,
        autoReleaseNoSign: true,
        releaseAfterMinutes: 20,
        violationStrategy: 'day-ban',
        description: '本自习室需保持安静，迟到超过 20 分钟系统将自动释放座位。'
      }
    }
  },
  computed: {
    todayText () {
      const d = new Date()
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },

    /* reservation computed */
    filteredReservations () {
      const { keyword, room, date, status } = this.reservationFilters
      return this.reservations.filter(item => {
        let ok = true
        if (keyword && keyword.trim()) {
          const k = keyword.trim()
          ok = ok && (item.studentName.includes(k) || item.studentNo.includes(k) || item.code.includes(k))
        }
        if (room) ok = ok && item.room === room
        if (date) ok = ok && item.date === date
        if (status) ok = ok && item.status === status
        return ok
      })
    },
    paginatedReservations () {
      const start = (this.reservationPage - 1) * this.reservationPageSize
      return this.filteredReservations.slice(start, start + this.reservationPageSize)
    },
    reservationStats () {
      const stats = { total: 0, booked: 0, signed: 0, canceled: 0, violation: 0 }
      stats.total = this.filteredReservations.length
      this.filteredReservations.forEach(item => {
        if (item.status === 'booked') stats.booked++
        else if (item.status === 'signed') stats.signed++
        else if (item.status === 'canceled') stats.canceled++
        else if (item.status === 'violation') stats.violation++
      })
      return stats
    },

    /* user computed */
    filteredUsers () {
      const { keyword, role, status, onlyAbnormal } = this.userFilters
      return this.users.filter(user => {
        let ok = true
        if (keyword && keyword.trim()) {
          const k = keyword.trim()
          ok = ok && (user.name.includes(k) || user.studentNo.includes(k) || (user.major && user.major.includes(k)))
        }
        if (role) ok = ok && user.role === role
        if (status) ok = ok && user.status === status
        if (onlyAbnormal) ok = ok && (user.status === 'warning' || user.status === 'blacklist')
        return ok
      })
    },
    paginatedUsers () {
      const start = (this.userPage - 1) * this.userPageSize
      return this.filteredUsers.slice(start, start + this.userPageSize)
    },
    userStats () {
      const stats = { total: 0, normal: 0, warning: 0, blacklist: 0 }
      stats.total = this.filteredUsers.length
      this.filteredUsers.forEach(user => {
        if (user.status === 'normal') stats.normal++
        else if (user.status === 'warning') stats.warning++
        else if (user.status === 'blacklist') stats.blacklist++
      })
      return stats
    },

    /* seat computed */
    filteredRooms () {
      return this.rooms.filter(room => {
        let ok = true
        if (this.currentCampus) ok = ok && room.campus === this.currentCampus
        if (this.currentBuild) ok = ok && room.buildingCode === this.currentBuild
        if (this.onlyShowOpen) ok = ok && room.status === 'open'
        if (this.roomKeyword && this.roomKeyword.trim()) {
          const k = this.roomKeyword.trim()
          ok = ok && (room.name.includes(k) || String(room.floor).includes(k) || room.building.includes(k))
        }
        if (this.roomCapacityFilter === 'small') ok = ok && room.capacity <= 40
        if (this.roomCapacityFilter === 'medium') ok = ok && room.capacity > 40 && room.capacity < 80
        if (this.roomCapacityFilter === 'large') ok = ok && room.capacity >= 80
        return ok
      })
    },
    selectedRoom () {
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (!room) return null
      return { ...room, statusModel: room.status === 'open' }
    },
    seatStats () {
      const stats = { free: 0, occupied: 0, disabled: 0, blockedArea: 1 }
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => {
          if (seat.status === 'free') stats.free++
          else if (seat.status === 'occupied') stats.occupied++
          else if (seat.status === 'disabled') stats.disabled++
        })
      })
      return stats
    }
  },
  watch: {
    currentPage (val) {
      if (val !== 'admin-home') this.stopDashboardTimer()
      else if (this.autoRefresh) this.startDashboardTimer()

      if (val !== 'admin-reservations') this.stopReservationTimer()
      else if (this.reservationAutoRefresh) this.startReservationTimer()

      if (val !== 'admin-users') this.stopUserTimer()
      else if (this.userAutoRefresh) this.startUserTimer()
    },
    autoRefresh (val) {
      if (val && this.currentPage === 'admin-home') this.startDashboardTimer()
      else this.stopDashboardTimer()
    },
    reservationAutoRefresh (val) {
      if (val && this.currentPage === 'admin-reservations') this.startReservationTimer()
      else this.stopReservationTimer()
    },
    userAutoRefresh (val) {
      if (val && this.currentPage === 'admin-users') this.startUserTimer()
      else this.stopUserTimer()
    }
  },
  created () {
    this.initSeatGrid()
  },
  beforeUnmount () {
    this.stopDashboardTimer()
    this.stopReservationTimer()
    this.stopUserTimer()
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    },

    /* ---------- dashboard timers ---------- */
    startDashboardTimer () {
      this.stopDashboardTimer()
      this.dashboardTimer = setInterval(() => {
        this.refreshDashboardMetrics()
      }, 8000)
    },
    stopDashboardTimer () {
      if (this.dashboardTimer) {
        clearInterval(this.dashboardTimer)
        this.dashboardTimer = null
      }
    },
    refreshDashboardMetrics () {
      // 纯前端演示：轻微波动一下数值，体现“自动刷新”确实在动
      const jitter = (n, min, max) => Math.min(max, Math.max(min, n))
      this.metrics.todayReservations = jitter(this.metrics.todayReservations + (Math.random() < 0.5 ? -1 : 1) * Math.floor(Math.random() * 3), 80, 200)
      this.metrics.todaySignRate = jitter(this.metrics.todaySignRate + (Math.random() < 0.5 ? -1 : 1) * Math.floor(Math.random() * 2), 60, 98)
      this.metrics.pendingItems = jitter(this.metrics.pendingItems + (Math.random() < 0.5 ? -1 : 1), 0, 20)
    },

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
      // 让“前往处理”真的能跳页（整合后可用）
      if (row.type === 'complaint') return this.emitChange('admin-complaints')
      if (row.type === 'rule') return this.emitChange('admin-seats')
      // violation / system 统一引导到日志统计
      return this.emitChange('admin-logs')
    },
    handleTodoDone (row) {
      this.todos = this.todos.filter(t => t.id !== row.id)
    },
    handleMarkAllDone () {
      this.todos = []
    },

    /* ---------- reservation timers ---------- */
    startReservationTimer () {
      this.stopReservationTimer()
      this.reservationTimer = setInterval(() => {
        this.handleReservationSearch()
      }, 8000)
    },
    stopReservationTimer () {
      if (this.reservationTimer) {
        clearInterval(this.reservationTimer)
        this.reservationTimer = null
      }
    },

    /* reservation methods */
    handleReservationSearch () {
      this.reservationPage = 1
    },
    resetReservationFilters () {
      this.reservationFilters = { keyword: '', room: '', date: '', status: '' }
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
      row.status = 'signed'
    },
    handleCancel (row) {
      row.status = 'canceled'
    },
    handleMarkViolation (row) {
      row.status = 'violation'
    },
    handleViewDetail (row) {
      console.log('查看预约详情', row)
    },
    handleBatchSign () {
      this.reservationSelection.forEach(item => {
        if (item.status === 'booked') item.status = 'signed'
      })
      this.reservationSelection = []
    },
    handleBatchCancel () {
      this.reservationSelection.forEach(item => {
        if (item.status === 'booked') item.status = 'canceled'
      })
      this.reservationSelection = []
    },

    /* ---------- user timers ---------- */
    startUserTimer () {
      this.stopUserTimer()
      this.userTimer = setInterval(() => {
        this.handleUserSearch()
      }, 8000)
    },
    stopUserTimer () {
      if (this.userTimer) {
        clearInterval(this.userTimer)
        this.userTimer = null
      }
    },

    /* user methods */
    handleUserSearch () {
      this.userPage = 1
    },
    resetUserFilters () {
      this.userFilters = { keyword: '', role: '', status: '', onlyAbnormal: false }
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
      row.status = 'blacklist'
      if (row.creditScore > 60) row.creditScore = 60
    },
    handleUnlockUser (row) {
      row.status = 'normal'
      if (row.creditScore < 80) row.creditScore = 80
    },
    handleResetPassword (row) {
      console.log('重置密码', row)
    },
    handleViewUserDetail (row) {
      console.log('查看用户详情', row)
    },
    handleBatchLock () {
      this.userSelection.forEach(user => {
        user.status = 'blacklist'
        if (user.creditScore > 60) user.creditScore = 60
      })
      this.userSelection = []
    },
    handleBatchUnlock () {
      this.userSelection.forEach(user => {
        user.status = 'normal'
        if (user.creditScore < 80) user.creditScore = 80
      })
      this.userSelection = []
    },

    /* seat methods */
    initSeatGrid () {
      const rows = 6
      const cols = 8
      const grid = []
      let id = 1
      for (let r = 0; r < rows; r++) {
        const row = { rowIndex: r, seats: [] }
        for (let c = 0; c < cols; c++) {
          row.seats.push({
            id: id++,
            row: r,
            col: c,
            label: `${r + 1}-${c + 1}`,
            status: 'free',
            selected: false
          })
        }
        grid.push(row)
      }
      grid[0].seats[0].status = 'occupied'
      grid[0].seats[1].status = 'occupied'
      grid[2].seats[3].status = 'disabled'
      grid[3].seats[4].status = 'disabled'
      this.seatGrid = grid
    },
    handleSelectRoom (row) {
      this.selectedRoomId = row.id
    },
    toggleRoomOpen (row) {
      row.status = row.status === 'open' ? 'closed' : 'open'
    },
    handleEditRoom (row) {
      console.log('编辑自习室配置（弹窗预留）', row)
    },
    syncRoomStatus (val) {
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (room) room.status = val ? 'open' : 'closed'
    },
    seatCellClass (seat) {
      return [`seat-status-${seat.status}`, seat.selected ? 'seat-selected' : '']
    },
    handleSeatClick (seat) {
      seat.selected = !seat.selected
      if (this.showDemoOccupied && seat.status === 'free' && Math.random() < 0.05) {
        seat.status = 'occupied'
      }
    },
    handleBatchDisable () {
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => {
          if (seat.selected) {
            seat.status = 'disabled'
            seat.selected = false
          }
        })
      })
    },
    handleClearSelection () {
      this.seatGrid.forEach(row => {
        row.seats.forEach(seat => { seat.selected = false })
      })
    },
    resetSeatRule () {
      this.seatRule = {
        allowCrossTime: false,
        maxHoursPerOrder: 4,
        maxOrdersPerDay: 2,
        autoReleaseNoSign: true,
        releaseAfterMinutes: 20,
        violationStrategy: 'day-ban',
        description: '本自习室需保持安静，迟到超过 20 分钟系统将自动释放座位。'
      }
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

/* 通用卡片 */
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
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

/* ============ Dashboard styles ============ */
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
}

.welcome-left, .welcome-right { flex: 1.1; }

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

.meta-separator { opacity: 0.7; }

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

.metric-value.warning { color: #fee2e2; }

.metric-unit { font-size: 12px; margin-left: 2px; }

.metric-desc {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.9;
}

.metric-progress { margin-top: 4px; }

.middle-row {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.trend-card { flex: 1.2; }
.notice-card { flex: 1; }

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

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-item {
  padding: 6px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.trend-item:last-child { border-bottom: none; }

.trend-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-item-title { font-size: 13px; color: #111827; }
.trend-item-value { font-size: 13px; font-weight: 600; color: #111827; }
.trend-progress { margin: 4px 0; }
.trend-item-desc { font-size: 12px; color: #6b7280; }

.notice-carousel { margin-top: 4px; }
.notice-item { display: flex; flex-direction: column; justify-content: center; }

.notice-tag-wrapper { display: flex; align-items: center; gap: 8px; }
.notice-time { font-size: 12px; color: #9ca3af; }

.notice-title { margin-top: 4px; font-size: 14px; font-weight: 600; color: #111827; }
.notice-desc { margin-top: 2px; font-size: 12px; color: #4b5563; }

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

.todo-table { margin-top: 4px; }

.todo-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dashboard-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.dashboard-card { flex: 1; min-width: 280px; }

/* timeline */
.timeline-list { list-style: none; padding: 0; margin: 4px 0 0; }
.timeline-item { padding: 10px 0; border-bottom: 1px dashed #e5e7eb; }
.timeline-item:last-child { border-bottom: none; }

.timeline-main { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.timeline-text { font-size: 13px; color: #111827; }
.timeline-tag { font-size: 12px; padding: 2px 8px; border-radius: 999px; }
.tag-book { background-color: #eff6ff; color: #1d4ed8; }
.tag-sign { background-color: #ecfdf3; color: #15803d; }
.tag-cancel { background-color: #fef3c7; color: #b45309; }
.timeline-time { margin-top: 4px; font-size: 12px; color: #6b7280; }

/* quick actions */
.quick-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.quick-tip { margin-top: 8px; font-size: 12px; color: #6b7280; }

/* ============ Reservation/User shared styles ============ */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.reservation-header-right,
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

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-item { min-width: 180px; }
.filter-input { flex: 2; }
.filter-select, .filter-date { flex: 1; }

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-prefix-icon { font-size: 14px; }

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

.stat-label { font-size: 12px; color: #6b7280; }
.stat-value { margin-top: 4px; font-size: 18px; font-weight: 600; color: #111827; }

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

.toolbar-right { font-size: 12px; color: #6b7280; }
.toolbar-tip { font-size: 12px; color: #4b5563; }

.reservation-table, .user-table { width: 100%; }

.student-cell { display: flex; flex-direction: column; }
.student-name { font-size: 13px; color: #111827; }
.student-no { font-size: 12px; color: #6b7280; }

.remark-text { font-size: 12px; color: #4b5563; }

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.el-dropdown-link { font-size: 13px; color: #2563eb; cursor: pointer; }
.disabled-text { font-size: 12px; color: #9ca3af; }

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* user special */
.user-name-cell { display: flex; flex-direction: column; }
.user-name-main { font-size: 13px; color: #111827; }

.user-role-tag {
  margin-left: 4px;
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 11px;
  background-color: #eff6ff;
  color: #1d4ed8;
}

.user-role-tag.teacher { background-color: #ecfdf3; color: #15803d; }
.user-role-tag.admin { background-color: #fef3c7; color: #b45309; }

.user-extra { font-size: 12px; color: #6b7280; }

.credit-score { font-size: 13px; font-weight: 600; color: #111827; }
.credit-low { color: #b91c1c; }
.credit-high { color: #15803d; }

/* ============ Seat Manage styles ============ */
.seat-manage {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.seat-top-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.seat-top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-select { min-width: 140px; }

.top-switch {
  display: flex;
  align-items: center;
  gap: 6px;
}

.top-switch-label {
  font-size: 12px;
  color: #4b5563;
}

.seat-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.seat-left-card {
  flex: 0 0 40%;
  max-width: 420px;
}

.left-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.left-title { margin: 0; font-size: 15px; font-weight: 600; color: #111827; }
.left-subtitle { margin: 4px 0 0; font-size: 12px; color: #6b7280; }

.left-filter {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.room-table { width: 100%; }

.room-name { display: flex; flex-direction: column; }
.room-name-main { font-size: 13px; color: #111827; }
.room-name-sub { font-size: 12px; color: #6b7280; }

.seat-right { flex: 1; }
.seat-right-card { width: 100%; }

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 16px;
}

.right-title { margin: 0; font-size: 16px; font-weight: 600; color: #111827; }
.right-subtitle { margin: 4px 0 0; font-size: 12px; color: #6b7280; }

.right-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seat-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.seat-tabs { margin-top: 4px; }

.layout-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0 8px;
  flex-wrap: wrap;
  gap: 8px;
}

.layout-toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.layout-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.seat-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.legend-box {
  width: 14px;
  height: 14px;
  border-radius: 4px;
}

.legend-free { background-color: #e0f2fe; }
.legend-occupied { background-color: #fee2e2; }
.legend-disabled { background-color: #e5e7eb; }
.legend-selected { background-color: #ddd6fe; }

.seat-grid {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(135deg, #f3f4f6, #f9fafb);
  padding: 12px;
  box-sizing: border-box;
}

.seat-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.seat-row:last-child { margin-bottom: 0; }

.seat-row-label {
  width: 70px;
  font-size: 12px;
  color: #6b7280;
}

.seat-row-seats {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 6px;
}

.seat-cell {
  height: 26px;
  border-radius: 6px;
  box-sizing: border-box;
  cursor: pointer;
  position: relative;
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}

.seat-cell:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(15, 23, 42, 0.12);
}

.seat-status-free { background-color: #e0f2fe; border: 1px solid #bfdbfe; }
.seat-status-occupied { background-color: #fee2e2; border: 1px solid #fecaca; }
.seat-status-disabled { background-color: #e5e7eb; border: 1px solid #d1d5db; }

.seat-selected { box-shadow: 0 0 0 2px #a855f7; }

.seat-no {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: #0f172a;
}

.rule-form { margin-top: 6px; }

.item-desc {
  margin-left: 6px;
  font-size: 12px;
  color: #6b7280;
}

.inline-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* simple page */
.simple-tip { margin-top: 12px; font-size: 13px; color: #4b5563; }

/* responsive */
@media (max-width: 1024px) {
  .welcome-card { flex-direction: column; }
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .middle-row { flex-direction: column; }
}

@media (max-width: 980px) {
  .seat-layout { flex-direction: column; }
  .seat-left-card { flex: 1; max-width: 100%; }
}

@media (max-width: 900px) {
  .card-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .reservation-header-right, .user-header-right { width: 100%; justify-content: flex-start; }
  .filter-bar { flex-direction: column; }
  .filter-item, .filter-input, .filter-select, .filter-date { width: 100%; }
  .reservation-toolbar { flex-direction: column; align-items: flex-start; }
  .table-footer { justify-content: center; }
}

@media (max-width: 780px) {
  .seat-top-card { flex-direction: column; align-items: flex-start; }
  .seat-top-right { flex-wrap: wrap; }
  .right-header { flex-direction: column; align-items: flex-start; }
  .layout-toolbar { flex-direction: column; align-items: flex-start; }
}
</style>