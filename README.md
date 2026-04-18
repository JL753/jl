# 智备优教（A3 赛题）

> 赛题：基于大模型的个性化资源生成与学习多智能体系统开发

## 1. 项目概述

本项目实现一个面向高校教学场景的“多智能体 + 个性化学习资源生成”系统，围绕教师端、学生端、数据中台三大视角进行设计，结合你提供的参考视频风格进行了页面复现与功能对齐，满足今年软件杯 A3 赛题的核心要求：

- 对话式学习画像动态构建（不少于 6 个维度，当前实现 10 个维度）
- 多智能体协同生成（5 类个性化资源）
- 个性化学习路径规划与资源推送
- 智能辅导（文字 + 图解/视频建议）
- 学习效果评估与动态优化建议
- 教师端 / 学生端 / 数据中台完整演示链路

## 2. 技术栈

- 前端：Vue3 + Element Plus + ECharts + Pinia + Axios + Marked
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT
- 构建：Maven / Vite / Docker Compose

## 3. 功能结构

### 3.1 教师端

- 登录页
- 课程资源门户首页
- 教师首页数据看板
- 教学管理中心
- 教学助手（对话式学习画像 + 智能辅导）
- 资源生成中心（多智能体生成 5 类资源）
- 考试管理
- 智慧数据中台大屏

### 3.2 学生端

- 学生首页学习概览
- 语音风格学习助手
- 数字人课程讲解页
- 学习报告与错题本
- 个性化学习路径规划
- 学习效果评估
- 在线考试与提交

### 3.3 多智能体模块

当前后端已实现 5 个角色智能体：

- `SyllabusAgent`：课程讲解文档生成
- `MindMapAgent`：知识点思维导图生成
- `QuestionBankAgent`：分层练习题/测评题生成
- `MediaAgent`：多模态视频/动画脚本生成
- `CodingAgent`：代码实操案例与实践项目材料生成

## 4. 对赛题要求的映射

### 4.1 对话式学习画像自主构建

接口：`/api/profile/dialogue`

支持学生通过自然语言输入学习情况，自动抽取并构建动态画像。当前已覆盖以下维度：

- 专业
- 课程
- 知识基础
- 认知风格
- 易错点/短板
- 兴趣偏好
- 学习节奏
- 考试目标
- 学习目标
- 资源偏好
- 风险标签
- 对话总结

### 4.2 多智能体协同资源生成

接口：`/api/resources/generate`

当前系统支持一键生成以下 5 类个性化资源：

1. 专业课程讲解文档
2. 知识点思维导图
3. 分层练习题与测评题
4. 多模态视频 / 动画脚本
5. 代码实操案例与实践项目材料

### 4.3 个性化学习路径规划与资源推送

接口：`/api/path/generate`

根据学生阶段、目标与画像生成动态学习路径，路径中体现了多智能体参与过程，并为学生推送：

- 讲义精读包
- 知识脉络导图
- 分层练习题
- 短视频/动画脚本
- 代码实操任务单

### 4.4 智能辅导

接口：`/api/tutor/ask`

支持学习问题输入后输出：

- Markdown 结构化讲解
- 图解建议
- 短视频讲解建议
- 参考资料建议

### 4.5 学习效果评估

接口：`/api/assessment/evaluate`

根据学习任务完成度、刷题情况、学习时长、资源使用等数据，输出：

- 准确率
- 活跃度
- 完成度
- 资源使用度
- 学习稳定性
- 诊断结论
- 优化建议

## 5. 页面复现说明

结合你提供的参考截图，当前版本已重点复现以下视觉与业务形态：

- 科技感登录页
- 教师首页图表看板
- 学生首页学习看板
- 教师资源生成中心
- 学生学习助手页面
- 深色科技风数据中台大屏
- 教师/学生双端导航结构

说明：
当前版本已在“结构、主题、布局、交互链路、页面角色划分”上对参考项目进行高相似度实现；其中涉及真实大模型、数字人视频、语音转写、摄像头监考等能力，当前采用“可演示的系统化实现 + 接口预留 + 模拟数据输出”的方式落地，适合比赛演示与后续继续增强。

