# 智备优教（A3 赛题）

> 赛题：基于大模型的个性化资源生成与学习多智能体系统开发

## 1. 项目概述

本项目实现一个面向高校教学场景的"多智能体 + 个性化学习资源生成"系统，围绕教师端、学生端两大视角进行设计，满足软件杯 A3 赛题的核心要求：

- 对话式学习画像动态构建（不少于 6 个维度，当前实现 10 个维度）
- 多智能体协同生成（5 类个性化资源）
- 个性化学习路径规划与资源推送
- 智能辅导（文字 + 图解/视频建议）
- 学习效果评估与动态优化建议
- 教师端 / 学生端完整演示链路

## 2. 技术栈

- 前端：Vue 3 + Element Plus + ECharts 5 + Pinia + Vue Router 4 + Axios + Marked + SCSS
- 后端：Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 + MySQL 8.0 + Redis 7 + JWT
- 构建：Maven / Vite 5.4 / Docker Compose

## 3. 功能结构

### 3.1 前端页面路由

**门户（公开访问）**
- `/` — 门户首页（PortalHome）
- `/subjects` — 学科目录
- `/subjects/:id` — 课程详情
- `/questionnaire` — AI 初始问卷（需登录）

**学生端（/student）**
- `dashboard` — 学习概览看板
- `subjects` — 学科目录
- `courses/:id` — 课程学习页（含 AI 对话、练习、讲义）
- `knowledge-map` — 知识星图
- `companion` — AI 学习伙伴（沉浸式）
- `profile` — 个人中心（含成就、学习分析、自适应测评、考试面板）
- `community` — 学习社区

**教师端（/teacher）**
- `dashboard` — 教学数据看板
- `assistant` — 教学助手（SSE 流式 AI 对话 + PPT 生成）
- `manage` — 题库管理
- `classes` — 班级管理
- `content` — 内容管理（课程/章节/知识点）
- `review` — AI 内容审核
- `assignments` — 作业管理
- `profile` — 个人中心

### 3.2 多智能体模块

后端已实现 5 个角色智能体：

- `SyllabusAgent`：课程讲解文档生成
- `MindMapAgent`：知识点思维导图生成
- `QuestionBankAgent`：分层练习题/测评题生成
- `MediaAgent`：多模态视频/动画脚本生成
- `CodingAgent`：代码实操案例与实践项目材料生成

## 4. 赛题要求映射

### 4.1 对话式学习画像自主构建

接口：`/api/profile/dialogue` — 支持学生通过自然语言输入学习情况，自动抽取并构建动态画像，覆盖专业、课程、知识基础、认知风格、易错点/短板、兴趣偏好、学习节奏、考试目标、学习目标、资源偏好、风险标签、对话总结等 12 个维度。

### 4.2 多智能体协同资源生成

接口：`/api/resources/generate` — 一键生成 5 类个性化资源：课程讲解文档、思维导图、分层练习题、视频/动画脚本、代码实操案例。

### 4.3 个性化学习路径规划

接口：`/api/path/generate` — 根据学生阶段、目标与画像生成动态学习路径，推送讲义精读包、知识脉络导图、分层练习题、视频脚本、代码实操任务单。

### 4.4 智能辅导

接口：`/api/tutor/ask` — 支持 SSE 流式输出，返回 Markdown 结构化讲解、图解建议、视频讲解建议、参考资料建议。

### 4.5 学习效果评估

接口：`/api/assessment/evaluate` — 输出准确率、活跃度、完成度、资源使用度、学习稳定性、诊断结论、优化建议。

## 5. 启动方式

### 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Maven | 3.8+ |

### 方式 A：本地启动

**① 启动 MySQL + Redis**

- MySQL：`127.0.0.1:3306`，账号密码 `root/root`
- Redis：`127.0.0.1:6379`

执行数据库初始化：

```sql
source sql/init.sql;
```

**② 启动后端**

```bash
cd softwacecup/backend
mvn spring-boot:run
# 后端地址：http://localhost:8080
```

**③ 启动前端**

```bash
cd softwacecup/frontend
npm install
npm run dev
# 前端地址：http://localhost:5173
```

### 方式 B：Docker 启动

```bash
docker compose up -d
```

### 常见问题

**Q: 前端请求后端接口报跨域错误？**
A: 确认后端已启动在 8080 端口，`frontend/vite.config.js` 已配置代理 `/api` → `http://localhost:8080`。

**Q: 数据库连接失败？**
A: 检查 MySQL 是否启动，数据库名 `smartprep` 是否已创建，用户名密码是否正确。

**Q: 大模型功能不可用？**
A: 检查 `LLM_API_KEY` 环境变量是否配置，或在 `backend/src/main/resources/application.yml` 中修改 `llm.api-key`。

## 6. 默认账号

- 管理员：admin / 123456
- 教师：teacher / 123456
- 学生：student / 123456

## 7. 项目结构

```
softwacecup/
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/iflytek/smartprep/
│       ├── config/              # 安全配置、JWT、拦截器
│       ├── controller/          # REST 控制器（30个）
│       ├── domain/              # MyBatis-Plus 实体
│       ├── dto/                 # 数据传输对象
│       ├── mapper/              # MyBatis-Plus Mapper
│       └── service/             # 业务逻辑 + 5个 Agent
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── api/                 # HTTP 客户端（10个模块，85个函数）
│       ├── components/          # 通用组件（14个 + course子组件6个）
│       ├── layout/              # 布局组件
│       ├── router/              # Vue Router 路由配置
│       ├── stores/              # Pinia 状态管理
│       ├── styles/              # 全局样式
│       ├── utils/               # 工具函数
│       └── views/               # 页面视图（17个路由页面）
├── sql/                         # 数据库初始化脚本
│   └── init.sql
├── knowledge-base/              # 知识库文件
└── docker-compose.yml
```

## 8. 安全机制

- JWT Token 鉴权 + 角色权限控制（基于注解的接口鉴权）
- 内容安全过滤（敏感词拦截）
- 资源生成可信度字段输出
- 全局异常处理
- 前端 Axios 拦截器自动注入 Token + 401 自动登出

## 9. 更新日志

详见 [CHANGELOG.md](softwacecup/CHANGELOG.md)（2026-05-28 前端全面修复与优化）
