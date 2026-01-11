<template>
  <div class="ai-lite-root">
    <!-- 打开时的“弹出”脉冲动画（从悬浮球位置扩散） -->
    <div
        v-if="pulseVisible"
        class="ai-pulse"
        :style="{ left: pulseX + 'px', top: pulseY + 'px' }"
    ></div>

    <!-- 悬浮球（长按可拖动；松手贴边吸附） -->
    <div
        v-show="!drawerVisible"
        class="ai-float"
        :class="{ 'is-dragging': dragging }"
        :style="floatStyle"
        @pointerdown="onPointerDown"
    >
      <div class="ai-float-inner" :class="{ dragging: dragging }" title="AI 自习室助手（长按拖动）">
        <div class="ai-float-emoji">🤖</div>
        <div class="ai-float-dot" v-if="hasUnreadHint"></div>
      </div>
    </div>

    <!-- 抽屉聊天窗 -->
    <el-drawer
        v-model="drawerVisible"
        :with-header="false"
        size="440px"
        class="ai-drawer"
        :append-to-body="true"
        :modal="true"
        :lock-scroll="true"
    >
      <div class="ai-shell" :class="{ 'is-opening': openingAnim }">
        <div class="ai-header">
          <div class="ai-brand">
            <div class="ai-brand-icon">🤖</div>
            <div class="ai-brand-text">
              <div class="ai-title">AI 自习室助手</div>
              <div class="ai-subtitle">预约 / 签到 / 信用规则 · 轻量版</div>
            </div>
          </div>

          <div class="ai-actions">
            <el-button size="small" @click="clearChat" :disabled="sending">清空</el-button>
            <el-button size="small" @click="drawerVisible = false">关闭</el-button>
          </div>
        </div>

        <div class="ai-quick" v-if="showQuick">
          <div class="ai-quick-row">
            <div class="ai-quick-label">快捷提问</div>
            <div class="ai-quick-items">
              <el-tag
                  v-for="q in quickQuestions"
                  :key="q"
                  class="ai-chip"
                  effect="plain"
                  @click="useQuick(q)"
              >
                {{ q }}
              </el-tag>
            </div>
          </div>
        </div>

        <el-scrollbar class="ai-body">
          <div class="ai-msgs">
            <div v-if="messages.length === 0" class="ai-empty">
              <div class="ai-empty-title">今天也要好好学习（但别把自己学成异常值）</div>
              <div class="ai-empty-desc">
                你可以问：怎么预约？我今天有哪些预约？为什么变预警/黑名单？
              </div>
            </div>

            <div
                v-for="m in messages"
                :key="m.id"
                :class="['ai-msg', m.role === 'user' ? 'is-user' : 'is-ai']"
            >
              <div class="ai-avatar">
                <span v-if="m.role === 'user'">你</span>
                <span v-else>AI</span>
              </div>

              <div class="ai-bubble">
                <div class="ai-text" v-text="m.text"></div>
                <div class="ai-meta">{{ formatTime(m.ts) }}</div>
              </div>
            </div>

            <div v-if="sending" class="ai-msg is-ai">
              <div class="ai-avatar">AI</div>
              <div class="ai-bubble">
                <div class="ai-text">正在思考…</div>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <div class="ai-input">
          <el-input
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入问题，例如：怎么预约？我今天有哪些预约？"
              @keydown.enter.exact.prevent="send"
              @keydown.enter.shift.exact.stop
          />
          <div class="ai-sendbar">
            <div class="ai-hint">Enter 发送 · Shift+Enter 换行 · 长按悬浮球可拖动</div>
            <el-button
                class="ai-send-btn"
                type="primary"
                :loading="sending"
                :disabled="sending || !inputText.trim()"
                @click="send"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'