## 6. 启动方式

### 方式 A：本地启动

#### 6.1 启动 MySQL + Redis

- MySQL：`127.0.0.1:3306`，账号密码 `root/root`
- Redis：`127.0.0.1:6379`

执行数据库初始化：

```sql
source sql/init.sql;
```

#### 6.2 启动后端

```bash
cd backend
mvn spring-boot:run
```

#### 6.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

默认访问门户首页：`http://localhost:5173/portal`

后端地址：`http://localhost:8080`

### 方式 B：Docker 启动

```bash
docker compose up -d
```

## 7. 默认账号

- 管理员：admin / 123456
- 教师：teacher / 123456
- 学生：student / 123456

## 8. 已实现的基础安全与防幻觉机制

- 内容安全过滤：命中敏感词直接拦截
- 角色权限控制：基于注解的接口鉴权
- Token 鉴权：JWT
- 资源生成可信度字段输出
- 全局异常处理

## 9. 数据与知识库

当前内置示例课程：`人工智能导论`

示例知识库位置：

- `knowledge-base/artificial-intelligence-course.md`

数据库初始化脚本：

- `sql/init.sql`

## 10. 当前可继续增强的方向

若你要继续冲击更高比赛分数，建议下一步继续补：

- 对接科大讯飞星火大模型，实现真实流式输出
- 增加向量检索 / RAG，提高内容准确性
- 增加语音输入、语音播报
- 增加数字人讲解页
- 增加考试摄像头行为检测模块
- 增加资源生成进度追踪、任务队列
- 增加课程资源门户页与教师管理后台更多模块

## 11. 当前实现状态说明

当前仓库已经完成：

- 前后端工程结构
- 主要页面 UI
- 主要接口联调结构
- 赛题核心功能链路
- 可构建通过的前端工程
- 可用于比赛讲解与后续深化开发的完整基础版本

如果继续推进，我下一步可以直接再帮你补：

1. 课程资源门户首页
2. 数字人问答页
3. 语音输入按钮与交互动画
4. 教师助手页进一步复刻参考截图
5. 演示 PPT 文案
6. 7 分钟参赛演示脚本
7. 软件设计说明书 / 测试说明书模板


