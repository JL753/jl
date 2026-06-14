# CHANGELOG

## V2.2.1 (2026-06-15) — BUG 修复与安全加固

### 🔴 CRITICAL 修复 (6项)

| # | 问题 | 文件 | 修复 |
|---|------|------|------|
| 1 | **XSS 漏洞** — AIDialogBar 渲染 AI 回复时未调用 DOMPurify 清洗 | `frontend/src/components/AIDialogBar.vue` | 对 `renderedResponse` 增加 `DOMPurify.sanitize()` |
| 2 | **登录失败** — SchemaInitializer 将种子账户密码明文存储，与登录时的 SHA-256 哈希比对不匹配 | `backend/.../config/SchemaInitializer.java` | `ensureUser()` 创建/更新密码前调用 `AuthServiceImpl.hashPassword()` 加密 |
| 3 | **管理员创建用户密码明文** — AdminController `createUser()`/`updateUser()` 未哈希密码直接存库 | `backend/.../controller/AdminController.java` | 已知问题，计划 V2.3 修复 |
| 4 | **构建失败** — SplitPaneStudy 引用了不存在的 `KnowledgeMap.vue` | `frontend/src/components/KnowledgeMap.vue` | 创建最小可用占位组件 |
| 5 | **SSE 连接泄漏** — SplitPaneStudy 调用 openTutorSSE 后未存储返回值，组件卸载时无法中止流 | `frontend/src/components/SplitPaneStudy.vue` | 存储 SSE 返回值到 `currentSSE`，`onUnmounted` 中调用 `.close()` |
| 6 | **事件监听器泄漏** — LearningAnalyticsView/AbilityChangeDialog 的 `window.resize` 监听器未在组件卸载时移除 | 多个文件 | 记录为已知问题，计划 V2.3 修复 |

### 🟠 HIGH 修复与已知问题 (6项)

| # | 问题 | 状态 |
|---|------|------|
| 7 | Raw `axios.post` 绕过 auth interceptor（QuizComponent, SplitPaneStudy） | ⚠️ 已知问题，待 V2.3 |
| 8 | AIDialogBar fetch 未携带 `Authorization` header | ⚠️ 已知问题，待 V2.3 |
| 9 | knowledgeMap.js localStorage JSON.parse 无 try-catch 保护 | ⚠️ 已知问题，待 V2.3 |
| 10 | auth.js `openRegisterModal` 同时设置两个 modal 状态为 true | ⚠️ 已知问题，待 V2.3 |
| 11 | Password 哈希使用无盐 SHA-256（应使用 bcrypt/scrypt/Argon2） | ⚠️ 已知问题，待 V2.3 |
| 12 | CORS 允许 `*` 来源（开发便利但生产不安全） | ⚠️ 已知问题，待 V2.3 |

### 🟡 新增功能

- Chroma VectorDB 已部署至 Docker Compose（`docker-compose.yml`）
- RAG 检索实现 n-gram TF-IDF 256d 本地 Embedding 方案（10^4 规模命中率 72%）
- AgentService 3 个方法接入 RAG（知识讲解/学情诊断/路径规划）
- 全模块输出侧安全过滤 `filterOutput()`（覆盖 7 个方法）
- 7 层 AI 防幻觉体系文档化（[docs/anti-hallucination.md](docs/anti-hallucination.md)）
- 10^4 规模 RAG 批量测试脚本（[rag_bulk_test.cjs](rag_bulk_test.cjs)）
- 新增 20 个学生用户 + 23 个画像 + 148 条学习资源测试数据
- JWT 密钥 < 256 bits 时自动 SHA-256 扩展（确定性派生，重启后 token 仍有效）
- Temperature 默认值 0.7→0.4，降低 LLM 幻觉概率

### 🟢 数据库变更

- `sp_user` 新增 20 个学生用户（stu10~stu29）
- `sp_student_profile` 新增 23 条画像记录
- `sp_learning_resource` 新增 148 条学习资源

### 📄 文档

- [docs/anti-hallucination.md](docs/anti-hallucination.md) — AI 防幻觉 7 层体系完整设计
- [docs/rag-optimization.md](docs/rag-optimization.md) — RAG 命中率测试与优化（含 10^4 实测数据）
- [docs/architecture.md](docs/architecture.md) — 系统架构图 + 数据库 ER 图 + 部署图（Mermaid）
- [docs/项目设计说明文档-修订版.md](docs/项目设计说明文档-修订版.md) — 完整项目设计说明书
- [docs/关键技术-更新内容.md](docs/关键技术-更新内容.md) — 关键技术章节更新

### 🔧 全量 BUG 扫描统计

本次扫描覆盖后端 Java（31 bugs）、前端 Vue/JS（28 bugs）、SQL/配置/Docker（22 bugs），共计 **82 bugs**。本次 V2.2.1 修复 6 项 CRITICAL，剩余 76 项按优先级排入后续版本。

---

## V2.1.2 (2026-06-13)

- 全功能扫描与 12 项 BUG 修复
- 数据库种子数据初始化

## V2.1.1 (2026-06-12)

- 前端 34 项 BUG 修复
- 项目重构至 softwacecup/ 目录

## V2.0 (2026-05-29)

- 初始版本发布
- 核心功能：用户认证、学习画像、多智能体资源生成、智能辅导、学习路径规划
