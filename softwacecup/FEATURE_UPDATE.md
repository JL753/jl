# 功能更新说明 v1.2.1

## 更新日期
2026-05-12

## 主要功能

### 1. 学生端首页问答记录功能
- **位置**：学生端首页左下角
- **功能**：
  - 显示最近5条AI问答记录
  - 点击问答记录可跳转到智能学习模块并自动提问
  - 实时从数据库加载问答历史
  - 支持时间格式化显示（分钟前、小时前、天前）

### 2. 学生端首页课表联动
- **位置**：学生端首页右侧
- **功能**：
  - 根据当前星期自动显示今日课表
  - 从数据库实时加载课程安排
  - 显示课程时间、名称、地点、教师信息
  - 无课时显示"今日无课程安排"

### 3. 个人信息页面课表管理
- **位置**：个人信息页面 Group 04（仅学生端显示）
- **功能**：
  - 7天课表网格展示
  - 添加课程：支持选择星期、时间、课程名称、地点、教师、周次
  - 编辑课程：点击课程卡片进入编辑模式
  - 删除课程：单个删除或批量清空
  - 刷新课表：手动刷新最新数据
  - 数据持久化到数据库

### 4. AI智能学习模块问答保存
- **功能**：
  - 每次AI回答完成后自动保存到数据库
  - 生成问答摘要（取问题前30字）
  - 支持会话ID关联
  - 支持URL参数自动提问（`/student/companion?q=问题`）

### 5. 数据库自动初始化禁用
- **修改**：注释掉 `SchemaInitializer` 的 `@PostConstruct` 注解
- **原因**：避免每次启动后端覆盖数据库数据
- **管理方式**：通过 SQL 文件手动管理数据库结构和初始数据

## 数据库变更

### 新增表

#### sp_course_schedule（课程表）
```sql
CREATE TABLE sp_course_schedule (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  day_of_week INT NOT NULL COMMENT '星期几 1-7',
  start_time VARCHAR(8) NOT NULL COMMENT '开始时间 HH:mm',
  end_time VARCHAR(8) NOT NULL COMMENT '结束时间 HH:mm',
  course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
  location VARCHAR(128) COMMENT '上课地点',
  teacher VARCHAR(64) COMMENT '授课教师',
  weeks VARCHAR(128) COMMENT '上课周次 如: 1-16',
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_schedule_user (user_id)
);
```

#### sp_qa_history（AI问答历史）
```sql
CREATE TABLE sp_qa_history (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  question TEXT NOT NULL COMMENT '用户提问',
  answer LONGTEXT COMMENT 'AI回答',
  summary VARCHAR(512) COMMENT '问答摘要',
  session_id VARCHAR(64) COMMENT '会话ID',
  created_at DATETIME,
  INDEX idx_qa_user (user_id),
  INDEX idx_qa_session (session_id)
);
```

### 初始数据
为学生演示账号（user_id=3）添加了大三数据科学与大数据技术专业的完整课表，包括：
- 周一：大数据处理技术、机器学习、数据挖掘
- 周二：Python数据分析、深度学习
- 周三：数据可视化、大数据系统架构
- 周四：Spark大数据开发、自然语言处理
- 周五：计算机视觉、数据科学综合实践

## 后端新增文件

### 实体类
- `CourseSchedule.java` - 课程表实体
- `QaHistory.java` - 问答历史实体

### Mapper
- `CourseScheduleMapper.java` - 课程表数据访问
- `QaHistoryMapper.java` - 问答历史数据访问

### Service
- `CourseScheduleService.java` - 课程表业务逻辑
- `QaHistoryService.java` - 问答历史业务逻辑

### Controller
- `CourseScheduleController.java` - 课程表API接口
  - `GET /api/schedule/my` - 获取我的课表
  - `GET /api/schedule/day/{dayOfWeek}` - 获取指定星期的课程
  - `POST /api/schedule/add` - 添加课程
  - `PUT /api/schedule/update` - 更新课程
  - `DELETE /api/schedule/{id}` - 删除课程
  - `DELETE /api/schedule/clear` - 清空课表

- `QaHistoryController.java` - 问答历史API接口
  - `GET /api/qa/recent?limit=10` - 获取最近问答记录
  - `POST /api/qa/save` - 保存问答记录
  - `GET /api/qa/session/{sessionId}` - 获取会话问答记录

## 前端修改文件

### StudentDashboard.vue
- 左下角课表替换为问答记录展示
- 右侧课表改为从数据库实时加载
- 添加问答记录点击跳转功能
- 添加时间格式化函数

### SplitPaneStudy.vue
- 添加问答历史保存功能
- 支持URL参数自动提问
- AI回答完成后自动保存到数据库

### ProfileView.vue
- 添加课表管理模块（Group 04）
- 7天课表网格展示
- 课程添加/编辑/删除对话框
- 课表数据加载和刷新

## 部署步骤

### 1. 更新数据库
```bash
# 方式1：执行完整SQL文件（会清空现有数据）
mysql -u root -proot smartprep < sql/init.sql

# 方式2：仅添加新表（保留现有数据）
mysql -u root -proot smartprep -e "
CREATE TABLE IF NOT EXISTS sp_course_schedule (...);
CREATE TABLE IF NOT EXISTS sp_qa_history (...);
INSERT INTO sp_course_schedule (...) ON DUPLICATE KEY UPDATE updated_at = NOW();
"
```

### 2. 重启后端
```bash
cd backend
mvn clean package
mvn spring-boot:run
```

### 3. 重新构建前端
```bash
cd frontend
npm run build
```

## 测试账号

- **学生账号**：student / 123456
- **教师账号**：teacher / 123456
- **管理员账号**：admin / 123456

学生账号已预置完整课表数据，可直接测试课表功能。

## 注意事项

1. **数据库密码**：本地MySQL账号密码均为 root/root
2. **自动初始化已禁用**：后端启动不再自动初始化数据库，需手动执行SQL文件
3. **课表数据**：仅学生端显示课表管理功能
4. **问答历史**：所有角色都会保存问答历史，但仅在学生端首页显示
5. **URL参数提问**：访问 `/student/companion?q=你的问题` 可自动触发AI提问

## 已修复的BUG

1. 修复了 `ApiResponse` 字段不匹配问题（`code` → `success`）
2. 修复了前端多处 `response.data.code === 200` 的错误判断
3. 修复了 `/api/crawl/search` 404错误（需重启后端）
