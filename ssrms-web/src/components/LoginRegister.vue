<template>
  <div class="login-page">
    <div class="auth-wrapper">
      <!-- 左侧：登录/注册 -->
      <div class="auth-left">
        <div class="auth-logo-row">
          <div class="logo-icon">
            <span class="logo-initial">自</span>
          </div>
          <div class="logo-text">
            <div class="logo-title">自习室预约系统</div>
            <div class="logo-subtitle">Study Room Reservation</div>
          </div>
        </div>

        <div class="auth-card">
          <div class="mode-toggle">
            <button type="button" class="mode-btn" :class="{ active: isLogin }" @click="switchMode('login')">登录</button>
            <button type="button" class="mode-btn" :class="{ active: !isLogin }" @click="switchMode('register')">注册</button>
          </div>

          <!-- 登录 -->
          <form v-if="isLogin" class="auth-form" @submit.prevent="handleLogin">
            <div class="form-group">
              <label class="form-label">账号</label>
              <input v-model="loginForm.account" class="form-input" type="text" placeholder="请输入账号" />
            </div>

            <div class="form-group">
              <label class="form-label">密码</label>
              <input v-model="loginForm.password" class="form-input" type="password" placeholder="请输入密码" />
            </div>

            <!-- 登录身份选择：只在登录时出现 -->
            <div class="form-group form-inline">
              <label class="form-label-inline">登录身份</label>

              <label class="radio-label">
                <input type="radio" value="student" v-model="loginForm.role" />
                用户
              </label>

              <label class="radio-label">
                <input type="radio" value="admin" v-model="loginForm.role" />
                管理员
              </label>
            </div>

            <div class="form-helper">
              <label class="check-label">
                <input type="checkbox" v-model="loginForm.remember" />
                记住登录状态
              </label>
              <button type="button" class="link-btn" @click.prevent="handleForgot">忘记密码？</button>
            </div>

            <button type="submit" class="primary-btn">登录</button>

            <div class="bottom-tip">
              还没有账号？
              <button type="button" class="link-btn" @click="switchMode('register')">立即注册</button>
            </div>
          </form>

          <!-- 注册 -->
          <form v-else class="auth-form" @submit.prevent="handleRegister">
            <div class="form-group">
              <label class="form-label">账号</label>
              <input
                  v-model="registerForm.account"
                  class="form-input"
                  type="text"
                  placeholder="请输入账号"
                  @input="scheduleCheckAccount"
                  @blur="checkAccountExists"
              />
              <div v-if="registerErrors.account" class="field-error">{{ registerErrors.account }}</div>
            </div>

            <div class="form-group">
              <label class="form-label">学号</label>
              <input v-model="registerForm.studentNo" class="form-input" type="text" placeholder="请输入学号" />
            </div>

            <div class="form-group">
              <label class="form-label">昵称</label>
              <input v-model="registerForm.name" class="form-input" type="text" placeholder="请输入昵称" />
            </div>

            <div class="form-group">
              <label class="form-label">设置密码</label>
              <input
                  v-model="registerForm.password"
                  class="form-input"
                  type="password"
                  placeholder="至少 6 位，仅支持字母/数字/_"
                  @input="checkPasswordRules(); checkPasswordMatch()"
                  @blur="checkPasswordRules"
              />
              <div v-if="registerErrors.password" class="field-error">{{ registerErrors.password }}</div>
            </div>

            <div class="form-group">
              <label class="form-label">确认密码</label>
              <input
                  v-model="registerForm.confirmPassword"
                  class="form-input"
                  type="password"
                  placeholder="请再次输入密码"
                  @input="checkPasswordMatch"
              />
              <div v-if="registerErrors.confirmPassword" class="field-error">{{ registerErrors.confirmPassword }}</div>
            </div>

            <div class="form-helper" style="justify-content:flex-start;">
              <label class="check-label">
                <input type="checkbox" v-model="registerForm.agree" />
                已阅读并同意
                <button type="button" class="link-btn" @click.prevent="handleShowAgreement">《用户协议》</button>
              </label>
            </div>

            <button type="submit" class="primary-btn">注册</button>

            <div class="bottom-tip">
              已有账号？
              <button type="button" class="link-btn" @click="switchMode('login')">直接登录</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 右侧文案 -->
      <div class="auth-right">
        <div class="slogan-block">
          <h2 class="slogan-title">预约自习室的最佳通用解决方案</h2>
          <p class="slogan-desc">统一的自习室预约平台，让学生轻松选座，管理员高效管理场地和预约。</p>
          <ul class="slogan-list">
            <li>· 支持多校区、多楼栋、多自习室统一管理</li>
            <li>· 支持预约、签到、违约处理与信用分统计</li>
            <li>· 预留 API 接口，方便与学校现有系统对接</li>
          </ul>
          <div class="slogan-sign">—— SSRMS</div>
        </div>
      </div>

      <!-- 用户协议弹窗 -->
      <div v-if="showAgreement" class="agreement-mask" @click.self="closeAgreement">
        <div class="agreement-dialog">
          <div class="agreement-header">
            <h3 class="agreement-title">自习室预约系统用户协议</h3>
            <button class="agreement-close" type="button" @click="closeAgreement">×</button>
          </div>

          <div class="agreement-body">
            <div class="agreement-scroll">
              <p><b>1. 协议说明</b></p>
              <p>本协议适用于自习室预约系统（以下简称“本系统”）提供的账号注册、登录、预约、签到、取消、违约处理、信用分管理等服务。用户注册或使用本系统即视为已阅读并同意本协议。</p>

              <p><b>2. 账号与信息</b></p>
              <p>用户应提供真实、准确、完整的注册信息，并妥善保管账号与密码。因账号泄露或密码保管不当造成的损失由用户自行承担。</p>

              <p><b>3. 预约与签到规则</b></p>
              <p>用户应按预约时间到场并完成签到。若系统或管理员设置了签到时限、迟到规则、自动释放规则等，用户应遵守相应要求。自习室资源有限，请合理预约、按需取消。</p>

              <p><b>4. 违约与信用分</b></p>
              <p>出现以下情形可能被判定为违约：预约后未按规定时间签到、恶意占座、频繁占用资源影响他人等。系统可依据规则对用户进行提醒、扣减信用分、限制预约、加入黑名单等处理。</p>

              <p><b>5. 禁止行为</b></p>
              <p>用户不得利用本系统从事任何违法违规行为，不得恶意攻击系统、刷接口、伪造签到、干扰座位状态或影响其他用户正常使用。</p>

              <p><b>6. 数据与隐私</b></p>
              <p>本系统会在提供服务所必需的范围内收集并处理用户信息（如学号、姓名、预约记录、签到记录、违约记录等），用于账号管理与服务改进。未经授权，本系统不会向无关第三方泄露用户信息，但法律法规另有规定或司法/行政机关依法要求的除外。</p>

              <p><b>7. 免责声明</b></p>
              <p>因不可抗力、网络故障、设备故障、系统维护升级等原因导致服务中断或数据延迟，本系统将尽力修复与恢复，但不对因此造成的间接损失承担责任。</p>

              <p><b>8. 协议变更</b></p>
              <p>本系统有权根据运营需要更新本协议。更新后的协议将以公告、弹窗或页面提示等方式告知，用户继续使用即视为接受变更。</p>

              <p><b>9. 联系方式</b></p>
              <p>如对本协议或系统使用有疑问，请联系系统管理员或学校相关管理部门。</p>
            </div>
          </div>

          <div class="agreement-footer">
            <button type="button" class="primary-btn primary-btn-small" @click="agreeAndClose">
              我已阅读并同意
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'

