# 知域 (ZhiYu) — 重构设计规格

**日期**：2026-05-16  
**方案**：分层重构（方案 C）  
**对标**：可汗学院功能体系 + 知域星图宇宙设计理念

---

## 1. 产品定位

| 项目 | 内容 |
|------|------|
| 品牌名 | 知域 (ZhiYu) |
| 定位 | 全年龄段 AI 增强学习平台（K-12 + 大学） |
| 对标 | 可汗学院功能体系 |
| 技术栈 | Spring Boot 3 + Vue3 + MySQL + Redis |
| 角色 | 学生 / 教师+管理员（两角色） |

---

## 2. 设计系统

### 2.1 视觉主题
- **风格**：星图宇宙风，深色背景 `#080d1f`
- **组件**：液态玻璃（`backdrop-filter: blur` + 半透明边框 + 微光渐变）
- **主色调**：蓝 `#3b82f6` / 紫 `#a855f7` / 青 `#60d9fa`

### 2.2 加载动画
首页进入时显示 3D 旋转 Spinner，加载完成后淡出过渡（`opacity: 1 → 0`，300ms ease-out）。

### 2.3 透明度系统（CSS 自定义属性）
```css
/* 全局亮度/模糊控制，用户可在背景设置中调节 */
--leleo-brightness: 85%;   /* 壁纸亮度 */
--leleo-blur: 5px;          /* 组件背景模糊 */
--leleo-vcard-color: #fff;  /* 卡片主题色，默认白色 */
```
所有液态玻璃组件通过这三个变量统一控制透明度和模糊效果。

### 2.4 壁纸切换系统
浮动选择器，支持：
- **默认**：纯星图宇宙深色背景
- **静态壁纸（3张）**：书房夜晚 / 安逸舒适 / 海洋女孩（`.png` + `.webp` 预览）
- **动态壁纸（2个）**：向往航天的女孩 / 尼尔：机械纪元 团队（`.webm` 循环播放）

切换时同步更新 `--leleo-brightness` / `--leleo-blur` 以保证组件可读性。

### 2.5 卡片组件规范
- **展开/折叠**：Vuetify `<v-expand-transition>` + 手风琴效果（展开一个自动收起其他）
- **响应式布局**：移动端 2 列 → 平板 3 列 → 桌面 4 列（`cols="6" md="4" lg="3"`）
- **卡片图片**：16:9 固定比例（`aspect-ratio="1.7778"`），`opacity: 0.8`，`cover` 裁剪填充

---

## 3. 课程体系结构

```
学科 (Subject)
  └── 课程 (Course)
        └── 单元 (Unit)
              └── 课时 (Lesson)
                    ├── 视频（可选，教师上传）
                    ├── 图文讲义（AI生成初稿，教师审核发布）
                    └── 练习题 (Exercise) → 知识点掌握度更新
```

### 3.1 内容生产流程
1. 教师创建课时框架（标题、知识点标签、难度）
2. AI 五类 Agent 并发生成讲义/题目初稿
3. 教师在审核工作台审核、编辑、发布
4. 学生可学习

### 3.2 初始预置课程
系统初始化时预置一门完整**计算机科学**课程，覆盖完整链路（学科→课程→单元→课时→练习），供新用户完整体验平台功能。

**课程结构示例**：
```
学科：计算机科学
  └── 课程：编程入门
        ├── 单元1：计算机基础
        │     ├── 课时1：什么是计算机（视频+讲义+5题）
        │     ├── 课时2：二进制与数据（讲义+8题）
        │     └── 课时3：操作系统简介（讲义+6题）
        ├── 单元2：Python 编程基础
        │     ├── 课时1：变量与数据类型（讲义+10题）
        │     ├── 课时2：条件与循环（讲义+12题）
        │     └── 课时3：函数与模块（讲义+10题）
        └── 单元3：算法入门
              ├── 课时1：什么是算法（讲义+6题）
              ├── 课时2：排序算法（讲义+8题）
              └── 课时3：递归思想（讲义+8题）
```

---

## 4. 核心学习流程（课时页）

```
进入课时
  → [有视频] 播放视频  /  [无视频] 跳过
  → 阅读图文讲义
      AI 标注：重点🔴（必须掌握）/ 难点🟡（需多练）/ 易错点⚠️（常见误区）
  → 完成练习题（即时反馈 + 提示 + 解析）
  → 掌握度达标 → 解锁下一课时
  → AI 悬浮助手随时可唤起提问（RAG 检索知识库）
```

---

## 5. AI 功能体系

### 5.1 初始问卷（新用户引导）
对话式问卷，AI 询问：当前学习阶段、已掌握知识、学习目标、每日可用时间、学习风格偏好。  
输出：初始六维能力估算 + 个性化学习路径推荐。  
复用现有 `ProfileServiceImpl.buildByDialogue()`，扩展输出六维分数。

### 5.2 六维能力图（持续评估）

来源依据：学习科学 + 数据驱动量化框架

| 维度 | 数据来源 | 计算方式 |
|------|---------|---------|
| 知识广度 | 已完成课时 / 总课时 | 覆盖学科×单元比例 |
| 知识深度 | `sp_user_kp_mastery` | 已掌握知识点掌握度均值 |
| 解题能力 | 练习题答题记录 | 正确率 × 难度加权分 |
| 学习活跃度 | `sp_user_streak` + 操作日志 | 连续天数 + 日均学习时长 |
| 知识迁移 | 跨单元综合题表现 | 跨章节题目正确率 |
| 学习韧性 | `sp_wrong_question` | 错题复习率 + 失败后重试率 |

