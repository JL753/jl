# 知域智备优教 (SmartPrep) — 系统架构文档

> 生成日期: 2026-06-14 | 分支: master

---

## 目录

1. [系统架构图](#1-系统架构图)
2. [数据库 ER 图](#2-数据库-er-图)
3. [AI 对话与 RAG 数据流](#3-ai-对话与-rag-数据流)
4. [前端组件架构](#4-前端组件架构)
5. [部署架构](#5-部署架构)

---

## 1. 系统架构图

```mermaid
graph TB
    subgraph 客户端层["🖥️ 客户端层"]
        Browser["浏览器客户端<br/>Vue 3 SPA + WebGL<br/>Unity 3D 虚拟人"]
    end

    subgraph 网关层["🔐 网关/安全层"]
        Nginx["Nginx 反向代理<br/>静态资源托管 + API 转发"]
        JWT_Auth["JWT 认证拦截器<br/>AuthInterceptor<br/>RoleCheckInterceptor"]
    end

    subgraph 应用层["⚙️ 应用层 (Spring Boot 3.2.5)"]
        subgraph 门户认证["🔑 门户与认证"]
            AuthCtrl["用户认证<br/>登录 · 注册 · 用户信息<br/>JWT Token 签发"]
            CommonCtrl["门户首页<br/>Hero 统计 · 课程列表<br/>数据中心大屏"]
        end

        subgraph 教学核心["📚 教学核心"]
            CourseCtrl["课程体系管理<br/>学科 → 课程 → 章 → 节<br/>资源 · 公告 · Q&A"]
            DashboardCtrl["教学仪表盘<br/>教师/学生工作台<br/>考试管理 · 成绩榜"]
            ScheduleCtrl["周课程表<br/>排课 · 调课"]
            ClassCtrl["班级管理<br/>班级创建 · 成员管理<br/>作业布置 · 批阅"]
        end

        subgraph AI引擎["🤖 AI 引擎层"]
            TutorSvc["RAG 增强 AI 导师<br/>TutorServiceImplWithRAG<br/>流式 + 非流式回答<br/>文档检索增强生成"]
            AgentSvc["多智能体路由<br/>AgentService<br/>课程推荐 · 知识讲解<br/>学情诊断 · 路径规划<br/>Unity 虚拟人对话"]
            LLMClient["大模型调用客户端<br/>LLMClient<br/>OpenAI 兼容 HTTP API<br/>OkHttp 流式/非流式"]
            RAG系统["RAG 检索增强生成<br/>文档解析 · 切片<br/>向量化 · 语义检索<br/>Apache PDFBox/POI/Tika<br/>Chroma VectorDB"]
            多智能体["多智能体资源生成<br/>讲义生成 · 题库构建<br/>思维导图 · 视频脚本<br/>编程实验 · 联网搜索"]
            语音服务["语音交互服务<br/>TTS 语音合成<br/>STT 语音识别<br/>Web Speech API 前端实现"]
        end

        subgraph 知识图谱["🧠 知识图谱"]
            KGService["知识点掌握分析<br/>KnowledgeGraphService<br/>ELO 算法追踪<br/>薄弱点识别 · 推荐"]
            NeoSync["图谱同步服务<br/>KnowledgeGraphSyncService<br/>MySQL → Neo4j 同步<br/>学科/课程/知识点/依赖"]
        end

        subgraph 数据分析["📊 数据分析"]
            AnalyticsCtrl["学习行为追踪<br/>AnalyticsController<br/>活动记录 · 任务完成<br/>学习时长统计"]
            AbilityCtrl["六维能力评估<br/>StudentAbilityController<br/>广度 · 深度 · 解题<br/>活跃 · 迁移 · 韧性"]
            Gamification["游戏化激励<br/>GamificationService<br/>XP 经验 · 等级 · 徽章<br/>打卡 · 排行榜 · 挑战"]
        end

        subgraph 内容工具["🛠️ 内容工具"]
            Bilibili导入["Bilibili 视频导入<br/>视频解析 · AI 分类<br/>讲义/习题自动生成<br/>视频 → 课程自动化"]
            教师工具["教师工具箱<br/>TeacherToolService<br/>题库管理 · 组卷<br/>PPT 模板 · AI 课件"]
            自适应测验["自适应测验<br/>AdaptiveQuizService<br/>难度自适应出题<br/>错因智能分析"]
        end
    end

    subgraph 数据层["🗄️ 数据层"]
        MySQL[("MySQL 8.0<br/>主业务数据库<br/>46 张表 · 63 外键")]
        Redis[("Redis 7<br/>缓存加速<br/>Session 管理")]
        Neo4j[("Neo4j 5 Community<br/>知识图谱存储<br/>知识点依赖关系")]
        Chroma[("Chroma VectorDB<br/>文档向量存储<br/>语义相似度检索")]
    end

    subgraph 外部服务["🌐 外部服务"]
        LLM_API["DeepSeek API<br/>OpenAI 兼容协议<br/>对话补全 / 流式输出"]
        BaiduSearch["百度千帆 AI 搜索<br/>外部学习资源检索"]
        BilibiliAPI["Bilibili API<br/>视频元数据获取"]
        EmbeddingAPI["Embedding API<br/>文本向量化<br/>OpenAI 兼容接口"]
    end

    %% 客户端 → 网关
    Browser --> Nginx
    Nginx --> JWT_Auth

    %% 网关 → 应用层
    JWT_Auth --> AuthCtrl
    JWT_Auth --> CommonCtrl
    JWT_Auth --> CourseCtrl
    JWT_Auth --> DashboardCtrl
    JWT_Auth --> ScheduleCtrl
    JWT_Auth --> ClassCtrl

    %% 应用层内部依赖
    DashboardCtrl --> TutorSvc
    CourseCtrl --> CourseCtrl

    TutorSvc --> RAG系统
    TutorSvc --> LLMClient
    AgentSvc --> LLMClient
    AgentSvc --> KGService
    多智能体 --> LLMClient
    RAG系统 --> LLMClient
    AnalyticsCtrl --> LLMClient

    LLMClient --> LLM_API
    RAG系统 --> Chroma
    RAG系统 --> EmbeddingAPI
    KGService --> Neo4j
    NeoSync --> Neo4j
    NeoSync --> MySQL
    Bilibili导入 --> LLMClient
    Bilibili导入 --> BilibiliAPI
    BaiduSearch --> BaiduSearch

    %% 应用层 → 数据层
    AuthCtrl --> MySQL
    CourseCtrl --> MySQL
    DashboardCtrl --> MySQL
    KGService --> MySQL
    Gamification --> MySQL
    自适应测验 --> MySQL
    教师工具 --> MySQL
    AnalyticsCtrl --> MySQL
    RAG系统 --> MySQL

    AuthCtrl --> Redis
    DashboardCtrl --> Redis
```

---

## 2. 数据库 ER 图

### 2.1 核心教学域 — 用户 · 课程 · 知识体系

```mermaid
erDiagram
    sp_user ||--o{ sp_course : "创建课程 (教师)"
    sp_user ||--o{ sp_student_profile : "拥有画像"
    sp_user ||--o{ sp_exam_record : "提交考试"
    sp_user ||--o{ sp_qa_history : "发起提问"
    sp_user ||--o{ sp_user_xp : "积累经验"
    sp_user ||--o{ sp_user_streak : "连续打卡"
    sp_user ||--o{ sp_user_badge : "获得徽章"
    sp_user ||--o{ sp_user_kp_mastery : "掌握知识点"

    sp_subject ||--o{ sp_course : "包含课程"
    sp_course ||--o{ sp_chapter : "划分章"
    sp_chapter ||--o{ sp_sub_chapter : "包含节"
    sp_course ||--o{ sp_sub_chapter : "直接包含节"

    sp_sub_chapter ||--o{ sp_lesson_progress : "记录进度"
    sp_user ||--o{ sp_lesson_progress : "完成学习"

    sp_sub_chapter ||--o{ sp_knowledge_point : "涵盖知识点"
    sp_knowledge_point ||--o{ sp_kp_dependency : "前置依赖"
    sp_knowledge_point ||--o{ sp_exercise : "配套练习"
    sp_user ||--o{ sp_exercise_attempt : "作答练习"
    sp_exercise ||--o{ sp_exercise_attempt : "记录答题"

    sp_lesson ||--o{ sp_knowledge_point : "涵盖知识点 (旧版)"
    sp_unit ||--o{ sp_lesson : "组织课时 (旧版)"
    sp_subject ||--o{ sp_unit : "组织单元 (旧版)"

    sp_user ||--o{ sp_study_path : "生成学习路径"
    sp_user ||--o{ sp_learning_assessment : "接受评估"
    sp_user ||--o{ sp_learning_resource : "获取学习资源"

    sp_user {
        bigint id PK "用户ID"
        varchar username UK "用户名"
        varchar password "密码"
        varchar role "角色 admin|teacher|student"
        varchar display_name "显示名称"
        varchar avatar_url "头像地址"
    }

    sp_subject {
        bigint id PK "学科ID"
        varchar name "学科名称"
        varchar icon "图标"
        varchar color "主题色"
        varchar description "描述"
        int sort_order "排序"
    }

    sp_course {
        bigint id PK "课程ID"
        bigint subject_id FK "所属学科"
        varchar title "课程标题"
        varchar category "分类"
        text description "描述"
        varchar difficulty "难度等级"
        bigint created_by FK "创建者"
        datetime created_at "创建时间"
    }

    sp_chapter {
        bigint id PK "章ID"
        bigint course_id FK "所属课程"
        varchar title "章标题"
        text description "描述"
        int sort_order "排序"
    }

    sp_sub_chapter {
        bigint id PK "节ID"
        bigint chapter_id FK "所属章"
        bigint course_id FK "所属课程"
        varchar title "节标题"
        varchar type "类型 video|document|quiz"
        text content "内容 (Markdown)"
    }

    sp_knowledge_point {
        bigint id PK "知识点ID"
        bigint sub_chapter_id FK "所属节"
        varchar name "知识点名称"
        text description "描述"
        varchar difficulty_level "难度等级"
    }

    sp_kp_dependency {
        bigint id PK "依赖ID"
        bigint kp_id FK "知识点"
        bigint depends_on_kp_id FK "前置知识点"
    }

    sp_exercise {
        bigint id PK "练习ID"
        bigint knowledge_point_id FK "关联知识点"
        varchar type "题型 choice|blank|code"
        varchar difficulty "难度"
        json content_json "题目内容"
        text answer "答案"
    }

    sp_user_kp_mastery {
        bigint id PK "掌握记录ID"
        bigint user_id FK "用户"
        bigint knowledge_point_id FK "知识点"
        float mastery "掌握度 0.0~1.0"
        int practice_count "练习次数"
        datetime last_practice_at "最近练习"
    }
```

### 2.2 考试与班级域 — 测评 · 组班 · 作业

```mermaid
erDiagram
    sp_user ||--o{ sp_exam : "创建考试 (教师)"
    sp_exam ||--o{ sp_exam_question : "包含试题"
    sp_exam ||--o{ sp_exam_assignment : "分配给"
    sp_user ||--o{ sp_exam_assignment : "被分配 (学生)"
    sp_user ||--o{ sp_exam_record : "完成考试"
    sp_exam ||--o{ sp_exam_record : "考试记录"
    sp_exam_record ||--o{ sp_wrong_question : "生成错题"
    sp_exam_question ||--o{ sp_wrong_question : "错题关联"

    sp_user ||--o{ sp_class : "创建班级 (教师)"
    sp_class ||--o{ sp_class_member : "包含成员"
    sp_user ||--o{ sp_class_member : "加入班级 (学生)"
    sp_class ||--o{ sp_assignment : "布置作业"
    sp_assignment ||--o{ sp_assignment_submission : "收到提交"
    sp_user ||--o{ sp_assignment_submission : "提交作业 (学生)"

    sp_exam {
        bigint id PK "考试ID"
        bigint creator_user_id FK "创建者"
        varchar exam_name "考试名称"
        varchar course "关联课程"
        int duration "时长 (分钟)"
        varchar status "状态"
        int question_count "题目数量"
    }

    sp_exam_question {
        bigint id PK "试题ID"
        bigint exam_id FK "所属考试"
        int question_no "题号"
        varchar question_type "题型"
        text title "题目内容"
        int score "分值"
        text answer_key "参考答案"
    }

    sp_exam_record {
        bigint id PK "考试记录ID"
        bigint exam_id FK "考试"
        bigint user_id FK "考生"
        int score "得分"
        text review "教师评语"
        json answers_json "答题详情"
        datetime submitted_at "提交时间"
    }

    sp_wrong_question {
        bigint id PK "错题ID"
        bigint user_id FK "用户"
        bigint exam_record_id FK "考试记录"
        bigint exam_question_id FK "原题"
        text question_title "题目"
        text my_answer "我的答案"
        text correct_answer "正确答案"
        text analysis "错因分析"
    }

    sp_class {
        bigint id PK "班级ID"
        varchar name "班级名称"
        bigint teacher_id FK "教师"
        varchar invite_code UK "邀请码"
        text description "班级描述"
    }

    sp_class_member {
        bigint id PK "成员ID"
        bigint class_id FK "班级"
        bigint student_id FK "学生"
    }

    sp_assignment {
        bigint id PK "作业ID"
        bigint class_id FK "班级"
        varchar title "作业标题"
        text description "作业描述"
        datetime due_at "截止时间"
    }

    sp_assignment_submission {
        bigint id PK "提交ID"
        bigint assignment_id FK "作业"
        bigint student_id FK "学生"
        varchar status "状态"
        int score "得分"
        text feedback "反馈"
    }
```

### 2.3 游戏化与分析域 — 激励 · 能力 · 问答

```mermaid
erDiagram
    sp_user ||--|| sp_user_xp : "经验等级"
    sp_user ||--|| sp_user_streak : "连续打卡"
    sp_user ||--o{ sp_user_badge : "获得徽章"
    sp_badge_def ||--o{ sp_user_badge : "徽章定义"

    sp_user ||--o{ sp_learning_activity : "学习活动"
    sp_user ||--o{ sp_task_completion : "任务完成"
    sp_user ||--o{ sp_study_duration : "学习时长"
    sp_user ||--o{ sp_student_ability : "能力评估"

    sp_user ||--o{ sp_course_question : "发起提问"
    sp_course ||--o{ sp_course_question : "课程讨论"
    sp_course_question ||--o{ sp_course_answer : "收到回答"
    sp_user ||--o{ sp_course_answer : "回答问题"

    sp_user_xp {
        bigint id PK "经验ID"
        bigint user_id FK_UK "用户"
        int current_xp "当前经验值"
        int level "等级"
        varchar title "称号"
    }

    sp_user_streak {
        bigint id PK "打卡ID"
        bigint user_id FK_UK "用户"
        int current_streak "当前连续天数"
        int longest_streak "最长连续天数"
        date last_checkin_date "最后打卡日期"
        int total_checkins "总打卡次数"
    }

    sp_badge_def {
        bigint id PK "徽章定义ID"
        varchar name "徽章名称"
        text description "描述"
        varchar icon "图标"
        varchar color "颜色"
        text unlock_rule "解锁规则"
    }

    sp_user_badge {
        bigint id PK "记录ID"
        bigint user_id FK "用户"
        bigint badge_id FK "徽章"
        datetime unlocked_at "解锁时间"
    }

    sp_student_ability {
        bigint id PK "能力评估ID"
        bigint user_id FK "用户"
        float breadth_score "广度得分"
        float depth_score "深度得分"
        float problem_score "解题得分"
        float activity_score "活跃得分"
        float transfer_score "迁移得分"
        float resilience_score "韧性得分"
        text diagnosis "综合诊断"
    }

    sp_learning_activity {
        bigint id PK "活动ID"
        bigint user_id FK "用户"
        date activity_date "活动日期"
        int activity_score "活跃分"
        int login_count "登录次数"
        int study_minutes "学习分钟数"
        int question_count "答题数量"
    }

    sp_course_question {
        bigint id PK "提问ID"
        bigint course_id FK "课程"
        bigint user_id FK "提问者"
        varchar title "问题标题"
        text content "问题内容"
    }

    sp_course_answer {
        bigint id PK "回答ID"
        bigint question_id FK "问题"
        bigint user_id FK "回答者"
        text content "回答内容"
        tinyint is_ai "是否AI回答"
    }
```

---

## 3. AI 对话与 RAG 数据流

```mermaid
sequenceDiagram
    actor 用户 as 学生/教师
    participant 前端 as Vue 前端<br/>AICompanionImmersive<br/>SplitPaneStudy
    participant AgentCtrl as AgentController<br/>智能体路由
    participant TutorSvc as TutorServiceImplWithRAG<br/>RAG 增强导师
    participant RAG组件 as RAGService<br/>检索增强生成
    participant 向量库 as Chroma VectorDB<br/>文档向量存储
    participant LLM客户端 as LLMClient<br/>OkHttp 流式/非流式
    participant 数据库 as MySQL<br/>sp_qa_history

    用户->>前端: 输入问题 / 点击发送
    前端->>前端: useChatStore.addUserMessage()

    alt 虚拟人对话 — 小慧 (AI Companion)
        前端->>AgentCtrl: POST /api/agent/chat-stream<br/>{question, history, sessionId}
        AgentCtrl->>AgentCtrl: JWT 解析 userId
        AgentCtrl->>LLM客户端: chatStreamWithCompanion<br/>系统提示词 + 历史 + 问题
        LLM客户端-->>AgentCtrl: SSE 流式返回 delta chunks
        AgentCtrl-->>前端: SSE event stream<br/>delta → 文本增量<br/>finish → 对话完成
        AgentCtrl->>数据库: 保存问答历史<br/>question + answer + sessionId
    else RAG 增强辅导 (AI Tutor)
        前端->>TutorSvc: POST /api/tutor/ask<br/>{question, context, sessionId}
        TutorSvc->>RAG组件: retrieveRelevantDocuments<br/>question, course, topK
        RAG组件->>向量库: Embedding → 语义相似度检索
        向量库-->>RAG组件: 相关文档片段列表
        RAG组件-->>TutorSvc: List of RetrievalResult
        TutorSvc->>TutorSvc: buildUserPromptWithRAG<br/>检索结果 + 用户问题拼接
        TutorSvc->>LLM客户端: chat completions<br/>系统提示词 + RAG 上下文 + 问题
        LLM客户端-->>TutorSvc: Markdown 格式回答
        TutorSvc->>数据库: saveQaHistory<br/>userId, question, answer, sessionId
        TutorSvc-->>前端: TutorAnswer<br/>{markdown, citations, diagrams}
    end

    前端->>前端: parseStructuredReply<br/>提取 emotion/action/text/command
    前端->>前端: speakText → Web Speech API 朗读
    前端->>前端: buildLipSyncWav → AudioContext<br/>注入 uLipSync 节点 → 口型动画
    前端->>前端: chatStore.addAssistantMessage
```

---

## 4. 前端组件架构

```mermaid
graph TB
    subgraph 根组件["App.vue 根组件"]
        subgraph 路由系统["Vue Router 路由"]
            门户首页["/ → PortalHome.vue<br/>平台门户首页"]
            学生端["/student/* → StudentLayout.vue<br/>学生端布局 + 导航"]
            教师端["/teacher/* → TeacherLayout.vue<br/>教师/管理员布局 + 导航"]
        end

        subgraph 全局共享组件["全局共享组件"]
            登录弹窗["LoginModal<br/>登录/注册弹窗"]
            顶栏导航["TopNavBar<br/>顶部导航栏"]
            AI悬浮球["AIFloatingBall<br/>AI 智能助手悬浮球"]
            AI对话栏["AIDialogBar<br/>AI 对话输入栏"]
            壁纸设置["WallpaperModal<br/>壁纸主题设置"]
        end

        subgraph 状态管理["Pinia 状态管理"]
            AuthStore["useAuthStore<br/>token · 用户信息<br/>登录 · 注册 · 登出"]
            ChatStore["useChatStore<br/>对话消息 · 会话ID<br/>历史加载 · 口型同步"]
            BgStore["useBackgroundStore<br/>壁纸主题 · 亮度控制"]
            KMStore["useKnowledgeMapStore<br/>知识图谱状态"]
        end

        subgraph 接口层["API 接口层"]
            HTTP客户端["http.js Axios 实例<br/>JWT 拦截器 · 401 处理"]
            接口模块["auth | course | dashboard<br/>bilibili | graph | study<br/>manage | qa | sse"]
        end

        subgraph 学生端页面["学生端页面"]
            学生仪表盘["StudentDashboard<br/>学习数据概览"]
            沉浸伴学["AICompanionImmersive<br/>Unity 3D 虚拟人 + AI 对话"]
            课程学习["CourseView<br/>SplitPaneStudy 分屏学习<br/>资源预览 + AI 助手"]
            知识星图["KnowledgeStarMap<br/>ECharts 力导向图<br/>知识点图谱可视化"]
            在线考试["StudentExam<br/>在线考试与成绩查看"]
            问答社区["CommunityView<br/>课程问答广场"]
            学习分析["LearningAnalyticsView<br/>学习行为分析报告"]
        end

        subgraph 教师端页面["教师端页面"]
            教师仪表盘["TeacherDashboard<br/>教学概览"]
            AI备课助手["TeacherAssistant<br/>AI 智能备课"]
            题库管理["TeacherManage<br/>题库 · 组卷 · PPT"]
            班级管理["ClassManagement<br/>班级创建 · 成员管理"]
            内容管理["ContentManagement<br/>课程内容编排"]
            AI内容审核["AIContentReview<br/>AI 生成内容审核"]
        end
    end

    根组件 --> 路由系统
    根组件 --> 全局共享组件
    根组件 --> 状态管理

    路由系统 --> 学生端页面
    路由系统 --> 教师端页面

    状态管理 --> 接口层
    学生端页面 --> 接口层
    教师端页面 --> 接口层

    接口层 --> HTTP客户端
    HTTP客户端 -->|"/api/*"| 后端服务["Spring Boot 后端<br/>localhost : 8080"]
```

---

## 5. 部署架构

```mermaid
graph TB
    subgraph Docker环境["Docker Compose 容器编排"]
        subgraph 服务容器["服务容器 (5 个)"]
            MySQL容器["MySQL 8.0<br/>端口 3306<br/>数据卷: mysql_data"]
            Redis容器["Redis 7<br/>端口 6379<br/>缓存 / Session"]
            Neo4j容器["Neo4j 5 Community<br/>HTTP 7474 · Bolt 7687<br/>数据卷: neo4j_data"]
            后端容器["Spring Boot 3.2.5<br/>端口 8080<br/>Dockerfile 构建"]
            前端容器["Nginx 反向代理<br/>端口 80 → Vite 5173<br/>Dockerfile 构建"]
        end

        subgraph 持久化存储["持久化数据卷"]
            MySQL卷["mysql_data<br/>业务数据持久化"]
            Neo4j卷["neo4j_data<br/>图谱数据持久化"]
            上传卷["uploads<br/>文档/图片上传"]
        end
    end

    subgraph 外部依赖["外部 API 依赖"]
        DeepSeek["DeepSeek API<br/>api.deepseek.com/v1<br/>大语言模型调用"]
        Embedding["Embedding API<br/>文本向量化<br/>OpenAI 兼容接口"]
        百度搜索["百度千帆 AI 搜索<br/>外部学习资源检索"]
        B站API["Bilibili API<br/>视频元数据获取<br/>字幕提取"]
    end

    浏览器["浏览器客户端<br/>Vue 3 SPA"] --> 前端容器
    前端容器 -->|"proxy /api"| 后端容器
    后端容器 --> MySQL容器
    后端容器 --> Redis容器
    后端容器 --> Neo4j容器
    后端容器 -->|"HTTP API 调用"| DeepSeek
    后端容器 -->|"HTTP API 调用"| Embedding
    后端容器 -->|"HTTP API 调用"| 百度搜索
    后端容器 -->|"HTTP API 调用"| B站API

    MySQL容器 --> MySQL卷
    Neo4j容器 --> Neo4j卷
    后端容器 --> 上传卷
```

---

## 附录: AI 图片生成提示词

如果 Mermaid 图无法满足需求，可将以下提示词复制到 Midjourney / DALL·E / Stable Diffusion 等 AI 绘图工具中生成专业架构图。

> **重要说明**：所有提示词均要求模块标题使用**中文**，技术栈名称（如 Spring Boot、MySQL、Redis）保留**英文**。

### A. 系统架构图提示词

```
Create a professional system architecture diagram for an educational platform called "知域智备优教 (SmartPrep)". 

CRITICAL RULE: All module/component TITLES must be in CHINESE. Technology names (Spring Boot, MySQL, Redis, Neo4j, Vue, WebGL, etc.) should remain in ENGLISH.

The architecture should show these layers from top to bottom:

LAYER 1 - "客户端层 (Client Layer)": A web browser icon labeled "浏览器客户端 Vue 3 SPA + WebGL (Unity 3D 虚拟人)".

LAYER 2 - "网关安全层 (Gateway Layer)": Show "Nginx 反向代理" and "JWT 认证拦截器 (AuthInterceptor + RoleCheckInterceptor)".

LAYER 3 - "应用层 (Application Layer) Spring Boot 3.2.5": Inside this layer, show 6 sub-module groups as colored boxes:
- Green box "门户与认证": 用户认证 (JWT Token), 门户首页 (Hero统计/课程列表)
- Blue box "教学核心": 课程体系管理 (学科→课程→章→节), 教学仪表盘 (教师/学生工作台), 考试管理, 班级管理
- Orange box "AI 引擎": RAG增强AI导师 (文档检索+LLM), 多智能体路由 (课程推荐/知识讲解/学情诊断/路径规划), 大模型调用客户端 (LLMClient OkHttp), RAG系统 (文档解析/向量化/Chroma VectorDB), 多智能体资源生成 (讲义/题库/思维导图), 语音交互 (TTS/STT Web Speech API)
- Purple box "知识图谱": 知识点掌握分析 (ELO追踪), MySQL→Neo4j 同步服务
- Cyan box "数据分析": 学习行为追踪, 六维能力评估 (广度/深度/解题/活跃/迁移/韧性), 游戏化激励 (XP/等级/徽章/打卡)
- Red box "内容工具": Bilibili视频导入管道, 教师工具 (题库/组卷/PPT), 自适应测验 (错因分析)

LAYER 4 - "数据层 (Data Layer)": MySQL 8.0 (46张表), Redis 7 (缓存), Neo4j 5 (知识图谱), Chroma VectorDB (向量存储).

LAYER 5 - "外部服务 (External APIs)": DeepSeek API, 百度千帆AI搜索, Bilibili API, Embedding API.

Connect layers with directional arrows. Use clear Chinese labels on all arrows. 
Color scheme: light/white background with distinct colors for each module. Professional enterprise architecture diagram style.
16:9 aspect ratio. High resolution suitable for PPT presentation.
```

### B. 数据库 ER 图提示词

```
Create a database Entity-Relationship (ER) diagram for an educational platform called "知域智备优教 (SmartPrep)". 

CRITICAL RULE: All table DESCRIPTION labels must be in CHINESE. Table names (sp_user, sp_course, etc.) and column names/types (bigint, varchar, etc.) remain in ENGLISH.

Show these main entity groups with clear boundary boxes and Chinese group titles:

GROUP 1 - "用户与学科 (Users & Subjects)" at center: sp_user table with columns (id PK, username UK, password, role, display_name, avatar_url). sp_subject table (id PK, name, icon, color, description).

GROUP 2 - "课程体系 (Curriculum)" at left: sp_course, sp_chapter, sp_sub_chapter, sp_knowledge_point, sp_kp_dependency, sp_exercise tables. Show FK relationships labeled in Chinese: 学科→课程→章→节→知识点→练习. Show self-referential dependency on sp_kp_dependency labeled "前置依赖".

GROUP 3 - "考试与班级 (Exams & Classes)" at top right: sp_exam, sp_exam_question, sp_exam_record, sp_wrong_question, sp_class, sp_class_member, sp_assignment, sp_assignment_submission tables. All FK relationships labeled in Chinese.

GROUP 4 - "游戏化与分析 (Gamification & Analytics)" at bottom right: sp_user_xp, sp_user_streak, sp_user_badge, sp_badge_def, sp_user_kp_mastery, sp_student_ability, sp_learning_activity, sp_task_completion tables. All labeled in Chinese.

GROUP 5 - "问答与资源 (Q&A & Resources)" at bottom: sp_qa_history, sp_course_question, sp_course_answer, sp_student_profile, sp_learning_resource, sp_study_path tables. All labeled in Chinese.

Use Crow's Foot notation. Include Chinese column descriptions inside entity boxes (e.g., "用户名" next to username). 
Use different colors for each group. Clean, professional database diagram style.
White background. 16:9 aspect ratio. High resolution.
```

### C. AI 对话数据流图提示词

```
Create a sequence diagram showing the AI conversation flow in the "知域智备优教 (SmartPrep)" educational platform.

CRITICAL RULE: All component names and labels must be in CHINESE. API paths and technical method names can remain in ENGLISH.

Actors and components (all in Chinese):
- "学生/教师" (user icon)
- "Vue 前端 (AICompanionImmersive / SplitPaneStudy)"
- "AgentController 智能体路由"
- "TutorServiceImplWithRAG RAG增强导师"
- "RAGService 检索增强生成"
- "Chroma VectorDB 文档向量存储"
- "LLMClient 大模型客户端 (DeepSeek API)"
- "MySQL (sp_qa_history)"

Show TWO flows with different colored arrows (blue for companion, green for tutoring):

Flow 1 - "AI 虚拟人对话 — 小慧 (蓝色箭头)":
学生 → 前端: 输入问题
前端 → AgentController: POST /api/agent/chat-stream (question, history, sessionId)
AgentController → AgentController: JWT 解析 userId
AgentController → LLMClient: chatStreamWithCompanion (系统提示词+历史+问题)
LLMClient → AgentController: SSE 流式返回 delta chunks
AgentController → 前端: SSE event stream (delta/finish)
AgentController → MySQL: 保存问答历史 (question+answer+sessionId)
前端 → 前端: parseStructuredReply → Web Speech API 朗读
前端 → 前端: buildLipSyncWav → AudioContext → uLipSync节点 → 口型动画

Flow 2 - "RAG 增强辅导 (绿色箭头)":
学生 → 前端: 输入问题
前端 → TutorService: POST /api/tutor/ask (question, context, sessionId)
TutorService → RAGService: retrieveRelevantDocuments (question, course, topK)
RAGService → Chroma: Embedding → 语义相似度检索
Chroma → RAGService: 相关文档片段列表
RAGService → TutorService: List of RetrievalResult
TutorService → LLMClient: chat (系统提示词+RAG上下文+问题)
LLMClient → TutorService: Markdown 格式回答
TutorService → MySQL: saveQaHistory
TutorService → 前端: TutorAnswer (markdown, citations, diagrams)

Clean, modern sequence diagram style. White background. 16:9 aspect ratio. High resolution.
```
