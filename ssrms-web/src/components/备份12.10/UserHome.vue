<template>
  <div class="main">
    <!-- 首页 -->
    <div v-if="currentPage === 'home'" class="home-wrapper">
      <div class="card home-card">
        <!-- 上半部分：左文字 -->
        <div class="home-top">
          <div class="home-intro">
            <h2 class="page-title">欢迎使用自习室预约系统（学生端）</h2>
            <p class="page-subtitle">
              你可以在这里快速查看自习室开放情况、预约座位、查看自己的预约记录以及违规状态等。
            </p>
            <p class="home-tagline">
              建议提前 <strong>30 分钟</strong> 到达自习室完成签到，保持良好信用记录。
            </p>
          </div>
        </div>

        <!-- 今日概况 -->
        <div class="overview-wrapper">
          <div class="home-panel">
            <div>
              <div class="home-panel-title">今日自习室概况</div>
              <div class="home-panel-number">180 个座位</div>
              <div class="home-panel-desc">
                已预约 72 · 正在使用 58 · 剩余 50
              </div>
            </div>
            <div class="home-panel-footer">
              高峰时段：<span>19:00–21:30</span> · 301 自习室接近满座。
            </div>
          </div>
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
                @click="reserveVenueIndex = index"
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
            示例页面：点击“确认预约”后会弹出当前选择的时段，并跳转到“我的预约”；
            接入后端接口后即可真正完成预约。
          </p>
        </div>
      </div>
    </div>

    <!-- 我的预约 -->
    <div v-else-if="currentPage === 'user-reservations'">
      <div class="card">
        <h2 class="page-title">我的预约与签到签退</h2>
        <p class="page-subtitle">
          示意数据，实际系统可根据预约状态控制“签到 / 签退 / 取消”等按钮。
        </p>

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
            <tr>
              <td>Y20251204001</td>
              <td>本部 · 图书馆 3 楼 301</td>
              <td>2025-12-05</td>
              <td>08:00-10:00</td>
              <td>A-12</td>
              <td><span class="badge">待签到</span></td>
              <td class="text-right">
                <button class="link-btn">签到</button>
                <button class="link-btn link-danger">取消</button>
              </td>
            </tr>
            <tr>
              <td>Y20251128009</td>
              <td>本部 · 图书馆 4 楼 401</td>
              <td>2025-11-28</td>
              <td>19:00-21:30</td>
              <td>B-05</td>
              <td>已完成</td>
              <td class="text-right">
                <button class="link-btn">评价</button>
              </td>
            </tr>
            <tr>
              <td>Y20251101003</td>
              <td>东校区 · 教学楼 3 楼 305</td>
              <td>2025-11-01</td>
              <td>14:00-16:00</td>
              <td>C-18</td>
              <td>已取消</td>
              <td class="text-right">
                <button class="link-btn" disabled>不可操作</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 违规记录 -->
    <div v-else-if="currentPage === 'user-violations'">
      <div class="card">
        <h2 class="page-title">违规记录与信用分</h2>
        <p class="page-subtitle">
          信用分从 100 开始，迟到、缺席、早退等行为会扣分。以下为示意数据。
        </p>

        <div class="credit-summary">
          <div class="credit-score">
            当前信用分：<span class="score-number">96</span>
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
            <tr>
              <td>2025-11-30</td>
              <td>本部 · 图书馆 3 楼 301</td>
              <td>未签到</td>
              <td>-2</td>
              <td>预约后未按时签到</td>
            </tr>
            <tr>
              <td>2025-11-20</td>
              <td>本部 · 图书馆 4 楼 401</td>
              <td>早退</td>
              <td>-1</td>
              <td>提前 40 分钟离开</td>
            </tr>
            <tr>
              <td>2025-10-02</td>
              <td>东校区 · 教学楼 2 楼 203</td>
              <td>占座</td>
              <td>-1</td>
              <td>长时间离开座位未释放</td>
            </tr>
            </tbody>
          </table>
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
      <div class="card">
        <h2 class="page-title">个人中心</h2>
        <p class="page-subtitle">
          查看并维护个人基础信息，建议与学校学工系统保持一致，信息发生变动时及时更新。
        </p>

        <div class="profile-form">
          <div class="form-row">
            <div class="form-item">
              <label>姓名</label>
              <input type="text" value="张三" />
            </div>
            <div class="form-item">
              <label>学号</label>
              <input type="text" value="2023001234" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>学院</label>
              <input type="text" value="计算机学院" />
            </div>
            <div class="form-item">
              <label>年级与班级</label>
              <input type="text" value="大三 · 计科 3 班" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>手机号</label>
              <input type="text" value="138****1234" />
            </div>
            <div class="form-item">
              <label>邮箱</label>
              <input type="email" value="student@example.com" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>常用校区</label>
              <select>
                <option>本部</option>
                <option>东校区</option>
              </select>
            </div>
            <div class="form-item">
              <label>备注</label>
              <input type="text" placeholder="例如：偏好图书馆、晚上时段较多等" />
            </div>
          </div>

          <div class="profile-actions">
            <button class="primary-btn" type="button">保存信息</button>
            <span class="hint-text">
              示意按钮，接入接口后可更新数据库中的用户信息。
            </span>
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

      // 时间段
      timeSlots: [],
      // 示例：部分时间段不可预约
      disabledSlotIds: ['0', '1', '2', '3', '4', '5', '23'],

      // 已选时段（最多 4 个）
      selectedSlots: [],

      // 是否只展示“可预约”的时段
      onlyShowAvailable: false,
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
    }
  },
  created () {
    this.timeSlots = this.buildTimeSlots()
    this.initDates()
  },
  methods: {
    emitChange (page) {
      this.$emit('change-page', page)
    },

    // 构造 24 个时间段（0-1 点，1-2 点...）
    buildTimeSlots () {
      const list = []
      for (let h = 0; h < 24; h++) {
        const next = (h + 1) % 24
        const id = String(h)
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

    // 生成接下来 14 天的日期条
    initDates () {
      const today = new Date()
      const list = []
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      for (let i = 0; i < 14; i++) {
        const d = new Date(today)
        d.setDate(d.getDate() + i)
        const month = d.getMonth() + 1
        const day = d.getDate()
        const fullLabel =
            `${d.getFullYear()}-${month.toString().padStart(2, '0')}-` +
            `${day.toString().padStart(2, '0')}`
        list.push({
          key: i,
          year: d.getFullYear(),
          month,
          day,
          weekday: weekdays[d.getDay()],
          monthLabel: `${d.getFullYear()}-${month.toString().padStart(2, '0')}`,
          isToday: i === 0,
          isTomorrow: i === 1,
          fullLabel
        })
      }
      this.dateList = list
      this.visibleStart = 0
      this.currentDateIndex = 0
    },

    moveDates (direction) {
      if (direction === 'prev' && this.canMovePrev) {
        this.visibleStart -= 1
      } else if (direction === 'next' && this.canMoveNext) {
        this.visibleStart += 1
      }
    },

    selectDate (index) {
      this.currentDateIndex = index
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
      const key = this.buildKey(this.currentDateIndex, slot.id)
      const index = this.selectedSlots.findIndex(item => item.key === key)
      if (index !== -1) {
        this.selectedSlots.splice(index, 1)
        return
      }
      if (this.selectedSlots.length >= 4) {
        window.alert('最多只能选择 4 个预约时段')
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

    // 这里仍然保持“跳到我的预约页面”的原来逻辑
    submitReservations () {
      if (this.selectedSlots.length === 0) {
        window.alert('请先选择要预约的时段')
        return
      }
      const summary = this.selectedSlots
          .map(
              item =>
                  `${item.dateLabel} · ${this.currentVenueName} · ${item.slot.range}`
          )
          .join('\n')
      window.alert(
          '示例：这里会提交以下预约信息到后端（当前仅前端展示）：\n\n' + summary
      )
      // 保留原来的行为：预约后跳转到“我的预约”
      this.emitChange('user-reservations')
    },
    resetSelections () {
      this.selectedSlots = []
    },
  }
}
</script>

<style scoped>
.main {
  flex: 1;
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 首页外层容器，拉满右侧高度 */
.home-wrapper {
  flex: 1;
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

/* 首页这张大卡片要撑满高度 */
.home-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
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

/* 首页上半部分：欢迎文案 */
.home-top {
  display: flex;
  align-items: stretch;
  gap: 24px;
  margin-bottom: 20px;
}

.home-intro {
  flex: 1.3;
}

.overview-wrapper {
  margin-top: 8px;
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

.home-panel-title {
  font-size: 14px;
  opacity: 0.9;
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

/* 我要预约页面 */
.reserve-card {
  display: flex;
  flex-direction: column;
}

.reserve-header {
  margin-bottom: 16px;
}

.venue-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.venue-tab {
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  color: #4b5563;
}
.venue-tab.active {
  background-color: #eef2ff;
  border-color: #6366f1;
  color: #3730a3;
  font-weight: 600;
}

.date-strip {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.date-arrow {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
  width: 32px;
  font-size: 18px;
  cursor: pointer;
}
.date-arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.date-list {
  flex: 1;
  display: flex;
  overflow-x: auto;
  gap: 8px;
}

.date-item {
  min-width: 110px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  padding: 6px 10px;
  text-align: left;
  font-size: 12px;
  cursor: pointer;
  color: #4b5563;
}
.date-item.active {
  border-color: #6366f1;
  background-color: #eef2ff;
  color: #3730a3;
}

.date-month {
  font-size: 11px;
  opacity: 0.8;
}

.date-day-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.date-day-number {
  font-size: 18px;
  font-weight: 600;
}

.date-weekday {
  font-size: 12px;
}

.date-extra {
  font-size: 11px;
  color: #2563eb;
  margin-top: 2px;
}

.reserve-legend {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 12px;
  height: 6px;
  border-radius: 6px;
}

.legend-available {
  background-color: #bbf7d0;
}
.legend-disabled {
  background-color: #e5e7eb;
}
.legend-selected {
  background-color: #bfdbfe;
}

/* 时段卡片 */
.slot-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.slot-item {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  padding: 10px 8px;
  text-align: left;
  font-size: 12px;
  cursor: pointer;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.slot-time {
  font-size: 13px;
  margin-bottom: 4px;
}
.slot-range {
  font-size: 11px;
  color: #6b7280;
}

.slot-available {
  background-color: #f9fafb;
}

.slot-disabled {
  background-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.slot-selected {
  background-color: #4f46e5;
  color: #ffffff;
  border-color: #4338ca;
}
.slot-selected .slot-range {
  color: #e0e7ff;
}

.reserve-bottom {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 8px;
  background-color: #e0e7ff;
  padding: 6px 8px;
  font-size: 12px;
  color: #3730a3;
}

.selected-tag-main {
  margin-right: 4px;
}

.selected-tag-close {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: #4b5563;
}

.reserve-submit {
  display: flex;
  align-items: center;
  gap: 10px;
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

.link-btn {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
  margin-left: 6px;
}

.link-btn[disabled] {
  color: #9ca3af;
  cursor: not-allowed;
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

/* 个人中心 */

.profile-form {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.form-item {
  display: flex;
  flex-direction: column;
  font-size: 13px;
}

.form-item label {
  margin-bottom: 4px;
  color: #4b5563;
}

.form-item input,
.form-item select {
  border-radius: 8px;
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
}

.form-item input:focus,
.form-item select:focus {
  border-color: #2563eb;
}

.profile-actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 通用按钮和提示 */

.primary-btn {
  padding: 8px 18px;
  border-radius: 999px;
  border: none;
  outline: none;
  background-color: #2563eb;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

/* ===========================
   我要预约页面样式（美化）
   =========================== */

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
  margin-top: 4px;
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
  margin-top: 12px;
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

/* 状态色 */
.slot-available {
  background-color: #f9fafb;
}

.slot-disabled {
  background-color: #f3f4f6;
  color: #9ca3af;
  border-style: dashed;
}

.slot-selected {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.25);
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
</style>