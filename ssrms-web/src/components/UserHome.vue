<template>
  <div class="main">
    <!-- 首页 -->
    <div v-if="currentPage === 'home'" class="home-wrapper">
      <div class="card home-card">
        <!-- 顶部：欢迎 + 右上角天气小卡片 -->
        <div class="home-top home-head">
          <div class="home-intro">
            <h2 class="page-title">{{ timeGreeting }}，欢迎使用自习室预约系统（学生端）</h2>
            <p class="page-subtitle">
              今天是 {{ todayStr }} · {{ weekDayStr }}。你可以在这里查看自习室开放情况、预约座位、查看自己的预约记录以及违规状态等。
            </p>
          </div>

          <!-- 右上角天气迷你卡片 -->
          <div v-if="weatherData" class="weather-mini">
            <div class="weather-main">
              <span class="weather-icon">{{ emojiWeather(weatherData.desc) }}</span>
              <span class="weather-temp">{{ weatherData.temp }}°C</span>
            </div>
            <div class="weather-desc">
              {{ weatherData.city }} · {{ weatherData.desc }}
            </div>
          </div>
        </div>

        <!-- 中部布局：按行排列 -->
        <div class="home-main-grid">
          <!-- 第一行：今日提示（整行，居中） -->
          <div class="home-row">
            <div class="quote-card" v-if="dailyQuote">
              <!-- 右上角刷新按钮 -->
              <button
                  type="button"
                  class="quote-refresh-btn"
                  title="换一句"
                  @click="refreshQuote"
              >
                ↻
              </button>

              <div class="quote-header">
                <span class="quote-icon-inline">💡</span>
                <span class="quote-label">今日提示</span>
              </div>

              <div class="quote-content">
                <div class="quote-text">{{ dailyQuote }}</div>
              </div>
            </div>
          </div>

          <!-- 第二行：今日自习室概况（左） + 本月学习简报（右） -->
          <div class="home-row-two">
            <!-- 左：今日自习室概况 -->
            <div class="home-panel home-overview">
              <div class="home-panel-header">
                <div class="home-panel-title">今日自习室概况</div>
              </div>
              <div class="home-panel-body home-overview-body">
                <div class="home-overview-line">
                  <div class="home-panel-number">180 个座位</div>
                  <div class="home-panel-desc">
                    已预约 72 · 正在使用 58 · 剩余 50
                  </div>
                </div>
              </div>
            </div>

            <!-- 右：本月学习简报 -->
            <div class="month-report">
              <div class="report-title">本月学习简报</div>
              <div class="report-row">
                <span>本月累计预约</span>
                <span><strong>12</strong> 次</span>
              </div>
              <div class="report-row">
                <span>本月累计自习时长</span>
                <span><strong>28</strong> 小时</span>
              </div>
              <div class="report-row">
                <span>最近一次到馆</span>
                <span>昨天 19:10</span>
              </div>
            </div>
          </div>

          <!-- 第三行：公告 / 通知（整行） -->
          <div class="home-row">
            <div class="home-panel notice-panel">
              <div class="home-panel-header">
                <div class="home-panel-title">公告 / 通知</div>
                <button type="button" class="notice-more-btn">查看全部</button>
              </div>

              <ul class="notice-list">
                <li
                    v-for="item in homeNotices"
                    :key="item.id"
                    class="notice-item"
                >
                  <div
                      class="notice-tag"
                      :class="'notice-level-' + item.level"
                  >
                    {{ item.levelText }}
                  </div>
                  <div class="notice-main">
                    <div
                        class="notice-title"
                        :title="item.title"
                    >
                      {{ item.title }}
                    </div>
                    <div class="notice-meta">
                      {{ item.date }} · {{ item.target }}
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- 第四行：我的今日预约（整行） -->
          <div class="home-row">
            <div class="home-panel my-today-card">
              <div class="home-panel-header">
                <div class="home-panel-title">我的今日预约</div>
                <div
                    class="home-panel-subtitle"
                    v-if="todayAppointments.length"
                >
                  今日共 {{ todayAppointments.length }} 场自习安排
                </div>
                <div
                    class="home-panel-subtitle"
                    v-else
                >
                  今日暂无预约，可以安排一场高效自习～
                </div>
              </div>

              <div class="home-panel-body my-today-body">
                <!-- 有预约时，展示前 3 条 -->
                <template v-if="todayAppointments.length">
                  <div
                      v-for="(item, idx) in todayAppointments.slice(0, 3)"
                      :key="idx"
                      class="today-item"
                  >
                    <div class="today-left">
                      <div class="today-time">{{ formatTimeRange(item) }}</div>
                      <div class="today-room">
                        {{ item.roomFullName || item.roomName || '自习室' }}
                      </div>
                    </div>

                    <span class="badge" :class="statusClass(item.status)">
              {{ renderStatusText(item.status) }}
            </span>
                  </div>
                </template>

                <!-- 没预约时的文案 -->
                <div
                    v-else
                    class="today-empty"
                >
                  当前还没有预约，左侧导航栏中的“座位预约”可以快速创建新的预约。
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右下角“随手一评”浮层 -->
      <transition name="fade">
        <div v-if="openFeedback" class="feedback-float">
          <div class="feedback-panel">
            <div class="feedback-title">今天的自习氛围如何？</div>
            <div class="feedback-emojis">
              <span @click="submitFB(1)">😣</span>
              <span @click="submitFB(2)">😕</span>
              <span @click="submitFB(3)">🙂</span>
              <span @click="submitFB(4)">😊</span>
              <span @click="submitFB(5)">🤩</span>
            </div>
          </div>
        </div>
      </transition>
      <div class="feedback-float" v-if="!openFeedback">
        <div class="feedback-btn" @click="openFeedback = true">
          评价
        </div>
      </div>
    </div>

    <!-- 我要预约页面（美化版） -->
    <div v-else-if="currentPage === 'user-reserve'">
      <div class="card reserve-card">
        <!-- 顶部：标题 + 当前选择概览 -->
        <div class="reserve-header-row">
          <div class="reserve-header-left">
            <h2 class="page-title">我要预约</h2>
            <p class="page-subtitle">
              选择自习室、日期和时间段进行预约，点击卡片选择 / 取消，最多可同时选择 4 个时段。
            </p>
          </div>

          <!-- 右侧：当前选择概要 -->
          <div class="reserve-summary-box">
            <div class="summary-title">当前选择</div>
            <div class="summary-main">
              <div class="summary-row">
                <span class="summary-label">场地</span>
                <span class="summary-value">
              {{ currentVenueName }}
            </span>
              </div>
              <div class="summary-row">
                <span class="summary-label">日期</span>
                <span class="summary-value">
              {{ currentDateLabel || '请选择日期' }}
            </span>
              </div>
              <div class="summary-row">
                <span class="summary-label">时段</span>
                <span
                    class="summary-value"
                    v-if="selectedSlots.length"
                >
              已选 {{ selectedSlots.length }} / 4 个
            </span>
                <span
                    class="summary-value summary-empty"
                    v-else
                >
              尚未选择
            </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 场地选择 + “只看可预约”开关 -->
        <div class="reserve-toolbar">
          <div class="venue-tabs">
            <button
                v-for="(venue, index) in reserveVenues"
                :key="venue"
                type="button"
                class="venue-tab"
                :class="{ active: reserveVenueIndex === index }"
                @click="handleVenueClick(index)"
            >
              {{ venue }}
            </button>
          </div>

          <div class="reserve-toolbar-right">
            <span class="switch-label">只看可预约时段</span>
            <!-- Element Plus 的开关组件 -->
            <el-switch
                v-model="onlyShowAvailable"
                size="small"
            />
          </div>
        </div>

        <!-- 日期条 -->
        <div class="date-strip">
          <button
              class="date-arrow"
              type="button"
              @click="moveDates('prev')"
              :disabled="!canMovePrev"
          >
            ‹
          </button>

          <div class="date-list">
            <button
                v-for="d in visibleDates"
                :key="d.key"
                type="button"
                class="date-item"
                :class="{ active: currentDateIndex === d.index }"
                @click="selectDate(d.index)"
            >
              <div class="date-month">{{ d.monthLabel }}</div>
              <div class="date-day-row">
                <span class="date-day-number">{{ d.day }}</span>
                <span class="date-weekday">{{ d.weekday }}</span>
              </div>
              <div class="date-extra" v-if="d.isToday">今天</div>
              <div class="date-extra" v-else-if="d.isTomorrow">明天</div>
            </button>
          </div>

          <button
              class="date-arrow"
              type="button"
              @click="moveDates('next')"
              :disabled="!canMoveNext"
          >
            ›
          </button>
        </div>

        <!-- 时间段选择区域 -->
        <div class="slot-section">
          <div class="slot-header">
            <div class="slot-title">选择时间段</div>
            <div class="slot-legend">
          <span class="legend-item">
            <span class="legend-dot legend-available"></span>可预约
          </span>
              <span class="legend-item">
            <span class="legend-dot legend-selected"></span>已选
          </span>
              <span class="legend-item">
            <span class="legend-dot legend-disabled"></span>不可预约
          </span>
            </div>
          </div>

          <div class="slot-grid">
            <button
                v-for="slot in filteredTimeSlots"
                :key="slot.id"
                type="button"
                class="slot-item"
                :class="slotClass(slot)"
                :disabled="slotState(slot) === 'disabled'"
                @click="toggleSlot(slot)"
            >
              <span class="slot-time">{{ slot.label }}</span>
              <span class="slot-range">{{ slot.range }}</span>
            </button>
          </div>
        </div>

        <!-- 已选时段 + 提交区域 -->
        <div class="reserve-bottom">
          <div class="selected-tags" v-if="selectedSlots.length">
            <div
                v-for="item in selectedSlots"
                :key="item.key"
                class="selected-tag"
            >
          <span class="selected-tag-text">
            {{ item.dateLabel }} · {{ currentVenueName }} · {{ item.slot.range }}
          </span>
              <button
                  type="button"
                  class="selected-tag-close"
                  @click="removeSelected(item.key)"
              >
                ×
              </button>
            </div>
          </div>

          <div class="reserve-actions">
            <button
                type="button"
                class="link-btn"
                v-if="selectedSlots.length"
                @click="resetSelections"
            >
              清空选择
            </button>

            <button
                type="button"
                class="primary-btn reserve-btn"
                :disabled="!selectedSlots.length"
                @click="submitReservations"
            >
              确认预约
            </button>
          </div>

          <p class="hint-text">
            点击“确认预约”后会弹出当前选择的时段，并跳转到“我的预约”。
          </p>
        </div>
      </div>
    </div>

    <!-- 我的预约 -->
    <div v-else-if="currentPage === 'user-reservations'">
      <div class="card card-reservations">
        <h2 class="page-title">预约与签到签退</h2>
        <div class="table-wrapper">
          <table class="table">
            <thead>
            <tr>
              <th>预约编号</th>
              <th>自习室</th>
              <th>日期</th>
              <th>时间段</th>
              <th>座位号</th>
              <th>状态</th>
              <th style="text-align: right;">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr
                v-for="item in pagedReservations"
                :key="item.id"
            >
              <td>{{ item.reservationNo }}</td>
              <td>{{ item.roomName }}</td>
              <td>{{ item.date }}</td>
              <td>{{ formatTimeRange(item) }}</td>
              <td>{{ item.seatLabel || '-' }}</td>
              <td>
      <span
          class="badge"
          :class="statusClass(item.status)"
      >
        {{ renderStatusText(item.status) }}
      </span>
              </td>
              <td class="text-right">
                <!-- 待签到：可以“签到 / 取消” -->
                <template v-if="item.status === 'reserved'">
                  <button
                      class="link-btn"
                      type="button"
                      @click="handleCheckIn(item)"
                  >
                    签到
                  </button>
                  <button
                      class="link-btn link-danger"
                      type="button"
                      :disabled="!canCancel(item)"
                      @click="handleCancel(item)"
                  >
                    取消
                  </button>
                </template>

                <!-- 已签到 / 迟到 -->
                <button
                    class="link-btn"
                    v-else-if="item.status === 'checked_in' || item.status === 'late'"
                    type="button"
                    disabled
                >
                  已签到
                </button>

                <!-- 未签到 -->
                <button
                    class="link-btn"
                    v-else-if="item.status === 'no_show'"
                    type="button"
                    disabled
                >
                  已过期
                </button>

                <!-- 已取消 / 逾期取消（都不可再操作） -->
                <button
                    class="link-btn"
                    v-else-if="item.status === 'cancelled' || item.status === 'cancel_overdue'"
                    type="button"
                    disabled
                >
                  不可操作
                </button>
              </td>
            </tr>
            <!-- 没有任何预约时的占位行 -->
            <tr v-if="!myReservations.length">
              <td colspan="7" style="text-align: center; color: #9ca3af; padding: 16px 0;">
                暂无预约记录
              </td>
            </tr>
            </tbody>
          </table>
        </div>
        <!-- ⭐ 分页条：只有当总数 > 15 时才出现 -->
        <div class="pager" v-if="totalPages > 1">
          <!-- 上一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': reservationPageIndex <= 1 }"
              :disabled="reservationPageIndex <= 1"
              @click="gotoPrevPage"
          >
            上一页
          </button>

          <!-- 中间页码 -->
          <div class="pager-center">
            第 {{ reservationPageIndex }} / {{ totalPages }} 页
          </div>

          <!-- 下一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': reservationPageIndex >= totalPages }"
              :disabled="reservationPageIndex >= totalPages"
              @click="gotoNextPage"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 违规记录 -->
    <div v-else-if="currentPage === 'user-violations'">
      <div class="card">
        <h2 class="page-title">违规记录与信用分</h2>
        <p class="page-subtitle">
          信用分从 100 开始，未签到、迟到等行为会扣分。
        </p>

        <div class="credit-summary">
          <div class="credit-score">
            当前信用分：
            <span class="score-number">
          {{ 100 + (myViolations || []).reduce((sum, v) => sum + (v.penaltyScore || 0), 0) }}
        </span>
          </div>
          <p class="hint-text">
            信用分低于 60 可能会被列入黑名单，一段时间内无法预约。
          </p>
        </div>

        <div class="table-wrapper">
          <table class="table">
            <thead>
            <tr>
              <th>日期</th>
              <th>自习室</th>
              <th>违规类型</th>
              <th>扣分</th>
              <th>备注</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="!pagedViolations.length">
              <td colspan="5" style="text-align: center; color: #999;">
                暂无违规记录
              </td>
            </tr>
            <tr v-for="item in pagedViolations" :key="item.reservationId">
              <td>{{ item.date }}</td>
              <td>{{ item.roomFullName }}</td>
              <td>{{ item.violationType }}</td>
              <td>{{ item.penaltyScore }}</td>
              <td>{{ item.remark }}</td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页器 -->
        <div class="pager" v-if="violationTotalPages > 1">
          <!-- 上一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': violationPageIndex <= 1 }"
              :disabled="violationPageIndex <= 1"
              @click="gotoPrevViolationPage"
          >
            上一页
          </button>

          <div class="pager-center">
            第 {{ violationPageIndex }} / {{ violationTotalPages }} 页
          </div>

          <!-- 下一页 -->
          <button
              type="button"
              class="pager-btn"
              :class="{ 'pager-btn-disabled': violationPageIndex >= violationTotalPages }"
              :disabled="violationPageIndex >= violationTotalPages"
              @click="gotoNextViolationPage"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 评价与投诉 -->
    <div v-else-if="currentPage === 'user-feedback'">
      <div class="card">
        <h2 class="page-title">评价与投诉</h2>
        <p class="page-subtitle">
          可以对自习环境、规则执行情况进行评价，也可以提交投诉信息（示意表单）。
        </p>

        <div class="feedback-grid">
          <div class="feedback-column">
            <h3 class="section-title">快速评价</h3>
            <textarea
                class="textarea"
                rows="4"
                placeholder="例如：环境安静、卫生良好、管理员服务态度不错等。"
            ></textarea>
            <button class="primary-btn">提交评价</button>
          </div>
          <div class="feedback-column">
            <h3 class="section-title">投诉 / 建议</h3>
            <textarea
                class="textarea"
                rows="4"
                placeholder="请详细描述问题：发生时间、地点、涉及人员、具体情况等，以便管理员跟进处理。"
            ></textarea>
            <button class="primary-btn">提交投诉</button>
          </div>
        </div>

        <p class="hint-text">
          示例页面不会真正发送数据到后端，接入接口后，按钮可触发实际提交逻辑。
        </p>
      </div>
    </div>

    <!-- 个人中心 -->
    <div v-else-if="currentPage === 'user-profile'">
      <div class="card full-page-card">
        <!-- 顶部用户信息区域 -->
        <div class="user-header-section">
          <div class="user-info-header">
            <div class="user-info-left">
              <div class="user-profile-info">
                <!-- 这里显示后端来的姓名，没有就先叫“同学” -->
                <h2 class="user-name">
                  {{ profileForm.name || '同学' }}
                </h2>
                <span class="user-role">学生</span>
              </div>
            </div>
            <div class="header-actions">
              <!-- 预留退出登录按钮 -->
              <!-- <button class="logout-btn">退出登录</button> -->
            </div>
          </div>
        </div>

        <!-- 个人信息表单 -->
        <div class="profile-content">
          <!-- 个人信息模块 -->
          <div class="form-section">
            <h3 class="section-title">个人信息</h3>

            <div class="form-group">
              <div class="form-grid">
                <!-- 左边：姓名 -->
                <div class="form-item">
                  <label>姓名:</label>
                  <input
                      type="text"
                      v-model="profileForm.name"
                  />
                </div>
                <!-- 右边：学号（student_no） -->
                <div class="form-item">
                  <label>学号:</label>
                  <input
                      type="text"
                      v-model="profileForm.studentNo"
                  />
                </div>
                <div class="form-item">
                  <label>学院:</label>
                  <select v-model="profileForm.college">
                    <option value="">请选择学院</option>
                    <option value="计算机学院">计算机学院</option>
                    <option value="信息工程学院">信息工程学院</option>
                    <option value="商学院">商学院</option>
                    <option value="法学院">法学院</option>
                  </select>
                </div>
                <div class="form-item">
                  <label>年级与班级:</label>
                  <input
                      type="text"
                      v-model="profileForm.gradeClass"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 联系方式模块 -->
          <div class="form-section">
            <h3 class="section-title">联系方式</h3>

            <div class="form-grid">
              <div class="form-item">
                <label>手机号:</label>
                <div class="input-with-action">
                  <input
                      type="text"
                      v-model="profileForm.phone"
                  />
                </div>
              </div>
              <div class="form-item">
                <label>邮箱:</label>
                <div class="input-with-action">
                  <input
                      type="email"
                      v-model="profileForm.email"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 偏好设置模块 -->
          <div class="form-section">
            <h3 class="section-title">偏好设置</h3>

            <div class="form-grid">
              <div class="form-item">
                <label>常用校区:</label>
                <div class="radio-group">
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="本部校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">本部校区</span>
                  </label>
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="东校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">东校区</span>
                  </label>
                  <label class="radio-option">
                    <input
                        type="radio"
                        value="梅山校区"
                        v-model="profileForm.commonCampus"
                    />
                    <span class="radio-label">梅山校区</span>
                  </label>
                </div>
              </div>
              <div class="form-item full-width">
                <label>备注信息:</label>
                <textarea
                    placeholder="例如：偏好图书馆、晚上时段较多、不擅长高数等"
                    v-model="profileForm.profileRemark"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="profile-actions">
            <button
                class="primary-btn"
                type="button"
                @click="handleProfileSave"
                :disabled="profileSaving"
            >
              {{ profileSaving ? '保存中…' : '保存信息' }}
            </button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