export default {
  name: 'AiAssistantLite',
  props: {
    userId: {
      type: [Number, String],
      default: null
    }
  },
  data () {
    return {
      // drawer
      drawerVisible: false,
      openingAnim: false,

      // pulse anim
      pulseVisible: false,
      pulseX: 0,
      pulseY: 0,
      pulseTimer: null,

      // chat
      inputText: '',
      sending: false,
      conversationId: '',
      messages: [],

      // quick prompts
      showQuick: true,
      quickQuestions: [
        '怎么预约座位？',
        '我今天有哪些预约？',
        '为什么我变成预警/黑名单？',
        '如何签到/签退？'
      ],

      // floating ball drag
      floatX: null,
      floatY: null,
      dragging: false,
      longPressTimer: null,
      longPressMs: 260,
      startClientX: 0,
      startClientY: 0,
      startFloatX: 0,
      startFloatY: 0,
      pointerId: null,

      // 在窗口上监听，防止移动过快“丢事件”
      windowListening: false,

      hasUnreadHint: false
    }
  },
  computed: {
    resolvedUserId () {
      const p = this.userId
      if (p !== null && p !== undefined && String(p).trim() !== '') return Number(p)

      try {
        const raw = localStorage.getItem('ssrmsUser') || sessionStorage.getItem('ssrmsUser')
        if (!raw) return null
        const u = JSON.parse(raw)
        const id = u.userId ?? u.id ?? null
        return id === null ? null : Number(id)
      } catch (e) {
        return null
      }
    },
    storageKey () {
      return `ssrms_ai_lite_${this.resolvedUserId ?? 'guest'}`
    },
    floatPosKey () {
      return `ssrms_ai_float_pos_${this.resolvedUserId ?? 'guest'}`
    },
    floatStyle () {
      const size = 56
      const pad = 8
      const x = (this.floatX === null || this.floatX === undefined)
          ? (this.getViewportW() - pad - size)
          : this.floatX
      const y = (this.floatY === null || this.floatY === undefined)
          ? (this.getViewportH() - pad - size)
          : this.floatY

      return { left: `${x}px`, top: `${y}px` }
    }
  },
  mounted () {
    this.loadFromStorage()
    this.restoreFloatPos()
    window.addEventListener('resize', this.clampFloatToViewport)
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.clampFloatToViewport)
    this.removeWindowListeners()
    if (this.pulseTimer) clearTimeout(this.pulseTimer)
  },
  watch: {
    drawerVisible (v) {
      if (v) {
        this.hasUnreadHint = false
        this.$nextTick(() => this.scrollToBottom())
      }
    }
  },
  methods: {
    // ===== 关键：用 clientWidth/clientHeight，避免右侧滚动条导致“贴边被裁” =====
    getViewportW () {
      return document.documentElement?.clientWidth || this.getViewportW() || 0
    },
    getViewportH () {
      return document.documentElement?.clientHeight || this.getViewportH() || 0
    },

    // =========================
    // Floating ball: drag + snap
    // =========================
    restoreFloatPos () {
      try {
        const raw = localStorage.getItem(this.floatPosKey)
        if (!raw) return
        const obj = JSON.parse(raw)
        if (typeof obj.x === 'number' && typeof obj.y === 'number') {
          this.floatX = obj.x
          this.floatY = obj.y
          this.clampFloatToViewport()
        }
      } catch (e) {
        // 本地存储可能不可用，忽略即可
      }
    },
    saveFloatPos () {
      try {
        localStorage.setItem(this.floatPosKey, JSON.stringify({ x: this.floatX, y: this.floatY }))
      } catch (e) {
        // 本地存储可能不可用，忽略即可
      }
    },
    clampFloatToViewport () {
      const size = 56
      const pad = 8
      const maxX = Math.max(pad, this.getViewportW() - size - pad)
      const maxY = Math.max(pad, this.getViewportH() - size - pad)

      if (this.floatX === null || this.floatX === undefined) this.floatX = maxX
      if (this.floatY === null || this.floatY === undefined) this.floatY = maxY

      this.floatX = Math.min(Math.max(this.floatX, pad), maxX)
      this.floatY = Math.min(Math.max(this.floatY, pad), maxY)
    },
    snapToEdge () {
      const size = 56
      const pad = 8
      const maxX = Math.max(pad, this.getViewportW() - size - pad)
      const centerX = (this.floatX ?? 0) + size / 2
      const targetX = centerX < this.getViewportW() / 2 ? pad : maxX
      this.floatX = targetX
      this.clampFloatToViewport()
      this.saveFloatPos()
    },
    addWindowListeners () {
      if (this.windowListening) return
      this.windowListening = true
      window.addEventListener('pointermove', this.onWindowPointerMove, { passive: false })
      window.addEventListener('pointerup', this.onWindowPointerUp, { passive: true })
      window.addEventListener('pointercancel', this.onWindowPointerUp, { passive: true })
    },
    removeWindowListeners () {
      if (!this.windowListening) return
      this.windowListening = false
      window.removeEventListener('pointermove', this.onWindowPointerMove)
      window.removeEventListener('pointerup', this.onWindowPointerUp)
      window.removeEventListener('pointercancel', this.onWindowPointerUp)
    },

    onPointerDown (e) {
      if (e.button !== undefined && e.button !== 0) return

      this.pointerId = e.pointerId
      this.startClientX = e.clientX
      this.startClientY = e.clientY

      // 用当前 DOM rect 作为起点，避免样式切换误差
      const rect = e.currentTarget.getBoundingClientRect()
      this.startFloatX = rect.left
      this.startFloatY = rect.top

      // 关键：立刻在 window 上监听，防止鼠标过快移动“离开元素后丢事件”
      this.addWindowListeners()

      // 尝试立即 capture（更稳）
      try {
        e.currentTarget.setPointerCapture(this.pointerId)
      } catch (err) {
        // 某些浏览器不支持 capture，忽略即可
      }

      // 长按触发拖动
      this.longPressTimer = setTimeout(() => {
        this.dragging = true
        this.hasUnreadHint = false
      }, this.longPressMs)

      // 阻止部分浏览器的拖拽/选中文本行为
      try { e.preventDefault() } catch (err) {
        //
      }
    },

    onWindowPointerMove (e) {
      if (this.pointerId === null || e.pointerId !== this.pointerId) return

      const dx0 = e.clientX - this.startClientX
      const dy0 = e.clientY - this.startClientY
      const movedFar = Math.hypot(dx0, dy0) > 10

      // 快速拖动：用户明显移动时，不必等长按时间，直接进入拖动模式
      if (!this.dragging && movedFar) {
        if (this.longPressTimer) {
          clearTimeout(this.longPressTimer)
          this.longPressTimer = null
        }
        this.dragging = true
      }

      if (!this.dragging) return

      const dx = e.clientX - this.startClientX
      const dy = e.clientY - this.startClientY
      this.floatX = this.startFloatX + dx
      this.floatY = this.startFloatY + dy
      this.clampFloatToViewport()

      // 防止页面在移动时滚动（触摸设备）
      try { e.preventDefault() } catch (err) {
        //
      }
    },

    onWindowPointerUp (e) {
      if (this.pointerId === null || (e && e.pointerId !== this.pointerId)) return

      if (this.longPressTimer) {
        clearTimeout(this.longPressTimer)
        this.longPressTimer = null
      }

      // 拖动结束：贴边吸附 + 保存
      if (this.dragging) {
        this.dragging = false
        this.pointerId = null
        this.snapToEdge()
        this.removeWindowListeners()
        return
      }

      // 非拖动：视为点击打开
      this.pointerId = null
      this.removeWindowListeners()
      this.openDrawer()
    },

    // =========================
    // Drawer open animation
    // =========================
    playPulseFromFloat () {
      const size = 56
      const pad = 8
      const x = (this.floatX === null || this.floatX === undefined)
          ? (this.getViewportW() - pad - size)
          : this.floatX
      const y = (this.floatY === null || this.floatY === undefined)
          ? (this.getViewportH() - pad - size)
          : this.floatY

      this.pulseX = x + size / 2
      this.pulseY = y + size / 2
      this.pulseVisible = true

      if (this.pulseTimer) clearTimeout(this.pulseTimer)
      this.pulseTimer = setTimeout(() => {
        this.pulseVisible = false
      }, 420)
    },

    openDrawer () {
      this.playPulseFromFloat()

      this.drawerVisible = true
      this.openingAnim = true
      setTimeout(() => { this.openingAnim = false }, 220)

      this.loadFromStorage()
      this.$nextTick(() => this.scrollToBottom())
    },

    // =========================
    // Chat
    // =========================
    loadFromStorage () {
      try {
        const raw = localStorage.getItem(this.storageKey)
        if (!raw) {
          this.messages = []
          this.conversationId = ''
          return
        }
        const obj = JSON.parse(raw)
        this.messages = Array.isArray(obj.messages) ? obj.messages : []
        this.conversationId = obj.conversationId || ''
      } catch (e) {
        this.messages = []
        this.conversationId = ''
      }
    },
    saveToStorage () {
      try {
        localStorage.setItem(this.storageKey, JSON.stringify({
          conversationId: this.conversationId || '',
          messages: this.messages || []
        }))
      } catch (e) {
        // 本地存储可能被禁用/空间不足，忽略即可
      }
    },
    clearChat () {
      this.messages = []
      this.conversationId = ''
      this.saveToStorage()
      ElMessage.success('已清空对话')
      this.$nextTick(() => this.scrollToBottom())
    },
    useQuick (q) {
      if (this.sending) return
      this.inputText = q
    },
    pushMsg (role, text) {
      const msg = {
        id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
        role,
        text: String(text ?? ''),
        ts: Date.now()
      }
      this.messages.push(msg)
      this.saveToStorage()
      this.$nextTick(() => this.scrollToBottom())
    },
    formatTime (ts) {
      try {
        const d = new Date(ts)
        const hh = String(d.getHours()).padStart(2, '0')
        const mm = String(d.getMinutes()).padStart(2, '0')
        return `${hh}:${mm}`
      } catch (e) {
        return ''
      }
    },
    scrollToBottom () {
      try {
        const el = this.$el.querySelector('.ai-body .el-scrollbar__wrap')
        if (el) el.scrollTop = el.scrollHeight
      } catch (e) {
        // DOM 容器可能暂未渲染或不存在，忽略即可
      }
    },
    async send () {
      const text = this.inputText.trim()
      if (!text || this.sending) return

      if (!this.resolvedUserId) {
        ElMessage.warning('未获取到用户信息，请先登录后再使用 AI 助手')
        return
      }

      this.pushMsg('user', text)
      this.inputText = ''
      this.sending = true

      try {
        const payload = {
          userId: this.resolvedUserId,
          message: text,
          conversationId: this.conversationId || null
        }

        // 兼容：你的 request.js 可能已经 return response.data
        const res = await this.$axios.post('/ai/chat', payload)
        const body = (res && typeof res.code !== 'undefined')
            ? res
            : (res && res.data ? res.data : null)

        if (!body || Number(body.code) !== 200) {
          const msg = (body && (body.msg || body.message)) ? (body.msg || body.message) : 'AI 请求失败'
          throw new Error(msg)
        }

        const data = body.data || {}
        if (data.conversationId) this.conversationId = data.conversationId
        this.pushMsg('assistant', data.reply || '（AI 未返回内容）')
      } catch (e) {
        this.pushMsg('assistant', `（出错了）${e?.message || 'AI 请求失败'}`)
      } finally {
        this.sending = false
        this.saveToStorage()
        this.$nextTick(() => this.scrollToBottom())
      }
    }
  }
}
</script>

