# 修复与更新日志

> 日期：2026-05-28
> 范围：前端全面 BUG 修复、功能完善、代码清理

---

## 一、BUG 修复

### 1. CourseView.vue — 课程学习页（4 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **严重** | `exerciseLoading` 拼写错误导致练习按钮无法禁用 | 修正为 `practiceLoading` |
| **高** | `generatePractice()` 提前 return 时未重置 loading 状态，导致按钮永久卡死 | 在每个 early return 前添加 `practiceLoading.value = false` |
| **中** | `loadCourse()` 缺少 catch 块，API 失败时静默无提示 | 添加 catch 块显示错误消息 |
| **中** | `loadSubChapter()` 无 try-catch，子章节加载失败时崩溃 | 包裹 try-catch 异常处理 |
| **低** | 练习题生成失败时使用无意义的硬编码 fallback 数据 | 改为 `ElMessage.warning` 用户友好提示 |

### 2. PortalHome.vue — 门户首页（2 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | `goContinue()` 始终导航到 `/student/dashboard`，未使用实际最近课时 | 改为导航到 `lastLesson.lessonId`，无效时 fallback 到 `/student/subjects` |
| **低** | 导入了未使用的 `apiMe` | 移除无用导入 |

### 3. StudentDashboard.vue — 学生看板（4 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | 雷达图数据使用 `d.accuracy` 但 API 字段为 `weeklyAccuracy`，导致图表无数据 | 改为 `d.weeklyAccuracy \|\| d.accuracy \|\| 0` |
| **中** | `isNewUser` 判断逻辑错误：API 异常时也标记为新用户 | 区分"无数据"与"API 错误"两种场景 |
| **低** | `goPathStep()` 缺少 subChapterId fallback | 添加 fallback 逻辑 |
| **低** | ECharts 实例未在组件卸载时销毁（内存泄漏） | 添加 `chartInstance` ref + `onUnmounted` 清理 |

### 4. ClassManagement.vue — 班级管理（2 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **严重** | `removeMember()` 使用原始 `axios.delete()` 绕过认证拦截器，导致请求失败 | 改用已导入的 `apiRemoveClassMember()` 通过 http 实例发送 |
| **低** | 成员列表加载使用串行 `for` 循环 | 改为 `Promise.all` 并行加载 |

### 5. TeacherDashboard.vue — 教师看板（8 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **高** | ECharts 图表使用硬编码模拟数据，不反映真实 API 数据 | 所有图表改为从 API 响应动态渲染 |
| **中** | ECharts 实例未清理导致内存泄漏 | 添加 `onUnmounted` + `dispose()` 生命周期管理 |
| **中** | `getInstanceByDom()` 未在初始化前检查已有实例 | 添加已有实例销毁逻辑 |
| **低** | window resize 监听器未在卸载时移除 | 使用命名函数 + `removeEventListener` |
| **低** | 顶部统计卡片趋势值硬编码 | 改为从 `data.value.summary` 读取 |
| **低** | 缺少工作区待办任务区域 | 新增 pending tasks 区域 |

### 6. TeacherManage.vue — 题库管理（3 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | 题库表格列 `prop="name"` 与实际字段 `bankName` 不匹配 | 修正为 `prop="bankName"` |
| **中** | `saveQuestion()` 缺少 `selectedBank` null 检查 | 添加 `if (!selectedBank.value) return` |
| **低** | 上传提示文字 "png/jpg/png/gif" 重复 png | 修正为 "png/jpg/gif" |
| **低** | `apiSubjects()` 和 `apiCreateCourse()` 调用缺少异常处理 | 添加 try-catch + `ElMessage.error` |

### 7. ContentManagement.vue — 内容管理（7 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **高** | 所有 7+ 个 API 调用均无异常处理 | 全部添加 try-catch + `ElMessage.error` |
| **中** | 删除确认使用浏览器原生 `confirm()` 弹窗，视觉风格不统一 | 替换为 `ElMessageBox.confirm()` |
| **低** | 子章节类型显示原始枚举值，用户不可读 | 添加 `typeName()` 映射函数 |
| **低** | 无加载状态指示 | 添加 `loading` ref + 按钮 disabled 状态 |
| **低** | 空列表无提示 | 添加空状态文字 |

### 8. TeacherAssistant.vue — 教学助手（5 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | SSE 连接返回值未保存，无法在组件卸载时关闭 | 保存为 `sseConn.value` |
| **中** | 切换工具标签时未关闭上一个 SSE 连接 | 添加切换时的连接关闭逻辑 |
| **中** | SSE `onError` 使用替换而非追加，流式内容被覆盖 | 改为追加模式 |
| **低** | `generatePpt()` 未校验 `pptForm.points` 是否为数组 | 添加 `Array.isArray()` 检查 |
| **低** | 未使用的 `onMounted` 导入 | 移除，添加 `onUnmounted` |

### 9. LoginModal.vue — 登录弹窗（3 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | 关闭弹窗后表单状态未重置，再次打开显示旧数据 | 添加 `watch` 监听 `showLoginModal`，打开时重置表单 |
| **中** | 快捷登录无验证码时可绕过验证直接请求 | 添加 `captchaCode` 空值检查 |
| **低** | `handleClose` 未清理表单状态 | 添加完整的状态重置逻辑 |

### 10. background.js — 壁纸系统（1 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **严重** | 壁纸 URL 包含中文字符，Windows curl 发 GBK 编码请求导致 Vite 返回 HTTP 500 | 所有壁纸 URL 使用 `encodeURI()` 预编码 |

### 11. manage.js — API 模块（1 处修复）

| 严重度 | 问题 | 修复 |
|--------|------|------|
| **中** | 缺少 `apiRemoveClassMember` 接口函数 | 新增该函数 |

---

## 二、文件清理

### 已删除的未引用文件（8 个）

以下文件不在路由中且未被任何其他文件引用，已移除：

| 文件 | 原因 |
|------|------|
| `views/student/AICompanionView.vue` | 已被 `AICompanionImmersive.vue` 替代 |
| `views/teacher/TeacherExam.vue` | 未接入路由，功能已整合到其他页面 |
| `views/teacher/ResourceManage.vue` | 未接入路由，功能已整合到 TeacherManage |
| `views/common/DataCenter.vue` | 数据中台功能已分散到各看板页面 |
| `views/common/datacenter/DataCenterDepartmentLevelTab.vue` | DataCenter 子组件，随父组件移除 |
| `views/common/datacenter/DataCenterDepartmentPeopleTab.vue` | 同上 |
| `views/common/datacenter/DataCenterSchoolTab.vue` | 同上 |
| `views/common/datacenter/DataCenterStudentTab.vue` | 同上 |
| `views/common/datacenter/datacenter-shared.css` | 同上（目录已移除） |

---

## 三、修复统计

| 类别 | 数量 |
|------|------|
| 严重 BUG | 3 |
| 高 BUG | 4 |
| 中 BUG | 14 |
| 低 BUG | 13 |
| **BUG 合计** | **34** |
| 已删除无用文件 | 9 个 |
| 新增 API 函数 | 1 个 |

---

## 四、已知待完善项

1. **TeacherManage 题库系统**：当前仍使用部分 mock 数据，需进一步对接 `apiTeacherQuestionBank()` 等后端接口实现完整 CRUD
2. **Bilibili 视频导入**：BilibiliImportModal 组件已实现，但课程页内视频嵌入功能待完善
3. **Neo4j 知识图谱**：后端 Neo4jConfig 已配置，前端 KnowledgeStarMap 页面预留了图谱可视化区域，实际数据链路待打通
