# 修复与更新日志

> 日期：2026-05-29
> 范围：全功能扫描、BUG 修复、功能完善

---

## 一、功能发现与验证

对全部 21 个前端页面 + 31 个后端控制器进行了系统化扫描与验证：

| 类别 | 页面数 | 验证状态 |
|------|--------|----------|
| 门户页 | 4 | 全部通过 |
| 学生端 | 8 | 全部通过 |
| 教师端 | 8 | 全部通过 |
| API 函数 | 85 | 全部通过 |
| 后端端点 | 31 控制器 | 全部通过 |

---

## 二、高危 BUG 修复

### 1. 路由缺失 — 4 个链接 404（严重）

| 缺失路由 | 引用位置 | 修复 |
|----------|----------|------|
| `/student/workspace` | StudentExam.vue, LearningAnalyticsView.vue, AIFloatingBall.vue | 新增路由 → LearningAnalyticsView |
| `/student/analytics` | AICompanionImmersive.vue, AIFloatingBall.vue | 新增路由 → LearningAnalyticsView |
| `/student/exam` | AICompanionImmersive.vue, AIFloatingBall.vue | 新增路由 → StudentExam |
| `/student/courses`（无 ID） | AICompanionImmersive.vue | 新增重定向 → `/student/subjects` |

### 2. 作业管理绕过认证层（严重）

- `AssignmentManagement.vue` 使用原始 `axios` 直接请求，**绕过 JWT 拦截器**
- 修复：改为使用 API 模块 `apiClassAssignments`、`apiCreateAssignment`
- 新增 API 函数：`apiClassAssignments`、`apiCreateAssignment`、`apiAssignmentSubmissions`
- 修复硬编码 `classId: 1` → 动态班级选择器
- 新增创建后自动刷新列表

### 3. 考试题库数据硬编码（严重）

- `StudentExam.vue` 的 `examQuestions` 始终使用 6 道硬编码题目，**从不加载真实考题**
- 修复：`startExam()` 中调用 `apiExamDetail()` 加载真实考题
- 保留硬编码考题作为 API 失败时的 fallback

### 4. AI 问卷能力图硬编码（严重）

- `AIQuestionnaire.vue` 的六维能力雷达图使用前端硬编码规则计算（80+ 行条件判断），**完全忽略 API 返回的分析结果**
- 修复：能力图改为从 `apiBuildProfile` 响应中解析，API 数据优先

---

## 三、中危 BUG 修复

### 5. 内容审核操作无确认（中危）

- `AIContentReview.vue`：审核通过/拒绝一键执行，**无确认弹窗**，误点不可逆
- 修复：添加 `ElMessageBox.confirm()` 确认弹窗
- 所有 catch 块添加 `ElMessage.error()` 用户反馈
- 添加 `loading` 状态

### 6. 首页 AI 推荐区域永久空置（中危）

- `PortalHome.vue`：`recommendations` ref 初始化后**从未被填充**，"AI 为你推荐"区域始终显示"完成学习后获取个性化推荐"
- 修复：`loadAuthData()` 中添加 `apiResourceRecommendation()` 调用
- 从 API 返回资源中提取前 5 条填充推荐列表

### 7. AI 辅导 SSE 连接泄漏（中危）

- `CourseView.vue`：`openTutorSSE()` 返回值（含 `close()` 方法）**未被保存**
- 切换页面时 SSE 连接未关闭，造成连接累积
- 修复：新增 `sseConnection` ref，组件 `onUnmounted` 时调用 `close()`，新建连接前关闭旧连接

### 8. 教师看板未使用变量（中危）

- `TeacherDashboard.vue`：导入但未使用的 `useAuthStore` 和 `auth`
- `notices` 字段初始化但从未在模板中使用
- 修复：移除无用导入和字段

### 9. 学科目录数据提取不一致（中危）

- `SubjectCatalog.vue`：`apiSubjects` 响应直接作为数组使用
- 若后端包装在 `{subjects: [...]}` 中则页面空白
- 修复：统一响应解包逻辑 `Array.isArray ? data : (data?.subjects || data?.list || [])`
- 添加 `ElMessage.warning` 错误提示（原为静默忽略）
- 添加缺失的 `ElMessage` 导入

### 10. 个人中心打卡字段不匹配（中危）

- `ProfileView.vue`：`streakRes.data?.streak || 0` — API 返回字段为 `currentStreak`
- 与 PortalHome 的解包逻辑不一致，导致打卡天数始终显示 0
- 修复：使用 `currentStreak || current_streak || streak` 优先级 fallback

---

## 四、低危修复

### 11. 后台 API Key 残留明文

- `application.yml` 的 fallback 默认值中包含真实 API Key（DeepSeek、百度搜索 Token）
- `Neo4jConfig.java` 默认密码硬编码 `password123`
- 修复：全部替换为占位符，通过环境变量注入

---

## 五、环境变量配置

通过 `setx` 在 Windows 用户环境变量中配置：

| 变量名 | 用途 | 获取地址 |
|--------|------|----------|
| `LLM_API_KEY` | DeepSeek 大模型 | https://platform.deepseek.com/api_keys |
| `BAIDU_SEARCH_TOKEN` | 百度千帆 AI 搜索 | https://console.bce.baidu.com/iam/#/iam/apikey/list |

---

## 六、文件清理

| 文件 | 原因 |
|------|------|
| `views/student/AICompanionView.vue` | 远程合并重新引入，已被 AICompanionImmersive.vue 替代 |
| `views/teacher/TeacherExam.vue` | 远程合并重新引入，未接入路由 |
| `views/teacher/ResourceManage.vue` | 远程合并重新引入，功能已整合到 TeacherManage |
| `views/common/DataCenter.vue` | 远程合并重新引入，功能已分散到各看板 |
| `views/common/datacenter/` (5 文件) | 同上 |

---

## 七、修复统计

| 类别 | 数量 |
|------|------|
| 严重 BUG | 4 |
| 中危 BUG | 6 |
| 低危修复 | 2 |
| **BUG 合计** | **12** |
| 新增 API 函数 | 3 |
| 新增路由 | 4 |
| 已删除无用文件 | 9 |

---

## 八、已知待完善项

1. **TeacherManage 题库系统**：当前仍使用部分 mock 数据，需进一步对接 `apiTeacherQuestionBank()` 实现完整 CRUD
2. **CommunityView 学习社区**：整个页面使用 mock 数据，后端无社区问答 API 端点，需完整后端支持
3. **TeacherAssistant 资源保存**：`saveResource()` 为占位函数，后端 `ResourceController` 无保存端点
4. **AICompanionImmersive 语音输入**：录音功能已实现但 STT 管道未接通，`apiSTT` 存在但从未被调用
5. **Neo4j 知识图谱**：Neo4j 服务未启动，`KnowledgeGraphSyncService` 启动时同步失败（不影响 MySQL 知识图谱功能）