<style scoped>
.ai-lite-root {}

/* ===== 打开时脉冲动画（从悬浮球位置扩散） ===== */
.ai-pulse {
  position: fixed;
  z-index: 5000;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  transform: translate(-50%, -50%);
  background: rgba(64, 158, 255, 0.22);
  box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.24);
  pointer-events: none;
  animation: aiPulse 420ms ease-out forwards;
}

@keyframes aiPulse {
  0% {
    opacity: 0.9;
    transform: translate(-50%, -50%) scale(0.6);
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.30);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(10);
    box-shadow: 0 0 0 26px rgba(64, 158, 255, 0);
  }
}

/* ===== 悬浮球（带贴边吸附过渡） ===== */
.ai-float {
  position: fixed;
  z-index: 4000;
  width: 56px;
  height: 56px;
  transition: left 180ms cubic-bezier(0.2, 0.9, 0.2, 1), top 180ms cubic-bezier(0.2, 0.9, 0.2, 1);
}
.ai-float.is-dragging {
  transition: none;
}

.ai-float-inner {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(64,158,255,1) 0%, rgba(64,158,255,0.86) 100%);
  box-shadow: 0 14px 26px rgba(0,0,0,0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  /* 关键：触摸设备拖动更稳，不触发滚动/缩放手势 */
  touch-action: none;
}
.ai-float-inner:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(0,0,0,0.24);
}
.ai-float-inner.dragging {
  transform: scale(1.06);
  box-shadow: 0 18px 34px rgba(0,0,0,0.28);
  cursor: grabbing;
}
.ai-float-emoji {
  font-size: 22px;
  line-height: 1;
}
.ai-float-dot {
  position: absolute;
  right: 6px;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.9);
}

