---
name: 知域→AI天机学堂升级路线图状态
description: 三大阶段升级路线图的当前实施状态，标注已完成和剩余工作
type: project
---

## 当前状态 (2026-05-15)

### Phase 1：课程体系重构 + 知识图谱地基 — ✅ 已完成

- 数据库表 (sp_subject/sp_unit/sp_lesson/sp_knowledge_point/sp_kp_dependency/sp_user_kp_mastery/sp_exercise) 全部存在
- CourseServiceImpl 真实查询学科→单元→课时树
- KnowledgeGraphServiceImpl 真实实现 ELO 掌握度更新、前驱依赖分析
- CoursePlatform.vue 已接入真实 API

### Phase 2：AI 核心升级 — ✅ 已完成

- AdaptiveQuizService LLM出题+批改+错题溯源 全部真实
- AgentService 四个智能体(课程推荐/知识讲解/学情诊断/路径规划) 全部接入LLM
- AgentService.buildUserContext() 已接入 KnowledgeGraphService 获取真实用户画像
- StudyPathServiceImpl LLM生成路径+fallback
- LLMClient 统一工具类已存在(DeepSeek API)
- AIDialogBar.vue 真实 API 调用+意图路由+fallback
- AICompanionView.vue 真实聊天+SSE流+掌握度数据
- AdaptiveQuizView.vue 真实生成/提交/错题分析

### Phase 3：学习分析与游戏化 — ✅ 已完成

- LearningAnalyticsService: 真实 DB 查询 (statCards/radar/heatmap/trends/predictions)
- LearningAnalyticsService.ingestEvents(): 已实现实际 DB 更新 (sp_learning_activity)
- GamificationService: XP/等级/徽章/打卡/排行榜 全部真实
- GamificationService.checkBadgeRule(): 已实现12种规则判定
- GamificationService.leaderboard: 已关联用户表获取真实姓名
- GamificationService.getDailyChallenges(): 已返回真实进度
- analytics.js: 客户端埋点SDK已存在(页面/视频/答题/资源点击)
- 前端 API: analytics/gamification 完整覆盖
- LearningAnalyticsView.vue: 已从 API 获取真实数据
- GamificationView.vue: 已从 API 获取真实数据

### 剩余低优先级工作

- AICompanionView.vue 的 suggestions/dailyTasks 仍为硬编码（可后续从 KnowledgeGraphService/GamificationService 获取）
- KnowledgePuzzleView/KnowledgeRadarView/StyleEngineView 等创新功能的硬编码数据
- TutorServiceImplWithRAG 的 prompt engineering 持续优化