每次完成练习/课时后重新计算六维分数，LLM 生成文字诊断和改进建议。  
仪表盘布局：六维雷达图固定在左侧，右侧为内容流（继续学习 / AI推荐 / 今日任务）。

### 5.3 互联网资源推荐
新增 `WebSearchAgent`：根据当前学习内容 + 薄弱维度，调用搜索 API 检索外部平台（B站/知乎/CSDN/GitHub 等），推荐视频/文章/工具，展示在仪表盘"AI 为你推荐"卡片。

### 5.4 学习辅助（课时内）
AI 预处理讲义内容，标注三类标签（重点/难点/易错点）。  
悬浮 AI 助手支持随时提问，结合 RAG 检索知识库回答，保存至 `sp_qa_history`。

---

## 6. 页面结构

### 6.1 学生端
| 页面 | 说明 |
|------|------|
| 门户首页 | 学科目录入口，壁纸背景，3D Spinner 加载动画 |
| AI 初始问卷 | 新用户引导，对话式收集背景信息 |
| 学生仪表盘 | 六维图（左固定）+ 继续学习 + AI推荐 + 今日任务 |
| 学科目录 | 浏览所有学科，卡片网格，响应式布局 |
| 课程详情 | 单元列表 + 课时进度条 + 掌握度标识 |
| 课时学习 | 视频(可选)+图文讲义+练习题+AI辅助标注 |
| 练习/测验 | 自适应题目，即时反馈，错题收集 |
| 知识星图 | 知识点掌握度可视化（图谱形式） |
| AI 辅导 | 对话式辅导，RAG 增强，历史记录 |
| 成就中心 | XP / 徽章 / 连续学习 / 排行榜（个人中心下） |
| 个人中心 | 六维图详情 + 学习历史 + 成就中心入口 |

### 6.2 教师端
| 页面 | 说明 |
|------|------|
| 教师仪表盘 | 班级概览 + 学生进度 + 待审核内容 |
| 班级管理 | 创建班级 / 邀请学生 / 查看成员 |
| 内容管理 | 课程/单元/课时/练习的增删改，触发 AI 生成 |
| AI 内容审核 | 审核 AI 生成的讲义和题目，编辑后发布 |
| 作业管理 | 布置作业（指定课时/练习）/ 查看提交 / 批改 |
| 学生进度报告 | 班级六维能力分布 + 个人进度详情 |

---

## 7. 数据库变更

### 7.1 保留表（不变）
`sp_user`, `sp_user_xp`, `sp_user_streak`, `sp_user_badge`, `sp_badge_def`, `sp_question_bank`, `sp_wrong_question`, `sp_qa_history`, `sp_knowledge_doc`

### 7.2 重构表
| 表名 | 变更说明 |
|------|---------|
| `sp_subject` | 新增学科表（原无独立学科层级） |
| `sp_course` | 扩展字段：subject_id, cover_image, difficulty, status |
| `sp_unit` | 重构：关联 course_id，增加 order_index, description |
| `sp_lesson` | 重构：增加 video_url, content_json（讲义+标注）, status（草稿/已发布）|
| `sp_exercise` | 重构：增加 lesson_id, difficulty, knowledge_point_tags |
| `sp_user_kp_mastery` | 扩展：增加 mastery_level（0-4 五级）, practice_count |

### 7.3 新增表
| 表名 | 说明 |
|------|------|
| `sp_class` | 班级（id, name, teacher_id, invite_code, created_at） |
| `sp_class_member` | 班级成员（class_id, student_id, joined_at） |
| `sp_assignment` | 作业（id, class_id, title, lesson_ids_json, due_at） |
| `sp_assignment_submission` | 作业提交（assignment_id, student_id, submitted_at, score） |
| `sp_lesson_progress` | 课时进度（user_id, lesson_id, status, completed_at） |
| `sp_content_review` | AI内容审核（lesson_id, ai_draft_json, status, reviewer_id） |
| `sp_student_ability` | 六维能力历史（user_id, scores_json, evaluated_at, diagnosis） |
| `sp_resource_recommendation` | AI推荐资源（user_id, lesson_id, resources_json, created_at） |

---

## 8. 后端变更

### 8.1 保留（不变）
- `AuthController` + `JwtTokenProvider` + `AuthInterceptor`
- `LLMClient`（DeepSeek）
- 五类 Agent：`SyllabusAgent`, `MindMapAgent`, `QuestionBankAgent`, `MediaAgent`, `CodingAgent`
- `TutorServiceImplWithRAG` + RAG 模块
- `GamificationService` + 相关 Controller
- `AdaptiveQuizService`

### 8.2 新增 Controller / Service
| 模块 | 说明 |
|------|------|
| `ClassController` | 班级 CRUD + 邀请码加入 |
| `AssignmentController` | 作业布置/提交/批改 |
| `ContentReviewController` | AI内容审核工作台 |
| `StudentAbilityController` | 六维能力计算 + 历史查询 |
| `WebSearchAgent` | 互联网资源搜索推荐 |
| `LessonProgressController` | 课时进度追踪 |

### 8.3 角色简化
`ROLE_ADMIN` 合并入 `ROLE_TEACHER`，`RoleCheckInterceptor` 对应更新。

---

## 9. 重构范围总结

| 层 | 操作 | 内容 |
|----|------|------|
| 前端 | 完全重写 | 新 UX、液态玻璃组件库、星图主题、壁纸切换 |
| 后端 Controller | 新增 | 班级/作业/审核/六维能力/进度 |
| 后端 Service/Agent | 保留+扩展 | 保留 AI 核心，新增 WebSearchAgent |
| 数据库 | 重构+新增 | 课程体系重构，8张新表 |
| 初始数据 | 新增 | 预置完整计算机科学课程 |