export default {
  name: 'AppHome',
  props: {
    currentPage: {
      type: String,
      required: true
    }
  },
  emits: ['change-page'],
  data () {
    return {
      // 场地（地点）列表
      reserveVenues: ['本部 · 图书馆 301', '本部 · 图书馆 401', '东校区 · 教学楼 3 楼'],
      reserveVenueIndex: 0,

      // 日期相关
      dateList: [],
      visibleStart: 0,
      visibleCount: 7, // 一屏展示多少天
      currentDateIndex: 0,

      // 时间段列表（0 点~24 点）
      timeSlots: [],

      // 后端返回的“已满时段”（id 列表，例如 [8,9,14]）
      disabledSlotIds: [],

      // 已选时段（最多 4 个）
      selectedSlots: [],

      // 是否只展示“可预约”的时段
      onlyShowAvailable: false,

      // 房间 id 映射（按顺序和 reserveVenues 对应）
      roomIds: [1, 2, 3], // 换成你 room 表里的真实 id

      // 当前登录用户 id（从登录时存的用户信息里读）
      currentUserId: null,

      // 我的预约列表
      myReservations: [],

      // 我的预约分页
      pageSize: 15,
      reservationPageIndex: 1,

      // 违规记录相关
      myViolations: [],        // 后端拉回来的完整违规列表
      violationPageIndex: 1,   // 当前违规页码
      violationPageSize: 15,

      // 个人中心表单数据
      profileForm: {
        name: '',          // 姓名
        account: '',       // 登录账号
        studentNo: '',     // 学号
        college: '',
        gradeClass: '',
        phone: '',
        email: '',
        commonCampus: '',
        profileRemark: ''
      },
      profileLoading: false,
      profileSaving: false,

      // 首页 - 天气、金句、快捷反馈
      weatherData: null,       // 天气数据对象
      dailyQuote: '',          // 今日一句话
      openFeedback: false,     // 右下角快捷评价浮层是否打开

      // 首页 - 公告列表示例数据（后续可以接 /notice 接口替换）
      homeNotices: [
        {
          id: 1,
          title: '【开放时间调整】本部图书馆 301 自习室本周末延长开放至 22:30',
          date: '04-02',
          level: 'important',
          levelText: '重要',
          target: '全体学生'
        },
        {
          id: 2,
          title: '【考试占用提醒】本周六下午 14:00–17:00 401 自习室用于四六级模拟考试',
          date: '03-28',
          level: 'info',
          levelText: '提醒',
          target: '英语考生'
        },
        {
          id: 3,
          title: '【设备维护】东校区 3 楼自习室 4 月 3 日 9:00–12:00 暂停开放',
          date: '03-27',
          level: 'warning',
          levelText: '维护',
          target: '东校区学生'
        }
      ],

      // 放一个函数在 data 里也没问题，模板中可以直接调用
      emojiWeather: function (desc) {
        if (!desc) return '⛅'
        if (desc.includes('雨')) return '🌧️'
        if (desc.includes('云')) return '⛅'
        if (desc.includes('晴')) return '☀️'
        if (desc.includes('雪')) return '❄️'
        return '⛅'
      },

      // 首页底部的随机一句话
      quotes: [
        '代码写完要多测试，bug 总会在你最不想看到它的时候出现。',
        '保持自律的最好方式，就是给自己定一个很清晰、很小但能做到的目标。',
        '信用记录就像存钱罐，一点一滴都在改变别人对你的信任度。',
        '早点到教室，晚点离开，安静的自习室会给你额外的安全感。',
        '学习是场马拉松，保持节奏比短时间爆发更重要。',
        '不想学的时候，先坐下来学五分钟，很多坚持都是从这五分钟开始的。',
        '记不住是很正常的事，多写几遍、多讲几遍，大脑才知道这东西很重要。',
        '能在自习室刷手机，就一定能在自习室刷完一套题，选哪个看你自己。',
        '今天偷的懒，都会在考试周加倍还回来。',
        '别总羡慕别人自律，其实他们只是一次次按下了「继续做」而不是「算了吧」。',
        '看不懂的题先标记，不要卡死在一个地方，一道题拖垮一晚上太亏了。',
        '复习最大的骗局，是「我好像都看过」；真正有用的是「这题我能当场写出来」。',
        '熬夜是把信用卡，透支的是精神和身体，迟早要还的。',
        '专注一小时，胜过边刷手机边学习三小时。',
        '自习室不是用来躺平的地方，是用来慢慢把焦虑变成底气的地方。',
        '再晚也比不开始好，再小的进步也是在往前走。',
        '今天多坐十分钟，期末就少一点「背水一战」的紧张。',
        '你以为记不住的知识，其实只是还没复习到第二遍、第三遍。',
        '迟到一次没什么，但习惯迟到会慢慢把所有计划都打乱。',
        '把「明天再说」改成「现在先做一点」，很多事就不会堆成山。'
      ],
    }
  },
  computed: {
    visibleDates () {
      return this.dateList
          .slice(this.visibleStart, this.visibleStart + this.visibleCount)
          .map((d, idx) => ({
            ...d,
            index: this.visibleStart + idx
          }))
    },
    canMovePrev () {
      return this.visibleStart > 0
    },
    canMoveNext () {
      return this.visibleStart + this.visibleCount < this.dateList.length
    },
    currentVenueName () {
      return this.reserveVenues[this.reserveVenueIndex] || ''
    },
    // 当前选中的日期完整文本（用于右侧概要）
    currentDateLabel () {
      const cur = this.dateList[this.currentDateIndex]
      return cur ? cur.fullLabel : ''
    },
    // 根据开关过滤时间段列表
    filteredTimeSlots () {
      if (!this.onlyShowAvailable) {
        return this.timeSlots
      }
      // 只展示“可预约”的时段
      return this.timeSlots.filter(slot => this.slotState(slot) === 'available')
    },
    // 当前房间 id
    currentRoomId () {
      return this.roomIds[this.reserveVenueIndex]
    },
    // 后端需要 yyyy-MM-dd 格式的日期字符串
    currentDateStr () {
      const cur = this.dateList[this.currentDateIndex]
      // 我们在 initDates 里已经生成了 fullLabel = '2025-12-13'
      return cur ? cur.fullLabel : null
    },
    // 总页数
    totalPages () {
      if (!this.myReservations.length) return 0
      return Math.ceil(this.myReservations.length / this.pageSize)
    },
    // 当前页要展示的 15 条
    pagedReservations () {
      if (!this.myReservations.length) return []
      const total = this.totalPages || 1
      const page = Math.min(this.reservationPageIndex, total)
      const start = (page - 1) * this.pageSize
      const end = start + this.pageSize
      return this.myReservations.slice(start, end)
    },
    violationTotalPages () {
      if (!this.myViolations.length) return 0
      return Math.ceil(this.myViolations.length / this.violationPageSize)
    },
    pagedViolations () {
      if (!this.myViolations.length) return []
      const total = this.violationTotalPages || 1
      const page = Math.min(this.violationPageIndex, total)
      const start = (page - 1) * this.violationPageSize
      const end = start + this.violationPageSize
      return this.myViolations.slice(start, end)
    },
    // 首页问候：今天日期、星期几、时间段问候语
    todayStr () {
      const d = new Date()
      return `${d.getMonth() + 1} 月 ${d.getDate()} 日`
    },
    weekDayStr () {
      const list = ['日', '一', '二', '三', '四', '五', '六']
      return '星期' + list[new Date().getDay()]
    },
    timeGreeting () {
      const h = new Date().getHours()
      if (h < 11) return '上午好'
      if (h < 14) return '中午好'
      if (h < 19) return '下午好'
      return '晚上好'
    },
    // 新增：我的今日预约（从已加载的 myReservations 中筛选“今天”的记录）
    todayAppointments () {
      const list = Array.isArray(this.myReservations) ? this.myReservations : []
      if (!list.length) return []

      const todayStr = new Date().toISOString().slice(0, 10) // yyyy-MM-dd
      return list.filter(item => item.date === todayStr)
    },
  },
  created () {
    // 初始化时间段、日期
    this.timeSlots = this.buildTimeSlots()
    this.initDates()

    // 读取当前登录用户：从 localStorage 的 ssrmsUser 里取
    const raw = localStorage.getItem('ssrmsUser')
    if (raw) {
      try {
        const user = JSON.parse(raw)
        console.log('localStorage ssrmsUser = ', user)

        // ⭐ 先把 currentUserId 和表单都用本地数据填上
        this.currentUserId = user.id  // 如果你登录返回的是 user.userId，这里就改成 user.userId

        this.profileForm = {
          name: user.name || '',
          account: user.account || '',          // 登录账号
          studentNo: user.studentNo || '',      // 学号
          college: user.college || '',
          gradeClass: user.gradeClass || '',
          phone: user.phone || '',
          email: user.email || '',
          commonCampus: user.commonCampus || '',
          profileRemark: user.profileRemark || ''
        }

        // ⭐ 再去请求后端，拿“最新”的一份覆盖
        this.loadUserProfile()
      } catch (e) {
        console.error('解析 ssrmsUser 失败', e)
      }
    } else {
      console.warn('localStorage 里没有 ssrmsUser')
    }

    // 初始化完后拉一次当前选中日期（“明天”）的占用情况
    this.fetchSlotStatus()

    // 首页相关：天气 & 随机一句话
    this.loadWeather()
    if (this.quotes && this.quotes.length) {
      const idx = Math.floor(Math.random() * this.quotes.length)
      this.dailyQuote = this.quotes[idx]
    }
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    },

    // 点击场地 tab
    handleVenueClick (index) {
      this.reserveVenueIndex = index
      this.fetchSlotStatus()
    },

    buildTimeSlots () {
      const list = []

      // 👉 这里控制开放的时间段
      // startHour：第一个时段的开始小时
      // endHour：最后一个时段的开始小时
      // 下面这个例子：从 08:00-09:00 一直到 22:00-23:00
      const startHour = 8   // 08:00
      const endHour = 22    // 22:00

      for (let h = startHour; h <= endHour; h++) {
        const next = h + 1
        const id = String(h)   // 仍然用小时当作 slotId，和后端保持一致

        const range =
            `${h.toString().padStart(2, '0')}:00 - ` +
            `${next.toString().padStart(2, '0')}:00`

        const label =
            (h % 12 === 0 ? 12 : h % 12) +
            ': 00 ' +
            (h < 12 ? 'AM' : 'PM')

        list.push({
          id,
          label,
          range
        })
      }
      return list
    },



    // 生成接下来 14 天的日期条（从“明天”开始）
    initDates () {
      const today = new Date()
      const list = []
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

      for (let i = 0; i < 14; i++) {
        // ⭐ 这里加 1，表示从“明天”开始
        const d = new Date(today)
        d.setDate(d.getDate() + i + 1)

        const month = d.getMonth() + 1
        const day = d.getDate()
        const fullLabel =
            `${d.getFullYear()}-${month.toString().padStart(2, '0')}-` +
            `${day.toString().padStart(2, '0')}`

        list.push({
          key: i,   // 用 0~13 当 key 就够了
          year: d.getFullYear(),
          month,
          day,
          weekday: weekdays[d.getDay()],
          monthLabel: `${d.getFullYear()}-${month.toString().padStart(2, '0')}`,
          isToday: false,        // ⭐ 不再有“今天”
          isTomorrow: i === 0,   // ⭐ 第一张标记为“明天”
          fullLabel
        })
      }

      this.dateList = list
      this.visibleStart = 0
      this.currentDateIndex = 0   // ⭐ 默认选中第一天（也就是“明天”）
    },

    moveDates (direction) {
      if (direction === 'prev' && this.canMovePrev) {
        this.visibleStart -= 1
      } else if (direction === 'next' && this.canMoveNext) {
        this.visibleStart += 1
      }
    },

    // 点击日期
    selectDate (index) {
      this.currentDateIndex = index
      this.fetchSlotStatus()
    },

    buildKey (dateIndex, slotId) {
      return `${dateIndex}-${slotId}`
    },

    // 当前时段是可预约 / 已选 / 不可预约
    slotState (slot) {
      if (this.disabledSlotIds.includes(slot.id)) {
        return 'disabled'
      }
      const key = this.buildKey(this.currentDateIndex, slot.id)
      const exists = this.selectedSlots.some(item => item.key === key)
      if (exists) {
        return 'selected'
      }
      return 'available'
    },

    slotClass (slot) {
      const state = this.slotState(slot)
      return {
        'slot-available': state === 'available',
        'slot-disabled': state === 'disabled',
        'slot-selected': state === 'selected'
      }
    },

    // 点击时段：选中 / 取消
    toggleSlot (slot) {
      if (this.slotState(slot) === 'disabled') {
        return
      }
      const key = this.buildKey(this.currentDateIndex, slot.id)
      const index = this.selectedSlots.findIndex(item => item.key === key)
      if (index !== -1) {
        this.selectedSlots.splice(index, 1)
        return
      }
      if (this.selectedSlots.length >= 4) {
        alert('最多只能选择 4 个预约时段')
        return
      }
      const dateInfo = this.dateList[this.currentDateIndex]
      const dateLabel = dateInfo.fullLabel
      this.selectedSlots.push({
        key,
        dateIndex: this.currentDateIndex,
        dateLabel,
        slot
      })
    },

    removeSelected (key) {
      const index = this.selectedSlots.findIndex(item => item.key === key)
      if (index !== -1) {
        this.selectedSlots.splice(index, 1)
      }
    },

    resetSelections () {
      this.selectedSlots = []
    },

    // 提交预约：调用后端 /reservation/create
    async submitReservations () {
      if (!this.selectedSlots.length) return

      if (!this.currentUserId) {
        alert('请先登录后再预约')
        return
      }

      const slotIds = this.selectedSlots.map(item => Number(item.slot.id))

      try {
        const resp = await this.$axios.post('/reservation/create', {
          userId: this.currentUserId, // 先直接传，之后可以改成后端从登录态获取
          roomId: this.currentRoomId,
          date: this.currentDateStr,
          slotIds
        })

        const result = resp.data
        if (result.code && result.code !== 200) {
          alert(result.msg || '预约失败')
          return
        }

        alert('预约成功')
        // 清空选择
        this.selectedSlots = []
        // 重新拉一遍时段状态，刷新灰色/可选状态
        await this.fetchSlotStatus()
        // 保留原来的行为：预约后跳到“我的预约”
        this.emitChange('user-reservations')
      } catch (e) {
        console.error(e)
        alert('预约失败，服务器异常')
      }
    },

    // 从后端拉取某房间某天的“已满时段”
    async fetchSlotStatus () {
      // 房间或日期还没准备好就先不调
      if (!this.currentRoomId || !this.currentDateStr) return

      try {
        const resp = await this.$axios.get('/reservation/slots', {
          params: {
            roomId: this.currentRoomId,
            date: this.currentDateStr
          }
        })

        const result = resp.data
        const dto = result.data || {}
        // 后端返回的是 [8,9,14] 这样，我们前端用字符串 id
        this.disabledSlotIds = (dto.disabledSlotIds || []).map(id => String(id))
      } catch (e) {
        console.error(e)
        alert('获取时段状态失败')
      }
    },

    // 把后端返回的预约列表加载到表格
    async loadMyReservations () {
      // 确保有 currentUserId，没有的话再尝试从 localStorage 读一次
      if (!this.currentUserId) {
        const raw = localStorage.getItem('ssrmsUser')
        if (raw) {
          try {
            const user = JSON.parse(raw)
            this.currentUserId = user.id   // 如果主键叫 userId，就改成 user.userId
          } catch (e) {
            console.error(e)
          }
        }
      }

      if (!this.currentUserId) return

      // ⭐ 关键：先让后端刷新一次“未签到”状态
      await this.refreshNoShowStatus()

      try {
        const resp = await this.$axios.get('/reservation/my', {
          params: { userId: this.currentUserId }
        })
        const result = resp.data
        const list = Array.isArray(result.data) ? result.data.slice() : []

        // 日期升序，同一天按开始时间升序
        list.sort((a, b) => {
          const da = a.date || ''
          const db = b.date || ''
          if (da !== db) {
            // 日期升序
            return da.localeCompare(db)
          }
          const ta = a.startTime || ''
          const tb = b.startTime || ''
          // 同一天时间升序
          return ta.localeCompare(tb)
        })

        this.myReservations = list
      } catch (e) {
        console.error(e)
        alert('加载预约列表失败')
      }
    },

    // 把 startTime / endTime 拼成 “HH:mm-HH:mm”
    formatTimeRange (item) {
      const s = (item.startTime || '').slice(0, 5)
      const e = (item.endTime || '').slice(0, 5)
      return s && e ? `${s}-${e}` : ''
    },

    // 把后端状态英文转成中文文案
    renderStatusText (status) {
      switch (status) {
        case 'reserved':
          return '待签到'
        case 'checked_in':
          return '已签到'
        case 'late':
          return '迟到'
        case 'no_show':
          return '未签到'
        case 'cancelled':
          return '已取消'
        case 'cancel_overdue':
          return '逾期取消'
        default:
          return status || ''
      }
    },

    // 根据状态决定徽标颜色（先留好钩子，样式后面想美化再改）
    statusClass (status) {
      return {
        'badge-pending': status === 'reserved',
        'badge-done': status === 'checked_in',
        'badge-late': status === 'late',
        'badge-missed': status === 'no_show',
        'badge-cancelled': status === 'cancelled',
        'badge-cancel-overdue': status === 'cancel_overdue'
      }
    },

    gotoPrevPage () {
      if (this.reservationPageIndex > 1) {
        this.reservationPageIndex--
      }
    },

    gotoNextPage () {
      if (this.reservationPageIndex < this.totalPages) {
        this.reservationPageIndex++
      }
    },

    // 签到
    async handleCheckIn (item) {
      try {
        const resp = await this.$axios.post(`/reservation/checkin/${item.id}`)
        const result = resp.data
        if (result.code && result.code !== 200) {
          alert(result.msg || '签到失败')
          return
        }
        alert(result.msg || '签到成功')
        // 重新刷新列表和占用情况
        await this.loadMyReservations()
        // 如果你希望“我要预约”那边也立即刷新占用，可以顺带：
        // await this.fetchSlotStatus()
      } catch (e) {
        console.error(e)
        alert('签到失败，服务器异常')
      }
    },

    // 取消预约
    async handleCancel (item) {
      // 前端再保险一次 10 分钟规则
      if (!this.canCancel(item)) {
        alert('距离开始不足 10 分钟，无法取消，请联系管理员处理')
        return
      }

      const ok = window.confirm(`确定要取消本次预约（${item.date} ${this.formatTimeRange(item)}）吗？`)
      if (!ok) {
        return
      }

      try {
        const resp = await this.$axios.post(`/reservation/cancel/${item.id}`)
        const result = resp.data || {}

        // 这里按你的 Result 约定来，如果后端是 code === 200 代表成功就保留这一句
        if (result.code && result.code !== 200) {
          alert(result.msg || '取消失败')
          return
        }

        // 后端会根据规则把状态改成 cancelled 或 cancel_overdue
        alert(result.msg || '取消成功')

        // 重新加载预约列表
        await this.loadMyReservations()
        // 释放座位后，顺便刷新一下“我要预约”页面的占用情况
        await this.fetchSlotStatus()
      } catch (e) {
        console.error(e)
        alert('取消失败，服务器异常')
      }
    },

    // 是否允许取消：只拦“开始前 10 分钟”的情况
    canCancel (item) {
      if (item.status !== 'reserved') return false
      if (!item.date || !item.startTime) return false

      try {
        // date 例如 '2025-12-22'，startTime 例如 '01:00:00'
        const startStr = item.date + 'T' + (item.startTime || '').slice(0, 8)
        const start = new Date(startStr)
        if (isNaN(start.getTime())) {
          // 解析失败就不在前端拦，交给后端判断
          return true
        }
        const now = new Date()
        const diffMs = start.getTime() - now.getTime()
        const tenMinutes = 10 * 60 * 1000

        // diffMs > 10 分钟 ⇒ 可以取消
        return diffMs > tenMinutes
      } catch (e) {
        console.error(e)
        // 出异常直接放行，交给后端
        return true
      }
    },
    // 进入“我的预约”前，先让后台把已过期的预约批量标记为未签到
    async refreshNoShowStatus () {
      // 确保 currentUserId 有值
      if (!this.currentUserId) {
        const raw = localStorage.getItem('ssrmsUser')
        if (raw) {
          try {
            const user = JSON.parse(raw)
            this.currentUserId = user.id   // 如果你用的是 userId，就改成 user.userId
          } catch (e) {
            console.error(e)
          }
        }
      }

      if (!this.currentUserId) return

      try {
        const resp = await this.$axios.post('/reservation/refreshNoShow', null, {
          params: { userId: this.currentUserId }
        })
        const result = resp.data || {}
        const updated = typeof result.data === 'number' ? result.data : 0

        // 有新被标记为“未签到”的记录，再提醒一次
        if (updated > 0) {
          alert(`有 ${updated} 条已过期但未签到的预约，系统已自动标记为“未签到”，请留意信用分变化。`)
        }
      } catch (e) {
        console.error(e)
        // 这里不强制报错给用户，避免影响列表加载
      }
    },
    async loadMyViolations () {
      // 和 loadMyReservations 一样，先确保有 currentUserId
      if (!this.currentUserId) {
        const raw = localStorage.getItem('ssrmsUser')
        if (raw) {
          try {
            const user = JSON.parse(raw)
            this.currentUserId = user.id
          } catch (e) {
            console.error(e)
          }
        }
      }
      if (!this.currentUserId) return

      try {
        // 先让后端更新一次未签到状态
        await this.refreshNoShowStatus()

        const resp = await this.$axios.get('/reservation/violations', {
          params: { userId: this.currentUserId }
        })
        const result = resp.data
        const list = Array.isArray(result.data) ? result.data.slice() : []

        // 这里后端已经按日期倒序 + 时间升序排过了，也可以再按你想要的顺序排一次
        this.myViolations = list
        this.violationPageIndex = 1   // 每次加载回到第一页
      } catch (e) {
        console.error(e)
        alert('加载违规记录失败')
      }
    },

    gotoPrevViolationPage () {
      if (this.violationPageIndex > 1) {
        this.violationPageIndex--
      }
    },

    gotoNextViolationPage () {
      if (this.violationPageIndex < this.violationTotalPages) {
        this.violationPageIndex++
      }
    },

    // 加载个人信息
    async loadUserProfile () {
      // 先确保 currentUserId 有值
      if (!this.currentUserId) {
        const raw = localStorage.getItem('ssrmsUser')
        if (raw) {
          try {
            const user = JSON.parse(raw)
            // 登录时返回的就是 User，所以这里取 id 就行
            this.currentUserId = user.id
          } catch (e) {
            console.error(e)
          }
        }
      }

      if (!this.currentUserId) {
        console.warn('currentUserId 为空，无法加载个人信息')
        return
      }

      this.profileLoading = true
      try {
        // ✅ 和你后端一致：@RequestParam Integer userId
        const resp = await this.$axios.get('/user/profile', {
          params: { userId: this.currentUserId }
        })

        const result = resp.data || {}
        console.log('GET /user/profile 返回：', result)

        // 兼容两种写法：
        // 1）{ code, msg, data: { ...user } }
        // 2）直接就是 { id, name, ... }（万一以后你改 Result）
        const u = result.data || result
        if (!u || !u.id) {
          console.error('加载个人信息失败：', result.msg || '返回数据为空')
          return
        }

        // 和实体字段一一对应
        this.profileForm = {
          name: u.name || '',
          account: u.account || '',
          studentNo: u.studentNo || '',
          college: u.college || '',
          gradeClass: u.gradeClass || '',
          phone: u.phone || '',
          email: u.email || '',
          commonCampus: u.commonCampus || '',
          profileRemark: u.profileRemark || ''
        }
      } catch (e) {
        console.error('请求 /user/profile 失败', e)
      } finally {
        this.profileLoading = false
      }
    },

    // 保存个人信息
    async handleProfileSave () {
      if (!this.currentUserId) {
        alert('当前用户信息缺失，请重新登录后再试')
        return
      }

      this.profileSaving = true
      try {
        const payload = {
          id: this.currentUserId,
          name: this.profileForm.name,
          college: this.profileForm.college,
          gradeClass: this.profileForm.gradeClass,
          phone: this.profileForm.phone,
          email: this.profileForm.email,
          commonCampus: this.profileForm.commonCampus,
          profileRemark: this.profileForm.profileRemark,
          studentNo: this.profileForm.studentNo
        }

        const resp = await this.$axios.post('/user/profile', payload)
        const result = resp.data || {}

        if (result.code === 200) {
          alert('保存成功')

          // 顺便更新 localStorage 里的 ssrmsUser，让其他页面也用到最新信息
          const raw = localStorage.getItem('ssrmsUser')
          if (raw) {
            try {
              const user = JSON.parse(raw)
              Object.assign(user, payload)
              localStorage.setItem('ssrmsUser', JSON.stringify(user))
            } catch (e) {
              console.error(e)
            }
          }
        } else {
          alert(result.msg || '保存失败')
        }
      } catch (e) {
        console.error('请求 /user/profile 失败', e)
        alert('保存失败，请稍后重试')
      } finally {
        this.profileSaving = false
      }
    },

    // 右下角“随手一评”按钮
    submitFB (score) {
      // 这里只是演示：实际接入接口时，把 score、当前用户、时间等发给后端即可
      console.log('用户给了一个快捷评分：', score)
      this.openFeedback = false
    },

    // 把天气代码翻译成中文
    codeToDesc (code) {
      // open-meteo 的天气代码含义可以在官方文档里查，这里只列了一些常见的
      const map = {
        0: '晴',
        1: '多云',
        2: '多云',
        3: '阴',
        45: '有雾',
        48: '雾，地面结冰',
        51: '毛毛雨',
        53: '小雨',
        55: '中雨',
        61: '小雨',
        63: '中雨',
        65: '大雨',
        71: '小雪',
        73: '中雪',
        75: '大雪',
        95: '雷阵雨',
        96: '雷阵雨伴有冰雹'
      }
      return map[code] || '多云'
    },

    // 拉天气（示范接口：open-meteo，使用 fetch 即可）
    async loadWeather () {
      try {
        const resp = await fetch(
            'https://api.open-meteo.com/v1/forecast' +
            '?latitude=29.88&longitude=121.55' +
            '&current_weather=true'
        )
        const json = await resp.json()
        const cw = (json && json.current_weather) || {}
        const temp = cw.temperature
        const windSpeed = cw.windspeed
        const code = cw.weathercode
        const desc = this.codeToDesc(code)

        this.weatherData = {
          city: '宁波',
          temp: temp != null ? temp : 18,
          desc: desc || '多云',
          wind: windSpeed != null ? `风速 ${windSpeed} km/h` : '微风'
        }
      } catch (e) {
        console.warn('请求天气失败，使用兜底文案', e)
        this.weatherData = {
          city: '宁波',
          temp: 18,
          desc: '多云',
          wind: '微风'
        }
      }
    },

    refreshQuote () {
      // 没配置金句就直接返回
      if (!this.quotes || !this.quotes.length) return

      // 只有一句就没得换
      if (this.quotes.length === 1) {
        this.dailyQuote = this.quotes[0]
        return
      }

      // 尽量不要连刷出同一句
      let next = this.dailyQuote
      while (next === this.dailyQuote) {
        const idx = Math.floor(Math.random() * this.quotes.length)
        next = this.quotes[idx]
      }
      this.dailyQuote = next
    },

  },
  watch: {
    currentPage (newVal) {
      if (newVal === 'user-reservations') {
        this.loadMyReservations()
      }
      if (newVal === 'user-reserve') {
        this.fetchSlotStatus()
      }
      if (newVal === 'user-violations') {
        this.loadMyViolations()
      }
      if (newVal === 'user-profile') {
        this.loadUserProfile()
      }
    },
    myReservations () {
      this.reservationPageIndex = 1
    }
  },
}
</script>