/* ===== 抽屉整体 ===== */
.ai-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f6f7fb;
}

.ai-shell.is-opening {
  animation: aiOpen 220ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes aiOpen {
  0% {
    opacity: 0;
    transform: translateX(12px) scale(0.985);
  }
  100% {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

.ai-header {
  padding: 14px 14px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  background: #ffffff;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.ai-brand { display: flex; gap: 10px; align-items: center; }
.ai-brand-icon {
  width: 36px; height: 36px; border-radius: 12px;
  background: rgba(64,158,255,0.12);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
}
.ai-title { font-weight: 800; font-size: 16px; line-height: 1.1; }
.ai-subtitle { margin-top: 4px; font-size: 12px; color: rgba(0,0,0,0.55); }
.ai-actions { display: flex; gap: 8px; flex-shrink: 0; align-items: flex-start; }

/* ===== 快捷提问 ===== */
.ai-quick {
  padding: 10px 14px;
  background: #ffffff;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.ai-quick-row { display: flex; align-items: flex-start; gap: 10px; }
.ai-quick-label { font-size: 12px; color: rgba(0,0,0,0.55); padding-top: 3px; flex-shrink: 0; }
.ai-quick-items { display: flex; flex-wrap: wrap; gap: 8px; }
.ai-chip { cursor: pointer; user-select: none; border-radius: 999px; }

/* ===== 消息区 ===== */
.ai-body { flex: 1; padding: 12px 14px; }
.ai-msgs { display: flex; flex-direction: column; gap: 12px; }
.ai-empty {
  padding: 14px 12px;
  border-radius: 14px;
  background: rgba(255,255,255,0.8);
  border: 1px dashed rgba(0,0,0,0.12);
}
.ai-empty-title { font-weight: 700; font-size: 14px; color: rgba(0,0,0,0.85); }
.ai-empty-desc { margin-top: 6px; font-size: 12px; color: rgba(0,0,0,0.55); line-height: 1.55; }

.ai-msg { display: flex; gap: 10px; align-items: flex-end; }
.ai-msg.is-user { flex-direction: row-reverse; }

.ai-avatar {
  width: 30px; height: 30px; border-radius: 10px;
  background: rgba(0,0,0,0.06);
  color: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; flex-shrink: 0;
}
.ai-msg.is-user .ai-avatar {
  background: rgba(64,158,255,0.18);
  color: rgba(64,158,255,1);
}

.ai-bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
  background: #ffffff;
  border: 1px solid rgba(0,0,0,0.04);
}
.ai-msg.is-user .ai-bubble {
  background: rgba(64,158,255,0.12);
  border: 1px solid rgba(64,158,255,0.10);
}

.ai-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(0,0,0,0.88);
}
.ai-meta {
  margin-top: 6px;
  font-size: 11px;
  color: rgba(0,0,0,0.45);
  text-align: right;
}

/* ===== 输入区 ===== */
.ai-input {
  padding: 12px 14px 14px;
  background: #ffffff;
  border-top: 1px solid rgba(0,0,0,0.06);
}
.ai-sendbar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.ai-hint { font-size: 12px; color: rgba(0,0,0,0.5); line-height: 1.4; }
.ai-send-btn { min-width: 84px; border-radius: 12px; }

/* Drawer body 去 padding */
:deep(.el-drawer__body) { padding: 0; }
</style>