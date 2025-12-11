<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧登录表单区域 -->
      <div class="login-left">
        <!-- 顶部品牌栏 -->
        <header class="brand">
          <div class="brand-logo">
            <span>自</span>
          </div>
          <div class="brand-text">
            <div class="brand-title">自习室预约系统</div>
            <div class="brand-subtitle">Study Room Reservation</div>
          </div>
        </header>

        <!-- 中间登录内容 -->
        <main class="login-main">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">登录到您的账号，开始管理自习室预约。</p>

          <form class="login-form" @submit.prevent="onSubmit">
            <div class="form-group">
              <label for="username">账号（学号 / 工号）</label>
              <input
                  id="username"
                  type="text"
                  v-model="username"
                  placeholder="请输入学号或工号"
              >
            </div>

            <div class="form-group">
              <label for="password">密码</label>
              <input
                  id="password"
                  type="password"
                  v-model="password"
                  placeholder="请输入密码"
              >
            </div>

            <div class="role-group">
              <div class="role-group-label">登录身份</div>
              <div class="role-options">
                <label>
                  <input
                      type="radio"
                      value="user"
                      v-model="role"
                  >
                  学生用户
                </label>
                <label>
                  <input
                      type="radio"
                      value="admin"
                      v-model="role"
                  >
                  管理员
                </label>
              </div>
            </div>

            <button type="submit" class="btn btn-primary">
              登录
            </button>
          </form>
        </main>
      </div>

      <!-- 右侧图片 & 文案区域 -->
      <div class="login-right">
        <div class="hero-gradient"></div>
        <div class="hero-content">
          <h3 class="hero-title">预约自习室的最佳通用解决方案</h3>
          <p class="hero-desc">
            统一的自习室预约平台，让学生轻松选座，管理员高效管理场地和预约。
          </p>
          <p class="hero-author">— SSRMS</p>

          <div class="hero-images">
            <div class="image-card image-1">
              <!-- 在这里放第一张图片，例如：
              <img src="@/assets/login-1.jpg" alt="学习场景 1">
              -->
            </div>
            <div class="image-card image-2">
              <!-- 第二张图片，例如：
              <img src="@/assets/login-2.jpg" alt="学习场景 2">
              -->
            </div>
            <div class="image-card image-3">
              <!-- 第三张图片，例如：
              <img src="@/assets/login-3.jpg" alt="学习场景 3">
              -->
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

export default {
  name: 'LoginPage',
  setup () {
    const router = useRouter()
    const username = ref('')
    const password = ref('')
    const role = ref('user')   // 'user' = 学生, 'admin' = 管理员

    const onSubmit = async () => {
      if (!username.value || !password.value) {
        alert('请先输入账号和密码')
        return
      }

      // 和后端约定：0 管理员，1 学生
      const roleId = role.value === 'admin' ? 0 : 1

      try {
        const res = await axios.post('http://localhost:8090/user/login', {
          account: username.value,
          password: password.value,
          roleId
        })

        const body = res.data
        if (body.code === 200) {
          // 把当前登录用户的信息存起来（关键一步）
          // 后端返回的是 User 实体，我们直接存
          localStorage.setItem('ssrmsUser', JSON.stringify(body.data))

          // 然后按身份跳转
          if (roleId === 0) {
            router.push('/admin')
          } else {
            router.push('/user')
          }
        } else {
          alert(body.msg || '账号或密码错误')
        }
      } catch (error) {
        console.error(error)
        alert('登录失败，请检查后端服务是否已启动')
      }
    }

    return {
      username,
      password,
      role,
      onSubmit
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

/* 外层背景，居中大卡片 */
.login-page {
  min-height: 100vh;
  font-family: Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* 左右分栏的大容器 */
.login-container {
  width: 100%;
  max-width: 1200px;
  height: 680px;
  background-color: #f9fafb;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.35);
  display: flex;
}

/* 左侧：表单区域 */
.login-left {
  flex: 5;
  padding: 40px 56px;
  display: flex;
  flex-direction: column;
}

/* 左上角品牌区域 */
.brand {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.brand-logo {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #2563eb;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 30px;
  font-weight: 600;
  color: #111827;
}

.brand-subtitle {
  font-size: 20px;
  color: #9ca3af;
}

/* 中间表单主体 */
.login-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  max-width: 420px;
  padding-top: 20px;
}

.login-title {
  font-size: 30px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 10px;
}

.login-subtitle {
  font-size: 20px;
  color: #6b7280;
  margin-bottom: 20px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 表单通用样式 */
.form-group label {
  display: block;
  margin-bottom: 15px;
  font-size: 18px;
  color: #374151;
}

.form-group input {
  width: 100%;
  padding: 11px 12px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
  background-color: #ffffff;
}

.form-group input::placeholder {
  color: #9ca3af;
}

.form-group input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.3);
  background-color: #f9fafb;
}

/* 登录身份 */
.role-group {
  margin-top: 4px;
}

.role-group-label {
  font-size: 16px;
  color: #6b7280;
  margin-bottom: 10px;
}

.role-options {
  display: flex;
  gap: 50px;
  font-size: 15px;
  color: #374151;
}

.role-options label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.role-options input[type="radio"] {
  accent-color: #2563eb;
}

/* 登录按钮 */
.btn {
  margin-top: 30px;
  display: inline-block;
  width: 100%;
  border: none;
  border-radius: 999px;
  padding: 14px 20px;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.12s ease;
}

.btn-primary {
  background-color: #2563eb;
  color: #ffffff;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.4);
}

.btn-primary:hover {
  background-color: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 22px 55px rgba(37, 99, 235, 0.5);
}

.btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.45);
}

/* 底部说明 */
.helper-text {
  margin-top: 16px;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.6;
}

/* 右侧：图片与文案区域 */
.login-right {
  flex: 5;
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at top left, #111827, #020617);
  color: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(circle at top, rgba(96, 165, 250, 0.45), transparent 55%),
      radial-gradient(circle at bottom right, rgba(248, 250, 252, 0.1), transparent 60%);
  opacity: 0.9;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.hero-title {
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 12px;
}

.hero-desc {
  font-size: 14px;
  color: #e5e7eb;
  line-height: 1.7;
  margin-bottom: 24px;
}

.hero-author {
  font-size: 13px;
  color: #cbd5f5;
  margin-bottom: 40px;
}

/* 右侧悬浮图片卡片 */
.hero-images {
  position: absolute;
  right: 40px;
  top: 70px;
  bottom: 40px;
  width: 320px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  pointer-events: none;
}

.image-card {
  width: 210px;
  height: 130px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.92);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.75);
  overflow: hidden;
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 模拟右侧示意图中的倾斜效果 */
.image-1 {
  transform: rotate(-18deg) translateX(-40px);
}

.image-2 {
  transform: rotate(8deg);
}

.image-3 {
  transform: rotate(-6deg) translateX(30px);
}

/* 响应式：窄屏时隐藏右侧，仅显示表单 */
@media (max-width: 960px) {
  .login-container {
    height: auto;
    flex-direction: column;
  }

  .login-left {
    padding: 28px 20px 32px;
  }

  .login-main {
    max-width: 100%;
  }

  .login-right {
    display: none;
  }
}
</style>