<style scoped>
.main {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 通用卡片样式 */
.card {
  background-color: #ffffff;
  padding: 22px 24px;
  margin-bottom: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  border: 1px solid #e5e7eb;
}

.page-title {
  font-size: 24px;
  margin: 0 0 10px 0;
  color: #111827;
}

.page-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin-bottom: 16px;
}

/* 公共表格样式 */

.table-wrapper {
  margin-top: 12px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.table th {
  background-color: #f3f4f6;
  text-align: left;
  font-weight: 600;
  color: #374151;
}

.table tr:nth-child(even) td {
  background-color: #f9fafb;
}

.text-right {
  text-align: right;
}

.link-danger {
  color: #dc2626;
}

/* 信用分 */

.credit-summary {
  margin-top: 6px;
  margin-bottom: 10px;
}

.credit-score {
  font-size: 15px;
  color: #111827;
}

.score-number {
  font-size: 20px;
  font-weight: 600;
  color: #16a34a;
}

/* 评价与投诉 */

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 10px;
}

.feedback-column {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.textarea {
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 8px 10px;
  resize: vertical;
  font-size: 13px;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  background-color: #e0e7ff;
  color: #3730a3;
}

.hint-text {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
}

.reserve-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 顶部：标题 + 概要卡片 */
.reserve-header-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.reserve-header-left {
  flex: 1;
}

.reserve-summary-box {
  width: 260px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eef2ff, #f5f7ff);
  border: 1px solid #e0e7ff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
  font-size: 13px;
}

.summary-title {
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
}

.summary-label {
  color: #6b7280;
}

.summary-value {
  color: #111827;
  font-weight: 500;
  text-align: right;
}

.summary-empty {
  color: #9ca3af;
  font-weight: 400;
}

/* 场地 tabs + 开关 */
.reserve-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 2px;
}

