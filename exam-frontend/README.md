# 在线考试系统 - 前端

基于 **Vue 3 + Vite + Element Plus + Pinia + Vue Router** 的在线考试系统前端，配套 `exam-backend`（Spring Boot + MyBatis + MySQL）。

接口契约见仓库根目录 `在线考试系统 · 接口文档 v1.0.md`。本前端**严格按照该契约**实现请求层与页面。

## 快速开始

```bash
npm install
npm run dev      # 启动开发服务器，默认 http://localhost:5173
npm run build    # 生产构建
```

## 演示账号

| 角色 | 用户名 | 密码 |
| ---- | ------ | ---- |
| 管理员 | `admin` | `123456` |
| 学生 | `student` | `123456` |

登录页可点击「重置演示数据」恢复初始种子数据。

## Mock 数据模式（默认）

`.env.development` 中 `VITE_USE_MOCK=true`，前端默认运行在 **Mock 模式**，不依赖后端即可完整体验所有功能。Mock 引擎（`src/api/mock/`）按契约接口路径 + 统一返回格式 `{code, message, data}` 实现，数据存储在浏览器 `localStorage`。

### 切换到真实后端

1. 将 `.env.development` 中 `VITE_USE_MOCK` 改为 `false`；
2. 启动后端（端口 8080），前端 `/api` 请求会通过 Vite 代理转发到 `http://localhost:8080`；
3. WebSocket 倒计时直连 `ws://<hostname>:8080/ws/exam`（可通过 `VITE_WS_URL` 覆盖）。

## 功能与页面

| 角色 | 页面 | 路由 |
| ---- | ---- | ---- |
| 学生 | 考试列表 | `/exams` |
| 学生 | 考试答题（倒计时 / 自动保存 / 断线续考） | `/exam/:paperId` |
| 学生 | 交卷成绩 | `/exam/result/:recordId` |
| 学生 | 我的记录 | `/records` |
| 学生 | 记录详情 | `/records/:id` |
| 学生 | 错题本（列表 / 今日复习 / 标记掌握） | `/wrong-questions` |
| 学生 | 成绩分析（成绩趋势折线图 + 知识点掌握度雷达图） | `/stats` |
| 管理员 | 试卷管理（手动组卷 / 发布 / 删除） | `/admin/papers` |
| 管理员 | 智能组卷 | `/admin/papers/generate` |
| 管理员 | 题库管理（题型/难度/知识点/关键词筛选） | `/admin/questions` |
| 管理员 | 成绩分析（试卷正确率） | `/admin/stats` |

## 目录结构

```
src/
├── api/                 # 请求层
│   ├── request.js       # axios 封装 + 统一返回格式处理 + 拦截器
│   ├── auth.js          # 登录 / 注册 / 当前用户 / 退出
│   ├── question.js      # 题库管理
│   ├── paper.js         # 试卷管理 + 智能组卷 + 发布
│   ├── exam.js          # 学生端考试
│   ├── wrongQuestion.js # 错题本
│   ├── stats.js         # 成绩分析
│   └── mock/            # Mock 引擎与种子数据（db.js / index.js）
├── store/user.js        # Pinia 登录态
├── store/exam.js        # Pinia 当前考试 / 答题进度 / 剩余时间
├── router/index.js      # 路由 + 权限守卫
├── layout/MainLayout.vue# 侧边栏布局
├── utils/               # auth / format / ws（WebSocket 倒计时）
└── views/
    ├── Login.vue
    ├── student/         # 学生端
    └── admin/           # 管理员端
```

## 核心接口约定（详见接口文档 v1.0）

统一返回 `{ code, message, data }`，`code === 200` 表示成功；认证使用 `Authorization: Bearer {token}`。

| 模块 | 方法 | 路径 | 说明 |
| ---- | ---- | ---- | ---- |
| 认证 | POST | `/api/auth/login` | 登录，返回 `{ id, username, role, token }` |
| 认证 | POST | `/api/auth/register` | 学生注册 |
| 认证 | GET | `/api/auth/me` | 当前用户 |
| 题库 | GET/POST/PUT/DELETE | `/api/questions` | 题目增删改查 |
| 试卷 | GET/POST/PUT/DELETE | `/api/papers` | 试卷管理 |
| 试卷 | POST | `/api/papers/generate` | 智能组卷 |
| 试卷 | POST | `/api/papers/{id}/publish` | 发布试卷 |
| 考试 | GET | `/api/exams/available` | 可参加考试 |
| 考试 | POST | `/api/exams/{paperId}/start` | 开始 / 继续考试 |
| 考试 | POST | `/api/exams/{recordId}/save` | 保存进度 |
| 考试 | POST | `/api/exams/{recordId}/submit` | 交卷判分 |
| 考试 | GET | `/api/exams/records` | 我的记录 |
| 错题 | GET | `/api/wrong-questions` | 错题列表 |
| 统计 | GET | `/api/stats/my-scores` | 成绩趋势 |
| 统计 | GET | `/api/stats/knowledge-mastery` | 知识点掌握度 |
| 统计 | GET | `/api/stats/paper/{id}/accuracy` | 试卷正确率 |
| 推送 | WS | `/ws/exam` | 倒计时推送 |

> 数据模型与 `exam-backend` 表结构对应：`user` / `question` / `exam_paper` / `paper_question` / `exam_record` / `answer_record` / `wrong_question`。
> 试题 `type`：1 单选、2 多选、3 判断；`difficulty`：1 简单、2 中等、3 困难；单选答案为 `A`，多选为去重排序的字母串（如 `ABD`），判断为 `T` / `F`。