export default {
  name: 'LoginRegister',
  data () {
    return {
      mode: 'login',

      loginForm: { account: '', password: '', role: 'student', remember: true },

      registerForm: {
        account: '',
        studentNo: '',
        name: '',
        password: '',
        confirmPassword: '',
        agree: false
      },

      showAgreement: false,

      registerErrors: {
        account: '',
        password: '',
        confirmPassword: ''
      },

      accountTimer: null
    }
  },
  computed: {
    isLogin () {
      return this.mode === 'login'
    }
  },
  methods: {
    toast (type, msg) {
      ElMessage({ type, message: msg })
    },

    switchMode (next) {
      this.mode = next
      this.registerErrors.account = ''
      this.registerErrors.password = ''
      this.registerErrors.confirmPassword = ''
      if (this.accountTimer) {
        clearTimeout(this.accountTimer)
        this.accountTimer = null
      }
    },

    handleForgot () {
      this.toast('info', '忘记密码功能暂未实现，请联系管理员重置密码')
    },

    handleShowAgreement () { this.showAgreement = true },
    closeAgreement () { this.showAgreement = false },
    agreeAndClose () {
      this.registerForm.agree = true
      this.showAgreement = false
    },

    async handleLogin () {
      const account = (this.loginForm.account || '').trim()
      const password = (this.loginForm.password || '').trim()
      const roleStr = this.loginForm.role || 'student'
      const roleId = roleStr === 'admin' ? 0 : 1

      if (!account || !password) {
        this.toast('warning', '请填写账号和密码')
        return
      }

      try {
        // request.js 已经 return response.data，所以这里 r 就是 Result
        const r = await this.$axios.post('/user/login', { account, password, roleId })

        if (!r || r.code !== 200) {
          this.toast('error', r?.msg || '登录失败')
          return
        }

        const user = r.data || {}

        const storage = this.loginForm.remember ? localStorage : sessionStorage
        const finalRoleId = (user.roleId !== undefined && user.roleId !== null) ? user.roleId : roleId
        storage.setItem('ssrmsUser', JSON.stringify({ ...user, roleId: finalRoleId }))

        this.toast('success', '登录成功')

        const redirect = this.$route?.query?.redirect
        if (redirect) this.$router.replace(redirect)
        else this.$router.replace(finalRoleId === 0 ? '/admin' : '/user')
      } catch (e) {
        const status = e?.response?.status
        const msg = e?.response?.data?.msg || e?.message
        this.toast('error', status ? `登录失败(${status})：${msg}` : `登录失败：${msg}`)
      }
    },

    async handleRegister () {
      await this.checkAccountExists()
      this.checkPasswordRules()
      this.checkPasswordMatch()

      if (this.registerErrors.account) return this.toast('error', this.registerErrors.account)
      if (this.registerErrors.password) return this.toast('error', this.registerErrors.password)
      if (this.registerErrors.confirmPassword) return this.toast('error', this.registerErrors.confirmPassword)

      const account = (this.registerForm.account || '').trim()
      const name = (this.registerForm.name || '').trim()
      const studentNo = (this.registerForm.studentNo || '').trim()

      if (!account || !name || !studentNo) {
        this.toast('warning', '请填写账号、学号和昵称')
        return
      }
      if (!this.registerForm.password || !this.registerForm.confirmPassword) {
        this.toast('warning', '请设置并确认密码')
        return
      }
      if (!this.registerForm.agree) {
        this.toast('warning', '请先阅读并同意用户协议')
        return
      }

      try {
        // request.js 已经 return response.data，所以这里 r 就是 Result
        const r = await this.$axios.post('/user/register', {
          account,
          name,
          studentNo,
          password: this.registerForm.password,
          confirmPassword: this.registerForm.confirmPassword
        })

        if (!r || r.code !== 200) {
          const msg = r?.msg || '注册失败'
          if (String(msg).includes('已存在')) this.registerErrors.account = '该账号已存在'
          this.toast('error', msg)
          return
        }

        this.toast('success', '注册成功，请登录')
        this.switchMode('login')
      } catch (e) {
        const status = e?.response?.status
        const msg = e?.response?.data?.msg || e?.message
        this.toast('error', status ? `注册失败(${status})：${msg}` : `注册失败：${msg}`)
      }
    },

    scheduleCheckAccount () {
      if (this.mode !== 'register') return
      clearTimeout(this.accountTimer)
      this.accountTimer = setTimeout(() => this.checkAccountExists(), 350)
    },

    async checkAccountExists () {
      if (this.mode !== 'register') return
      const acc = (this.registerForm.account || '').trim()
      if (!acc) {
        this.registerErrors.account = ''
        return
      }
      try {
        const r = await this.$axios.get('/user/check-account', { params: { account: acc } })
        const exists = r?.code === 200 && r?.data === true
        this.registerErrors.account = exists ? '该账号已存在' : ''
      } catch (e) {
        this.registerErrors.account = ''
      }
    },

    checkPasswordRules () {
      if (this.mode !== 'register') return
      const p = this.registerForm.password || ''

      if (!p) {
        this.registerErrors.password = ''
        return
      }

      if (!/^[A-Za-z0-9_]+$/.test(p)) {
        this.registerErrors.password = '不支持除字母、数字、_以外的字符'
        return
      }

      if (p.length < 6) {
        this.registerErrors.password = '密码长度至少6位'
        return
      }

      this.registerErrors.password = ''
    },

    checkPasswordMatch () {
      const p1 = this.registerForm.password || ''
      const p2 = this.registerForm.confirmPassword || ''

      if (!p2) {
        this.registerErrors.confirmPassword = ''
        return
      }
      this.registerErrors.confirmPassword = (p1 !== p2) ? '密码不同' : ''
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.auth-wrapper {
  display: flex;
  width: 1040px;
  max-width: 96vw;
  min-height: 540px;
  max-height: calc(100vh - 80px);
  border-radius: 26px;
  overflow: hidden;
  background-color: #ffffff;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.35);
}

.auth-left {
  flex: 1;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
  padding: 40px 56px;
  overflow-y: auto;
}

.auth-logo-row {
  display: flex;
  align-items: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-weight: 600;
  font-size: 20px;
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.4);
}

.logo-initial { transform: translateY(1px); }

.logo-text { margin-left: 12px; }
.logo-title { font-size: 18px; font-weight: 600; color: #111827; }
.logo-subtitle { font-size: 12px; color: #9ca3af; }

.auth-card { max-width: 420px; }

.mode-toggle {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background-color: rgba(15, 23, 42, 0.04);
  margin-bottom: 12px;
}

.mode-btn {
  border: none;
  background: transparent;
  padding: 6px 22px;
  border-radius: 999px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mode-btn.active {
  background-color: #ffffff;
  color: #111827;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.16);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group { display: flex; flex-direction: column; }
.form-label { font-size: 13px; color: #4b5563; margin-bottom: 6px; }
.form-label-inline { font-size: 13px; color: #4b5563; margin-right: 8px; }

.form-input {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  padding: 10px 14px;
  font-size: 14px;
  outline: none;
  background-color: #ffffff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.form-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.form-inline {
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.radio-label {
  font-size: 13px;
  color: #4b5563;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.check-label {
  font-size: 12px;
  color: #4b5563;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.form-helper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.primary-btn {
  margin-top: 4px;
  width: 100%;
  height: 44px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(90deg, #2563eb, #1d4ed8);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.45);
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}

.primary-btn:hover { transform: translateY(-1px); box-shadow: 0 16px 36px rgba(37, 99, 235, 0.55); }
.primary-btn:active { transform: translateY(0); box-shadow: 0 8px 22px rgba(37, 99, 235, 0.4); }

.primary-btn-small { width: auto; min-width: 140px; height: 38px; font-size: 14px; }

.link-btn {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
}

.bottom-tip { margin-top: 6px; font-size: 12px; color: #6b7280; }

.auth-right {
  flex: 1;
  background: radial-gradient(circle at 0% 0%, #1f2937, #020617),
  radial-gradient(circle at 100% 10%, #1d4ed8, #020617);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 60px;
}

.slogan-block { max-width: 420px; }
.slogan-title { font-size: 26px; font-weight: 700; margin: 0 0 12px; }
.slogan-desc { font-size: 14px; opacity: 0.9; margin: 0 0 16px; }
.slogan-list { list-style: none; padding: 0; margin: 0 0 14px; font-size: 13px; opacity: 0.95; }
.slogan-list li { margin-bottom: 4px; }
.slogan-sign { font-size: 13px; opacity: 0.9; margin-top: 2px; }

.field-error { margin-top: 6px; font-size: 12px; color: #ef4444; }

/* 协议弹窗 */
.agreement-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.agreement-dialog {
  width: 720px;
  max-width: 95vw;
  max-height: 80vh;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.45);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agreement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.agreement-title { margin: 0; font-size: 16px; font-weight: 600; color: #111827; }

.agreement-close {
  border: none;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #6b7280;
}

.agreement-close:hover { color: #111827; }

.agreement-body { padding: 12px 20px 4px; flex: 1; }

.agreement-scroll {
  max-height: 54vh;
  overflow-y: auto;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
}

.agreement-scroll p { margin: 6px 0; }

.agreement-footer {
  padding: 10px 20px 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

/* 窄屏适配 */
@media (max-width: 960px) {
  .auth-wrapper { flex-direction: column; height: auto; max-height: none; width: 100%; border-radius: 0; }
  .auth-left { padding: 24px 20px 18px; overflow-y: visible; }
  .auth-card { max-width: 100%; }
  .auth-right { padding: 24px 24px 32px; }
}
</style>