.venue-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.venue-tab {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s ease;
}

.venue-tab:hover {
  border-color: #93c5fd;
  background-color: #eff6ff;
}

.venue-tab.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.25);
}

.reserve-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.switch-label {
  white-space: nowrap;
}

/* 日期条 */
.date-strip {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 14px;
  background-color: #f9fafb;
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.date-arrow {
  width: 32px;
  border-radius: 10px;
  border: none;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  font-size: 18px;
  color: #4b5563;
  cursor: pointer;
  flex-shrink: 0;
}

.date-arrow:disabled {
  opacity: 0.4;
  cursor: default;
  box-shadow: none;
}

.date-list {
  display: flex;
  gap: 8px;
  flex: 1;
}

.date-item {
  flex: 1;
  min-width: 90px;
  border-radius: 12px;
  border: 1px solid transparent;
  background-color: #fff;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
  height: 80px;
}

.date-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.06);
}

.date-item.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
}

.date-item.active .date-month,
.date-item.active .date-weekday,
.date-item.active .date-extra {
  color: #e0ecff;
}

.date-item.active .date-day-number {
  color: #fff;
}

.date-month {
  font-size: 11px;
  color: #6b7280;
}

.date-day-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 4px 0;
}

.date-day-number {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.date-weekday {
  font-size: 12px;
  color: #6b7280;
}

.date-extra {
  font-size: 11px;
  color: #2563eb;
}

/* 时间段区域 */

.slot-section {
  margin-top: 16px;
}

.slot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.slot-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.slot-legend {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #6b7280;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-available {
  background-color: #e5f3ff;
  border: 1px solid #93c5fd;
}

.legend-selected {
  background-color: #2563eb;
}

.legend-disabled {
  background-color: #e5e7eb;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.slot-item {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  background-color: #f9fafb;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  flex-direction: column; /* 纵向排布子元素 */
}

.slot-item:disabled {
  cursor: not-allowed;
}

.slot-time {
  font-size: 13px;
  margin-bottom: 4px;
}

.slot-range {
  font-size: 11px;
  color: #6b7280;
}

.slot-selected .slot-range {
  color: #e0ecff;
}

/* 已选时段 + 提交区域 */

.reserve-bottom {
  margin-top: 16px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 12px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background-color: #eff6ff;
  padding: 4px 8px 4px 10px;
  font-size: 11px;
  color: #1f2937;
}

.selected-tag-close {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: #6b7280;
}

.selected-tag-close:hover {
  color: #111827;
}

.reserve-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.link-btn {
  border: none;
  background: none;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
}

.link-btn:hover {
  color: #111827;
  text-decoration: underline;
}

.reserve-btn {
  padding: 8px 20px;
}

.primary-btn {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 18px;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.3);
  transition: all 0.15s ease;
}