```
软件杯大赛
├─ .idea
│  ├─ compiler.xml
│  ├─ dataSources
│  │  ├─ 78e31b75-80f2-4f16-9cc6-7c7946d97c98
│  │  │  └─ storage_v2
│  │  │     └─ _src_
│  │  │        └─ schema
│  │  │           ├─ information_schema.FNRwLQ.meta
│  │  │           ├─ mysql.osA4Bg.meta
│  │  │           ├─ performance_schema.kIw0nw.meta
│  │  │           ├─ sakila.oXLGyQ.meta
│  │  │           ├─ sakila.oXLGyQ.zip
│  │  │           └─ sys.zb4BAA.meta
│  │  ├─ 78e31b75-80f2-4f16-9cc6-7c7946d97c98.xml
│  │  ├─ 9488f904-a176-4701-bfdf-43b1321be886
│  │  │  └─ storage_v2
│  │  │     └─ _src_
│  │  │        └─ schema
│  │  │           ├─ information_schema.FNRwLQ.meta
│  │  │           ├─ mysql.osA4Bg.meta
│  │  │           ├─ performance_schema.kIw0nw.meta
│  │  │           └─ sys.zb4BAA.meta
│  │  └─ 9488f904-a176-4701-bfdf-43b1321be886.xml
│  ├─ dataSources.local.xml
│  ├─ dataSources.xml
│  ├─ encodings.xml
│  ├─ jarRepositories.xml
│  ├─ misc.xml
│  ├─ modules.xml
│  ├─ sqldialects.xml
│  ├─ vcs.xml
│  ├─ workspace.xml
│  └─ 软件杯大赛.iml
├─ backend
│  ├─ backend-error.log
│  ├─ backend.log
│  ├─ pom.xml
│  ├─ src
│  │  └─ main
│  │     ├─ java
│  │     │  └─ com
│  │     │     ├─ cnsoftbei
│  │     │     │  └─ smartlearning
│  │     │     │     ├─ api
│  │     │     │     │  ├─ AgentGenerateRequest.java
│  │     │     │     │  ├─ ApiResponse.java
│  │     │     │     │  ├─ LoginRequest.java
│  │     │     │     │  ├─ LoginResult.java
│  │     │     │     │  ├─ ProfileBuildRequest.java
│  │     │     │     │  ├─ ResourceCard.java
│  │     │     │     │  └─ TutorAskRequest.java
│  │     │     │     ├─ controller
│  │     │     │     │  └─ AppController.java
│  │     │     │     ├─ service
│  │     │     │     │  └─ DemoDataService.java
│  │     │     │     └─ SmartLearningAgentApplication.java
│  │     │     └─ iflytek
│  │     │        └─ smartprep
│  │     │           ├─ config
│  │     │           │  ├─ AuthInterceptor.java
│  │     │           │  ├─ JwtTokenProvider.java
│  │     │           │  ├─ LoginUser.java
│  │     │           │  ├─ LoginUserHolder.java
│  │     │           │  ├─ RequireRole.java
│  │     │           │  ├─ RoleCheckInterceptor.java
│  │     │           │  ├─ SchemaInitializer.java
│  │     │           │  └─ WebConfig.java
│  │     │           ├─ controller
│  │     │           │  ├─ AdminController.java
│  │     │           │  ├─ AssessmentController.java
│  │     │           │  ├─ AuthController.java
│  │     │           │  ├─ CommonController.java
│  │     │           │  ├─ DashboardController.java
│  │     │           │  ├─ GlobalExceptionHandler.java
│  │     │           │  ├─ ProfileController.java
│  │     │           │  ├─ ResourceController.java
│  │     │           │  ├─ StudyPathController.java
│  │     │           │  ├─ TeacherToolController.java
│  │     │           │  └─ TutorController.java
│  │     │           ├─ domain
│  │     │           │  ├─ Course.java
│  │     │           │  ├─ Exam.java
│  │     │           │  ├─ ExamAssignment.java
│  │     │           │  ├─ ExamQuestion.java
│  │     │           │  ├─ ExamRecord.java
│  │     │           │  ├─ KnowledgeDoc.java
│  │     │           │  ├─ LearningAssessment.java
│  │     │           │  ├─ LearningResource.java
│  │     │           │  ├─ OperationLog.java
│  │     │           │  ├─ PptTemplate.java
│  │     │           │  ├─ QuestionBankItem.java
│  │     │           │  ├─ StudentProfile.java
│  │     │           │  ├─ StudyPath.java
│  │     │           │  ├─ User.java
│  │     │           │  └─ WrongQuestion.java
│  │     │           ├─ dto
│  │     │           │  ├─ AdminCourseCreateRequest.java
│  │     │           │  ├─ AdminCourseUpdateRequest.java
│  │     │           │  ├─ AdminUserCreateRequest.java
│  │     │           │  ├─ AdminUserUpdateRequest.java
│  │     │           │  ├─ AgentResult.java
│  │     │           │  ├─ ApiResponse.java
│  │     │           │  ├─ AssessmentRequest.java
│  │     │           │  ├─ DashboardStats.java
│  │     │           │  ├─ DialogueProfileRequest.java
│  │     │           │  ├─ ExamPublishRequest.java
│  │     │           │  ├─ ExamSubmitRequest.java
│  │     │           │  ├─ GradeExamRequest.java
│  │     │           │  ├─ LoginRequest.java
│  │     │           │  ├─ LoginResponse.java
│  │     │           │  ├─ PaperCreateRequest.java
│  │     │           │  ├─ ProfileUpdateRequest.java
│  │     │           │  ├─ QuestionItemRequest.java
│  │     │           │  ├─ RegisterRequest.java
│  │     │           │  ├─ ResourceGenerateRequest.java
│  │     │           │  ├─ StudyPathRequest.java
│  │     │           │  ├─ TeachingPptGenerateRequest.java
│  │     │           │  ├─ TutorAnswer.java
│  │     │           │  ├─ TutorAskRequest.java
│  │     │           │  └─ TutorStreamEvent.java
│  │     │           ├─ mapper
│  │     │           │  ├─ CourseMapper.java
│  │     │           │  ├─ ExamAssignmentMapper.java
│  │     │           │  ├─ ExamMapper.java
│  │     │           │  ├─ ExamQuestionMapper.java
│  │     │           │  ├─ ExamRecordMapper.java
│  │     │           │  ├─ KnowledgeDocMapper.java
│  │     │           │  ├─ LearningAssessmentMapper.java
│  │     │           │  ├─ LearningResourceMapper.java
│  │     │           │  ├─ OperationLogMapper.java
│  │     │           │  ├─ PptTemplateMapper.java
│  │     │           │  ├─ QuestionBankItemMapper.java
│  │     │           │  ├─ StudentProfileMapper.java
│  │     │           │  ├─ StudyPathMapper.java
│  │     │           │  ├─ UserMapper.java
│  │     │           │  └─ WrongQuestionMapper.java
│  │     │           ├─ service
│  │     │           │  ├─ agent
│  │     │           │  │  ├─ Agent.java
│  │     │           │  │  ├─ CodingAgent.java
│  │     │           │  │  ├─ MediaAgent.java
│  │     │           │  │  ├─ MindMapAgent.java
│  │     │           │  │  ├─ QuestionBankAgent.java
│  │     │           │  │  └─ SyllabusAgent.java
│  │     │           │  ├─ AssessmentService.java
│  │     │           │  ├─ AuthService.java
│  │     │           │  ├─ DashboardService.java
│  │     │           │  ├─ impl
│  │     │           │  │  ├─ AssessmentServiceImpl.java
│  │     │           │  │  ├─ AuthServiceImpl.java
│  │     │           │  │  ├─ DashboardServiceImpl.java
│  │     │           │  │  ├─ ProfileServiceImpl.java
│  │     │           │  │  ├─ ResourceServiceImpl.java
│  │     │           │  │  ├─ StudyPathServiceImpl.java
│  │     │           │  │  ├─ TeacherToolServiceImpl.java
│  │     │           │  │  └─ TutorServiceImpl.java
│  │     │           │  ├─ ProfileService.java
│  │     │           │  ├─ ResourceService.java
│  │     │           │  ├─ StudyPathService.java
│  │     │           │  ├─ TeacherToolService.java
│  │     │           │  └─ TutorService.java
│  │     │           └─ SmartPrepApplication.java
│  │     └─ resources
│  │        └─ application.yml
│  └─ target
│     ├─ classes
│     │  └─ com
│     │     ├─ cnsoftbei
│     │     │  └─ smartlearning
│     │     │     ├─ api
│     │     │     │  ├─ AgentGenerateRequest.class
│     │     │     │  ├─ ApiResponse.class
│     │     │     │  ├─ LoginRequest.class
│     │     │     │  ├─ LoginResult.class
│     │     │     │  ├─ ProfileBuildRequest.class
│     │     │     │  ├─ ResourceCard.class
│     │     │     │  └─ TutorAskRequest.class
│     │     │     ├─ controller
│     │     │     │  └─ AppController.class
│     │     │     ├─ service
│     │     │     │  └─ DemoDataService.class
│     │     │     └─ SmartLearningAgentApplication.class
│     │     └─ iflytek
│     │        └─ smartprep
│     │           ├─ config
│     │           │  ├─ AuthInterceptor.class
│     │           │  ├─ JwtTokenProvider.class
│     │           │  ├─ LoginUser$LoginUserBuilder.class
│     │           │  ├─ LoginUser.class
│     │           │  ├─ LoginUserHolder.class
│     │           │  ├─ RequireRole.class
│     │           │  ├─ RoleCheckInterceptor.class
│     │           │  ├─ SchemaInitializer.class
│     │           │  └─ WebConfig.class
│     │           ├─ controller
│     │           │  ├─ AdminController.class
│     │           │  ├─ AssessmentController.class
│     │           │  ├─ AuthController.class
│     │           │  ├─ CommonController.class
│     │           │  ├─ DashboardController.class
│     │           │  ├─ GlobalExceptionHandler.class
│     │           │  ├─ ProfileController.class
│     │           │  ├─ ResourceController.class
│     │           │  ├─ StudyPathController.class
│     │           │  ├─ TeacherToolController.class
│     │           │  └─ TutorController.class
│     │           ├─ domain
│     │           │  ├─ Course.class
│     │           │  ├─ Exam.class
│     │           │  ├─ ExamAssignment.class
│     │           │  ├─ ExamQuestion.class
│     │           │  ├─ ExamRecord.class
│     │           │  ├─ KnowledgeDoc.class
│     │           │  ├─ LearningAssessment.class
│     │           │  ├─ LearningResource.class
│     │           │  ├─ OperationLog.class
│     │           │  ├─ PptTemplate.class
│     │           │  ├─ QuestionBankItem.class
│     │           │  ├─ StudentProfile.class
│     │           │  ├─ StudyPath.class
│     │           │  ├─ User.class
│     │           │  └─ WrongQuestion.class
│     │           ├─ dto
│     │           │  ├─ AdminCourseCreateRequest$ChapterItem.class
│     │           │  ├─ AdminCourseCreateRequest.class
│     │           │  ├─ AdminCourseUpdateRequest.class
│     │           │  ├─ AdminUserCreateRequest.class
│     │           │  ├─ AdminUserUpdateRequest.class
│     │           │  ├─ AgentResult$AgentResultBuilder.class
│     │           │  ├─ AgentResult.class
│     │           │  ├─ ApiResponse.class
│     │           │  ├─ AssessmentRequest.class
│     │           │  ├─ DashboardStats$DashboardStatsBuilder.class
│     │           │  ├─ DashboardStats.class
│     │           │  ├─ DialogueProfileRequest.class
│     │           │  ├─ ExamPublishRequest.class
│     │           │  ├─ ExamSubmitRequest$AnswerItem.class
│     │           │  ├─ ExamSubmitRequest.class
│     │           │  ├─ GradeExamRequest$QuestionAnnotation.class
│     │           │  ├─ GradeExamRequest.class
│     │           │  ├─ LoginRequest.class
│     │           │  ├─ LoginResponse$LoginResponseBuilder.class
│     │           │  ├─ LoginResponse.class
│     │           │  ├─ PaperCreateRequest.class
│     │           │  ├─ ProfileUpdateRequest.class
│     │           │  ├─ QuestionItemRequest.class
│     │           │  ├─ RegisterRequest.class
│     │           │  ├─ ResourceGenerateRequest.class
│     │           │  ├─ StudyPathRequest.class
│     │           │  ├─ TeachingPptGenerateRequest.class
│     │           │  ├─ TutorAnswer$TutorAnswerBuilder.class
│     │           │  ├─ TutorAnswer.class
│     │           │  ├─ TutorAskRequest.class
│     │           │  ├─ TutorStreamEvent$TutorStreamEventBuilder.class
│     │           │  └─ TutorStreamEvent.class
│     │           ├─ mapper
│     │           │  ├─ CourseMapper.class
│     │           │  ├─ ExamAssignmentMapper.class
│     │           │  ├─ ExamMapper.class
│     │           │  ├─ ExamQuestionMapper.class
│     │           │  ├─ ExamRecordMapper.class
│     │           │  ├─ KnowledgeDocMapper.class
│     │           │  ├─ LearningAssessmentMapper.class
│     │           │  ├─ LearningResourceMapper.class
│     │           │  ├─ OperationLogMapper.class
│     │           │  ├─ PptTemplateMapper.class
│     │           │  ├─ QuestionBankItemMapper.class
│     │           │  ├─ StudentProfileMapper.class
│     │           │  ├─ StudyPathMapper.class
│     │           │  ├─ UserMapper.class
│     │           │  └─ WrongQuestionMapper.class
│     │           ├─ service
│     │           │  ├─ agent
│     │           │  │  ├─ Agent.class
│     │           │  │  ├─ CodingAgent.class
│     │           │  │  ├─ MediaAgent.class
│     │           │  │  ├─ MindMapAgent.class
│     │           │  │  ├─ QuestionBankAgent.class
│     │           │  │  └─ SyllabusAgent.class
│     │           │  ├─ AssessmentService.class
│     │           │  ├─ AuthService.class
│     │           │  ├─ DashboardService.class
│     │           │  ├─ impl
│     │           │  │  ├─ AssessmentServiceImpl$1.class
│     │           │  │  ├─ AssessmentServiceImpl.class
│     │           │  │  ├─ AuthServiceImpl.class
│     │           │  │  ├─ DashboardServiceImpl.class
│     │           │  │  ├─ ProfileServiceImpl$1.class
│     │           │  │  ├─ ProfileServiceImpl.class
│     │           │  │  ├─ ResourceServiceImpl$1.class
│     │           │  │  ├─ ResourceServiceImpl.class
│     │           │  │  ├─ StudyPathServiceImpl$1.class
│     │           │  │  ├─ StudyPathServiceImpl.class
│     │           │  │  ├─ TeacherToolServiceImpl.class
│     │           │  │  └─ TutorServiceImpl.class
│     │           │  ├─ ProfileService.class
│     │           │  ├─ ResourceService.class
│     │           │  ├─ StudyPathService.class
│     │           │  ├─ TeacherToolService.class
│     │           │  └─ TutorService.class
│     │           └─ SmartPrepApplication.class
│     ├─ generated-sources
│     │  └─ annotations
│     ├─ generated-test-sources
│     │  └─ test-annotations
│     ├─ maven-status
│     │  └─ maven-compiler-plugin
│     │     └─ compile
│     │        └─ default-compile
│     │           ├─ createdFiles.lst
│     │           └─ inputFiles.lst
│     └─ test-classes
├─ docker-compose.yml
├─ frontend
│  ├─ .husky
│  │  └─ pre-commit
│  ├─ auto-imports.d.ts
│  ├─ components.d.ts
│  ├─ dist
│  │  ├─ assets
│  │  │  ├─ AdminLayout-C9CIPO6p.css
│  │  │  ├─ AdminLayout-CmPnRYrk.js
│  │  │  ├─ DataCenter-CEIi5Jmx.css
│  │  │  ├─ DataCenter-Df3lG4tX.js
│  │  │  ├─ DigitalHumanView-BIWkbAGO.js
│  │  │  ├─ DigitalHumanView-Dss1c5T0.css
│  │  │  ├─ index-Bb6yjXMn.js
│  │  │  ├─ index-BnyEoamd.css
│  │  │  ├─ index-ITr9GRMS.js
│  │  │  ├─ LoginView-BoQekaFr.css
│  │  │  ├─ LoginView-CL5TCd7Z.js
│  │  │  ├─ PortalHome-7Yhe_BY6.css
│  │  │  ├─ PortalHome-BtvAZhhU.js
│  │  │  ├─ ProfileView-Bl6LBry9.js
│  │  │  ├─ ProfileView-BMKZzL5K.css
│  │  │  ├─ ResourceManage-BbachVcG.css
│  │  │  ├─ ResourceManage-BGdIcTJg.js
│  │  │  ├─ sse-CVqQ8BoL.js
│  │  │  ├─ StudentAssistant-CmBGkJzb.js
│  │  │  ├─ StudentAssistant-DfBjqN7f.css
│  │  │  ├─ StudentDashboard-BgG11HMg.css
│  │  │  ├─ StudentDashboard-C9WZh6dY.js
│  │  │  ├─ StudentExam-1np53nHc.css
│  │  │  ├─ StudentExam-Bv4Y4ltU.js
│  │  │  ├─ StudentLayout-DBWvBjat.css
│  │  │  ├─ StudentLayout-D_b20UKZ.js
│  │  │  ├─ StudentReport-CRg_jO2k.js
│  │  │  ├─ StudentReport-DQTwdiu3.css
│  │  │  ├─ StudyPathView-Bokzf8PD.js
│  │  │  ├─ StudyPathView-Dp6nEOc2.css
│  │  │  ├─ TeacherAssistant-C4vsJVLa.js
│  │  │  ├─ TeacherAssistant-CiVz3frk.css
│  │  │  ├─ TeacherDashboard-BIdcONH-.css
│  │  │  ├─ TeacherDashboard-f5KktYpR.js
│  │  │  ├─ TeacherExam-G6QeRCig.css
│  │  │  ├─ TeacherExam-wMho2THf.js
│  │  │  ├─ TeacherLayout-BFSZQHPq.js
│  │  │  ├─ TeacherLayout-WHdj2uYL.css
│  │  │  ├─ TeacherManage-BnSJivUj.js
│  │  │  └─ TeacherManage-H-Wvf4CH.css
│  │  └─ index.html
│  ├─ frontend-error.log
│  ├─ frontend.log
│  ├─ index.html
│  ├─ package-lock.json
│  ├─ package.json
│  ├─ src
│  │  ├─ api
│  │  │  ├─ http.js
│  │  │  ├─ index.js
│  │  │  └─ sse.js
│  │  ├─ App.vue
│  │  ├─ layout
│  │  │  ├─ AdminLayout.vue
│  │  │  ├─ StudentLayout.vue
│  │  │  └─ TeacherLayout.vue
│  │  ├─ main.js
│  │  ├─ router
│  │  │  └─ index.js
│  │  ├─ stores
│  │  │  └─ auth.js
│  │  ├─ styles
│  │  │  └─ global.css
│  │  ├─ styles.css
│  │  └─ views
│  │     ├─ admin
│  │     │  ├─ AdminDashboard.vue
│  │     │  ├─ CourseManagement.vue
│  │     │  ├─ ExamManagement.vue
│  │     │  ├─ LogManagement.vue
│  │     │  ├─ ResourceManagement.vue
│  │     │  ├─ SystemSettings.vue
│  │     │  └─ UserManagement.vue
│  │     ├─ auth
│  │     │  └─ LoginView.vue
│  │     ├─ common
│  │     │  ├─ datacenter
│  │     │  │  ├─ datacenter-shared.css
│  │     │  │  ├─ DataCenterDepartmentLevelTab.vue
│  │     │  │  ├─ DataCenterDepartmentPeopleTab.vue
│  │     │  │  ├─ DataCenterSchoolTab.vue
│  │     │  │  └─ DataCenterStudentTab.vue
│  │     │  ├─ DataCenter.vue
│  │     │  ├─ PortalHome.vue
│  │     │  └─ ProfileView.vue
│  │     ├─ student
│  │     │  ├─ DigitalHumanView.vue
│  │     │  ├─ StudentAssistant.vue
│  │     │  ├─ StudentDashboard.vue
│  │     │  ├─ StudentExam.vue
│  │     │  ├─ StudentReport.vue
│  │     │  └─ StudyPathView.vue
│  │     └─ teacher
│  │        ├─ ResourceManage.vue
│  │        ├─ TeacherAssistant.vue
│  │        ├─ TeacherDashboard.vue
│  │        ├─ TeacherExam.vue
│  │        └─ TeacherManage.vue
│  └─ vite.config.js
├─ knowledge-base
│  └─ artificial-intelligence-course.md
├─ README.md
├─ smart-learning-system
│  └─ backend
├─ sql
│  └─ init.sql
├─ temp_patch1.py
├─ test.txt
└─ 前端
   └─ src
      └─ views
         └─ admin
            └─ SystemSettings.vue

```"# -" 
