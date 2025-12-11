<template>
  <div class="seat-manage">
    <!-- 顶部总览 -->
    <div class="card seat-top-card">
      <div class="seat-top-left">
        <h2 class="page-title">座位管理</h2>
        <p class="page-subtitle">
          按楼栋、自习室配置开放时间与座位布局，支持快速查看空余座位、禁用问题座位，以及设置预约规则。
        </p>
      </div>
      <div class="seat-top-right">
        <el-select
            v-model="currentCampus"
            size="small"
            class="top-select"
            placeholder="选择校区"
        >
          <el-option
              v-for="item in campusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>

        <el-select
            v-model="currentBuild"
            size="small"
            class="top-select"
            placeholder="选择楼栋"
            clearable
        >
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
      <!-- 左侧：自习室列表 -->
      <div class="card seat-left-card">
        <div class="left-header">
          <div class="left-header-main">
            <h3 class="left-title">自习室列表</h3>
            <p class="left-subtitle">按条件筛选自习室，查看容量与开放状态。</p>
          </div>
          <el-button size="small" type="primary" plain>
            新建自习室
          </el-button>
        </div>

        <!-- 筛选 -->
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

          <el-radio-group
              v-model="roomCapacityFilter"
              size="small"
              class="capacity-radio"
          >
            <el-radio-button label="all">全部容量</el-radio-button>
            <el-radio-button label="small">≤40 人</el-radio-button>
            <el-radio-button label="medium">40-80 人</el-radio-button>
            <el-radio-button label="large">≥80 人</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 自习室表格 -->
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
              <el-tag
                  size="small"
                  :type="scope.row.status === 'open' ? 'success' : 'info'"
              >
                {{ scope.row.status === 'open' ? '开放中' : '未开放' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="120" fixed="right">
            <template #default="scope">
              <el-button
                  type="primary"
                  link
                  size="small"
                  @click.stop="toggleRoomOpen(scope.row)"
              >
                {{ scope.row.status === 'open' ? '关闭自习室' : '开放自习室' }}
              </el-button>
              <el-button
                  type="info"
                  link
                  size="small"
                  @click.stop="handleEditRoom(scope.row)"
              >
                设置
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧：座位视图 + 规则配置 -->
      <div class="seat-right">
        <div class="card seat-right-card">
          <div class="right-header">
            <div>
              <h3 class="right-title">
                {{ selectedRoom ? selectedRoom.name : '请选择左侧自习室' }}
              </h3>
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
              <el-button size="small" plain>
                导出座位配置
              </el-button>
            </div>
          </div>

          <template v-if="selectedRoom">
            <!-- 小统计 -->
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
              <!-- 座位布局 -->
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
                    <el-button size="small" plain @click="handleBatchDisable">
                      批量禁用选中
                    </el-button>
                    <el-button size="small" plain @click="handleClearSelection">
                      清空选择
                    </el-button>
                  </div>
                </div>

                <!-- 座位网格 -->
                <div class="seat-legend">
                  <div class="legend-item">
                    <span class="legend-box legend-free" /> 可预约
                  </div>
                  <div class="legend-item">
                    <span class="legend-box legend-occupied" /> 已占用
                  </div>
                  <div class="legend-item">
                    <span class="legend-box legend-disabled" /> 禁用
                  </div>
                  <div class="legend-item">
                    <span class="legend-box legend-selected" /> 当前选择
                  </div>
                </div>

                <div class="seat-grid">
                  <div
                      v-for="row in seatGrid"
                      :key="row.rowIndex"
                      class="seat-row"
                  >
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

              <!-- 规则设置 -->
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
                    <el-input-number
                        v-model="seatRule.maxHoursPerOrder"
                        :min="1"
                        :max="8"
                    />
                    <span class="item-desc">小时</span>
                  </el-form-item>

                  <el-form-item label="每天最多预约次数">
                    <el-input-number
                        v-model="seatRule.maxOrdersPerDay"
                        :min="1"
                        :max="5"
                    />
                    <span class="item-desc">次/人/自习室</span>
                  </el-form-item>

                  <el-form-item label="自动释放未签到座位">
                    <div class="inline-group">
                      <el-switch v-model="seatRule.autoReleaseNoSign" />
                      <span class="item-desc">超过</span>
                      <el-input-number
                          v-model="seatRule.releaseAfterMinutes"
                          :min="5"
                          :max="60"
                      />
                      <span class="item-desc">分钟自动释放</span>
                    </div>
                  </el-form-item>

                  <el-form-item label="连续违约处理策略">
                    <el-select
                        v-model="seatRule.violationStrategy"
                        placeholder="选择策略"
                    >
                      <el-option
                          label="仅提醒，不限制"
                          value="tip"
                      />
                      <el-option
                          label="3 次违约当天禁止预约"
                          value="day-ban"
                      />
                      <el-option
                          label="连续 5 次违约一周内禁止预约"
                          value="week-ban"
                      />
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
                    <el-button size="small" @click="resetSeatRule">
                      重置为默认
                    </el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminSeatManage',
  data () {
    return {
      /* 顶部筛选 */
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

      /* 左侧自习室列表 */
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

      /* 右侧：座位布局 */
      activeTab: 'layout',
      showSeatNo: true,
      showDemoOccupied: true,
      seatGrid: [], // [{ rowIndex, seats: [{id,label,status,selected}]}]

      /* 规则设置 */
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
    filteredRooms () {
      return this.rooms.filter(room => {
        let ok = true
        if (this.currentCampus) {
          ok = ok && room.campus === this.currentCampus
        }
        if (this.currentBuild) {
          ok = ok && room.buildingCode === this.currentBuild
        }
        if (this.onlyShowOpen) {
          ok = ok && room.status === 'open'
        }
        if (this.roomKeyword && this.roomKeyword.trim()) {
          const k = this.roomKeyword.trim()
          ok =
              ok &&
              (room.name.includes(k) ||
                  String(room.floor).includes(k) ||
                  room.building.includes(k))
        }
        if (this.roomCapacityFilter === 'small') ok = ok && room.capacity <= 40
        if (this.roomCapacityFilter === 'medium') {
          ok = ok && room.capacity > 40 && room.capacity < 80
        }
        if (this.roomCapacityFilter === 'large') ok = ok && room.capacity >= 80
        return ok
      })
    },
    selectedRoom () {
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (!room) return null
      // 加一个 statusModel，用于右侧开关双向绑定（不影响左侧 status 字段）
      return {
        ...room,
        statusModel: room.status === 'open'
      }
    },
    seatStats () {
      const stats = {
        free: 0,
        occupied: 0,
        disabled: 0,
        blockedArea: 1 // 示例：假设有一个封锁区域
      }
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
  created () {
    this.initSeatGrid()
  },
  methods: {
    /* 初始化一个示例座位布局：6 行 x 8 列 */
    initSeatGrid () {
      const rows = 6
      const cols = 8
      const grid = []
      let id = 1
      for (let r = 0; r < rows; r++) {
        const row = { rowIndex: r, seats: [] }
        for (let c = 0; c < cols; c++) {
          const seat = {
            id: id++,
            row: r,
            col: c,
            label: `${r + 1}-${c + 1}`,
            status: 'free', // free / occupied / disabled
            selected: false
          }
          row.seats.push(seat)
        }
        grid.push(row)
      }

      // 做一点演示：随机几个已占用、禁用
      grid[0].seats[0].status = 'occupied'
      grid[0].seats[1].status = 'occupied'
      grid[2].seats[3].status = 'disabled'
      grid[3].seats[4].status = 'disabled'
      this.seatGrid = grid
    },

    handleSelectRoom (row) {
      this.selectedRoomId = row.id
      // 如果以后不同自习室要不同布局，可以在这里根据 id 重新加载 seatGrid
      console.log('选择自习室', row)
    },

    toggleRoomOpen (row) {
      row.status = row.status === 'open' ? 'closed' : 'open'
    },

    handleEditRoom (row) {
      console.log('编辑自习室配置（弹窗预留）', row)
    },

    syncRoomStatus (val) {
      // 右侧开关同步回 rooms 列表
      const room = this.rooms.find(r => r.id === this.selectedRoomId)
      if (room) {
        room.status = val ? 'open' : 'closed'
      }
    },

    seatCellClass (seat) {
      return [
        `seat-status-${seat.status}`,
        seat.selected ? 'seat-selected' : ''
      ]
    },

    handleSeatClick (seat) {
      // 点击时：优先选中/取消选中；按住 Ctrl 可切换禁用状态（这里只做简单示例）
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
        row.seats.forEach(seat => {
          seat.selected = false
        })
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
.seat-manage {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 公共卡片 */
.card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 18px 20px;
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

/* 顶部卡片 */
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

.top-select {
  min-width: 140px;
}

.top-switch {
  display: flex;
  align-items: center;
  gap: 6px;
}

.top-switch-label {
  font-size: 12px;
  color: #4b5563;
}

/* 主布局 */
.seat-layout {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

/* 左侧列表 */
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

.left-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.left-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.left-filter {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.left-filter-input {
  width: 100%;
}

.input-prefix-icon {
  font-size: 14px;
}

.capacity-radio {
  width: 100%;
}

/* 自习室表格 */
.room-table {
  width: 100%;
}

.room-name {
  display: flex;
  flex-direction: column;
}

.room-name-main {
  font-size: 13px;
  color: #111827;
}

.room-name-sub {
  font-size: 12px;
  color: #6b7280;
}

.room-capacity,
.room-used {
  font-size: 13px;
  color: #111827;
}

/* 右侧 */
.seat-right {
  flex: 1;
}

.seat-right-card {
  width: 100%;
}

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 16px;
}

.right-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.right-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.right-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 小统计卡片 */
.seat-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  min-width: 140px;
  border-radius: 12px;
  background-color: #f9fafb;
  padding: 8px 10px;
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

/* Tabs */
.seat-tabs {
  margin-top: 4px;
}

/* 布局工具条 */
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

/* 图例 */
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

.legend-free {
  background-color: #e0f2fe;
}

.legend-occupied {
  background-color: #fee2e2;
}

.legend-disabled {
  background-color: #e5e7eb;
}

.legend-selected {
  background-color: #ddd6fe;
}

/* 座位网格 */
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

.seat-row:last-child {
  margin-bottom: 0;
}

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

.seat-status-free {
  background-color: #e0f2fe;
  border: 1px solid #bfdbfe;
}

.seat-status-occupied {
  background-color: #fee2e2;
  border: 1px solid #fecaca;
}

.seat-status-disabled {
  background-color: #e5e7eb;
  border: 1px solid #d1d5db;
}

.seat-selected {
  box-shadow: 0 0 0 2px #a855f7;
}

.seat-no {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: #0f172a;
}

/* 规则配置表单 */
.rule-form {
  margin-top: 6px;
}

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

/* 分页等公共区域 */
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 响应式 */
@media (max-width: 980px) {
  .seat-layout {
    flex-direction: column;
  }

  .seat-left-card {
    flex: 1;
    max-width: 100%;
  }
}

@media (max-width: 780px) {
  .seat-top-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .seat-top-right {
    flex-wrap: wrap;
  }

  .right-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .layout-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