.primary-btn:disabled {
  opacity: 0.4;
  cursor: default;
  box-shadow: none;
}

.primary-btn:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.38);
}

.pager {
  margin-top: 10px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #4b5563;
}

.pager-center {
  min-width: 120px;
  text-align: center;
}

.pager-btn {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  cursor: pointer;
  font-size: 13px;
  min-width: 72px;
}

.pager-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

/* 禁用状态的样式 */
.pager-btn-disabled,
.pager-btn:disabled {
  color: #9ca3af;
  border-color: #e5e7eb;
  background-color: #f9fafb;
  cursor: not-allowed;
}

/* 只调“我的预约”那一页的间距 */
.card-reservations .page-title {
  margin-bottom: 15px;   /* 标题到表格的上间距的一半来自这里 */
}

.card-reservations .table-wrapper {
  margin-top: 15px;      /* 标题到表格的另一半来自这里 */
  margin-bottom: 15px;   /* 表格到底下分页的间距的一半 */
}

.card-reservations .pager {
  margin-top: 15px;      /* 表格到底部分页的另一半 */
}

/* 外层卡片，让个人中心撑满右侧高度 */
.full-page-card {
  padding: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  /* 一般内容不多，也不需要滚动条了，可以改成 auto 或干脆删掉 */
  overflow-y: auto;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 顶部用户信息区域 */
.user-header-section {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  padding: 24px 32px;
  color: #ffffff;
  border-radius: 12px 12px 0 0;
}

.user-info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.user-info-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-profile-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #ffffff;
}

