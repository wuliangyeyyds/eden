# SSRMS（自习室预约管理系统）项目说明

> 本 README 只描述**如何在本地跑起来**。

## 1. 环境与工具建议

- **强烈建议：IntelliJ IDEA 企业版（Ultimate）** 
  社区版（Community）是否能完整支持本项目的后端开发体验/启动脚本，我这边未做验证。
- 后端：Java + Maven + MySQL
- 前端：Node.js + Vue CLI

---

## 2. 后端启动（Spring Boot）

### 2.1 创建数据库并导入 SQL

1. 在本地 MySQL 中创建数据库：`ssrms`
2. 执行初始化脚本：`ssrms.sql`

### 2.2 修改 `application.yml`

在后端配置文件 `application.yml` 中，把数据库连接信息改成你本机的 MySQL 配置：

- 数据库名：`ssrms`
- 用户名：你的 MySQL 用户名
- 密码：你的 MySQL 密码

---

## 3. IDEA 打开项目的注意事项（Maven）

进入代码后，IDEA 可能会弹出“是否使用/导入 Maven”的提示：

- **一定要选择「是」**
- 否则可能**无法生成或识别后端的启动脚本**

---

## 4. 前端启动（Vue）

### 4.1 安装 Node.js 与 Vue CLI

前端需要先在本地安装 **Node.js**，安装完成后，执行：

```bash
npm install -g @vue/cli
```

> Vue / Node 的详细安装教程可自行在网上搜索。

### 4.2 安装依赖与启动

进入前端目录后安装依赖：

```bash
npm install
```

启动方式取决于你的项目脚本配置（通常是 `npm run serve` ）。 
如果你的 IDEA 里已经存在前端启动配置，可能是历史遗留配置，可以检查并确保：

- `package.json` 正确
- Node 解释器路径正确（IDEA Settings/Preferences 里设置）
- 运行配置（Run/Debug Configuration）中：
  - Command：`run`
  - Scripts：选择 `serve`

---

## 5. Coze API（AI 功能）

本项目使用了 **Coze 的 API** 来实现 AI 相关能力。

- 需要你自行研究如何申请/配置 Coze
- 然后将项目中相关的 API Key / Token / 配置项按教程接入

---

## 6. 检查清单

- 数据库是否已创建为 `ssrms`，并执行过 `ssrms.sql`
- `application.yml` 的 MySQL 用户名/密码/端口是否正确
- IDEA 是否已导入 Maven（依赖是否下载完成）
- 前端是否安装了 Node.js，且 `npm install` 成功