.user-role {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 右上角操作（预留“退出登录”按钮之类） */
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 主内容区域 */
.profile-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #ffffff;
}

/* 表单模块（个人信息 / 联系方式 / 偏好设置 / 备注） */
.form-section {
  background: #ffffff;
  border-radius: 8px;
  padding: 0;
  border-bottom: none;
}

.profile-content .section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.form-group {
  margin-top: 8px;
}

/* 两列栅格布局 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

/* 单个表单项 */
.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 文本输入框/下拉/文本域的通用样式，排除掉 radio */
.form-item input:not([type="radio"]),
.form-item select,
.form-item textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  color: #111827;
  background: #ffffff;
  transition: all 0.2s ease;
  outline: none;
  box-sizing: border-box;
}

/* 文本输入框获得焦点时的高亮（不作用于 radio） */
.form-item input:not([type="radio"]):focus,
.form-item select:focus,
.form-item textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-item input:read-only {
  background-color: #f9fafb;
  border-color: #e5e7eb;
  color: #6b7280;
  cursor: not-allowed;
}

/* 备注文本域占满一整行 */
.form-item.full-width {
  grid-column: 1 / -1;
}

/* 下拉框的小箭头（可以删掉这一段，也不影响功能） */
.form-item select {
  width: 100%;
  appearance: none;
  /* 这里原本有一长串 SVG 的 background-image，用来画下拉箭头。
     你可以先不写，有需要再补；不影响表单功能。 */
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
  padding-right: 36px;
  cursor: pointer;
}

/* 输入框 + 右侧“修改”按钮 */
.input-with-action {
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-with-action input {
  flex: 1;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 2px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 0;
}

.radio-option input[type="radio"] {
  width: 16px;
  height: 16px;
  accent-color: #3b82f6;
}

.radio-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.radio-option input[type="radio"]:checked + .radio-label {
  color: #3b82f6;
}

/* 备注文本域 */
.form-item textarea {
  min-height: 100px;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
  padding: 12px;
}

/* 操作按钮区域（底部的 取消 / 保存 + 提示） */
.profile-actions {
  margin-top: 4px;
  padding-top: 0;
  border-top: none;
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 只在个人中心底部使用的按钮样式 */
.profile-actions .primary-btn {
  padding: 10px 32px;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.profile-actions .primary-btn:hover {
  background: #2563eb;
}

/* 响应式适配：窄屏时压缩边距 & 按钮纵向排列 */
@media (max-width: 768px) {
  .user-header-section {
    padding: 20px;
  }

  .profile-content {
    padding: 20px;
  }

  .user-info-header {
    flex-direction: column;
    gap: 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .profile-actions .hint-text {
    text-align: center;
    margin-left: 0;
    margin-top: 8px;
    min-width: 100%;
  }
}

/* 首页外层容器，拉满右侧高度 */
.home-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 首页这张大卡片要撑满高度 */
.home-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

/* 首页上半部分：欢迎文案 */
.home-top {
  display: flex;
  align-items: stretch;
  gap: 24px;
  margin-bottom: 20px;
}

.home-top.home-head {
  position: relative;
}

.home-intro {
  flex: 1.3;
  padding-right: 150px;
}

.home-tagline {
  font-size: 13px;
  color: #4b5563;
  margin-top: 6px;
}

.home-tagline strong {
  color: #2563eb;
}

/* 今日概况卡片：统一为白色卡片背景 */
.home-panel {
  border-radius: 12px;
  padding: 16px 18px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  color: #111827;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.home-overview-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.home-overview-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.home-overview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.tag {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 999px;
  background-color: #eff6ff;
  color: #1d4ed8;
}

.tag-gray {
  background-color: #f3f4f6;
  color: #4b5563;
}

.tag-blue {
  background-color: #eff6ff;
  color: #2563eb;
}

.my-today-card .home-panel-body {
  padding-top: 8px;
}

.my-today-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.today-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.today-item:last-child {
  border-bottom: none;
}

.today-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.today-time {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.today-room {
  font-size: 12px;
  color: #6b7280;
}

.today-empty {
  font-size: 12px;
  color: #9ca3af;
  padding: 4px 0;
}

.notice-panel {
  margin-top: 4px;
}

.notice-more-btn {
  border: none;
  background: transparent;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
  padding: 2px 4px;
}

.notice-more-btn:hover {
  text-decoration: underline;
}

.notice-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 160px;
  overflow-y: auto;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.notice-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 13px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.home-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.home-panel-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.home-panel-number {
  font-size: 26px;
  font-weight: 600;
  margin: 6px 0 2px;
}

.home-panel-desc {
  font-size: 13px;
  opacity: 0.95;
}

.home-panel-footer {
  font-size: 12px;
  opacity: 0.88;
  margin-top: 8px;
}
.home-panel-footer span {
  font-weight: 500;
}

.weather-main{margin:6px 0}
.weather-icon{font-size: 28px}
.weather-temp{font-size: 22px;font-weight: 600;margin-left:6px}
.weather-desc{font-size: 12px;color:#9ca3af}

/* 月报 + 信用环 横向一排 */
.month-report{
  flex: 1;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(15,23,42,.04);
}
.report-title {
  font-size: 15px;      /* ⭐ 跟 .home-panel-title 一致 */
  font-weight: 600;
  margin-bottom: 10px;
  color: #111827;
}
.report-row{
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin: 6px 0;
}
.report-row strong{color:#2563eb}

/* 10. 快捷反馈浮层 */
.feedback-float{
  position: fixed;
  right: 24px;
  bottom: 80px;
  z-index: 999;
}
.feedback-btn{
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(37,99,235,.35);
}
.feedback-panel{
  position: absolute;
  bottom: 56px;
  right: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px 12px;
  box-shadow: 0 4px 12px rgba(15,23,42,.08);
  width: 140px;
}
.feedback-title{font-size: 12px;color:#6b7280;margin-bottom: 6px;text-align: center}
.feedback-emojis{display: flex;justify-content: space-around;font-size: 20px;cursor: pointer}

/* 新增 */
.weather-mini{
  position: absolute;
  top: 12px;
  right: 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  box-shadow: 0 2px 6px rgba(15,23,42,.06);
  width: 130px;
  z-index: 1;
}
.weather-mini .weather-main{ display: flex; align-items: center; justify-content: center; margin-bottom: 2px; }
.weather-mini .weather-icon{ font-size: 20px; }
.weather-mini .weather-temp{ font-size: 16px; font-weight: 600; margin-left: 6px; }
.weather-mini .weather-desc{ font-size: 11px; color: #6b7280; text-align: center; white-space: nowrap; }

/* 首页中部主布局：按行排列 */
.home-main-grid {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}

/* 通用一行容器（整行 1 列） */
.home-row {
  width: 100%;
}

/* 第二行：今日自习室概况 + 本月学习简报 */
.home-row-two {
  display: grid;
  grid-template-columns: 1.4fr 2fr;   /* ⭐ 左右 1:1 等宽 */
  gap: 12px;
}

/* 让第二行两块卡片同高 */
.home-row-two .home-overview,
.home-row-two .month-report {
  height: 100%;
}

/* 窄屏下：概况和月报上下堆叠 */
@media (max-width: 1024px) {
  .home-row-two {
    grid-template-columns: 1fr;
  }
}

/* 今日提示整块卡片 */
.quote-card {
  position: relative;              /* 为右上角按钮提供定位参照 */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: #f9fafb;
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.quote-refresh-btn {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.quote-refresh-btn:hover {
  background: #eff6ff;
  color: #2563eb;
}

.quote-content {
  max-width: 640px;          /* 控制一下宽度，避免太长一行 */
}

/* 上面一行：图标 + “今日提示” */
.quote-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 6px;
}

.quote-icon-inline {
  font-size: 24px;
}

.quote-label {
  font-size: 16px;
  color: #9ca3af;
}

/* 下面一句话内容，字体调大一点 */
.quote-text {
  font-size: 17px;           /* ⭐ 比之前更大一点 */
  color: #4b5563;
  line-height: 1.6;
}

</style>