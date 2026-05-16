# 知域 Plan A — 后端重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构数据库课程体系、简化角色、新增班级/作业/六维能力/WebSearch 等后端 API，为前端重写提供完整接口支撑。

**Architecture:** 保留现有认证/AI/游戏化/RAG 服务不动；重构 sp_course/unit/lesson/exercise 表结构；新增 8 张表；新增 6 个 Controller；角色从三角色简化为两角色（ROLE_TEACHER 兼 ROLE_ADMIN）。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, MySQL 8.0, Java 17, Lombok, JWT (JJWT 0.12)

---

## 文件变更总览

**新建文件：**
- `sql/migrate-v2.sql` — 全量重构 DDL + 初始数据
- `backend/.../domain/Subject.java`
- `backend/.../domain/Unit.java` (重构)
- `backend/.../domain/Lesson.java` (重构)
- `backend/.../domain/Exercise.java` (重构)
- `backend/.../domain/ZhiyuClass.java`
- `backend/.../domain/ClassMember.java`
- `backend/.../domain/Assignment.java`
- `backend/.../domain/AssignmentSubmission.java`
- `backend/.../domain/LessonProgress.java`
- `backend/.../domain/ContentReview.java`
- `backend/.../domain/StudentAbility.java`
- `backend/.../domain/ResourceRecommendation.java`
- `backend/.../mapper/SubjectMapper.java`
- `backend/.../mapper/UnitMapper.java`
- `backend/.../mapper/LessonMapper.java`
- `backend/.../mapper/ExerciseMapper.java`
- `backend/.../mapper/ZhiyuClassMapper.java`
- `backend/.../mapper/ClassMemberMapper.java`
- `backend/.../mapper/AssignmentMapper.java`
- `backend/.../mapper/AssignmentSubmissionMapper.java`
- `backend/.../mapper/LessonProgressMapper.java`
- `backend/.../mapper/ContentReviewMapper.java`
- `backend/.../mapper/StudentAbilityMapper.java`
- `backend/.../mapper/ResourceRecommendationMapper.java`
- `backend/.../controller/SubjectCourseController.java`
- `backend/.../controller/ClassController.java`
- `backend/.../controller/AssignmentController.java`
- `backend/.../controller/ContentReviewController.java`
- `backend/.../controller/StudentAbilityController.java`
- `backend/.../controller/LessonProgressController.java`
- `backend/.../service/StudentAbilityService.java`
- `backend/.../service/impl/StudentAbilityServiceImpl.java`
- `backend/.../service/agent/WebSearchAgent.java`
- `backend/.../dto/AbilityScoreDto.java`
- `backend/.../dto/ClassDto.java`
- `backend/.../dto/AssignmentDto.java`

**修改文件：**
- `sql/init.sql` — 替换为 v2 全量 DDL
- `backend/.../config/AuthInterceptor.java` — 开放新公开路径
- `backend/.../config/RoleCheckInterceptor.java` — ROLE_ADMIN → ROLE_TEACHER
- `backend/.../config/SchemaInitializer.java` — 禁用旧逻辑，改为检查 v2 表存在

---

## Task 1: 数据库 v2 全量 DDL

**Files:**
- Create: `sql/migrate-v2.sql`
- Modify: `sql/init.sql`

- [ ] **Step 1: 创建 `sql/migrate-v2.sql`（第一部分：DROP + 用户表 + 课程体系表）**

```sql
-- sql/migrate-v2.sql
-- 知域平台 v2 全量 DDL
-- 执行前确保数据库已存在：CREATE DATABASE IF NOT EXISTS smartprep DEFAULT CHARACTER SET utf8mb4;

USE smartprep;

-- ===== 清理旧表（按依赖顺序 DROP）=====
DROP TABLE IF EXISTS sp_resource_recommendation;
DROP TABLE IF EXISTS sp_student_ability;
DROP TABLE IF EXISTS sp_content_review;
DROP TABLE IF EXISTS sp_lesson_progress;
DROP TABLE IF EXISTS sp_assignment_submission;
DROP TABLE IF EXISTS sp_assignment;
DROP TABLE IF EXISTS sp_class_member;
DROP TABLE IF EXISTS sp_class;
DROP TABLE IF EXISTS sp_user_kp_mastery;
DROP TABLE IF EXISTS sp_exercise;
DROP TABLE IF EXISTS sp_lesson;
DROP TABLE IF EXISTS sp_unit;
DROP TABLE IF EXISTS sp_course;
DROP TABLE IF EXISTS sp_subject;
DROP TABLE IF EXISTS sp_user;

-- ===== 用户表 =====
CREATE TABLE sp_user (
  id         BIGINT PRIMARY KEY,
  username   VARCHAR(64)  NOT NULL UNIQUE,
  password   VARCHAR(128) NOT NULL,
  role       VARCHAR(32)  NOT NULL COMMENT 'ROLE_TEACHER | ROLE_STUDENT',
  display_name VARCHAR(64) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: 追加课程体系表到 `sql/migrate-v2.sql`**

```sql
-- ===== 课程体系 =====
CREATE TABLE sp_subject (
  id          BIGINT PRIMARY KEY,
  name        VARCHAR(64)  NOT NULL,
  icon        VARCHAR(128),
  color       VARCHAR(32),
  description TEXT,
  order_index INT DEFAULT 0
);

CREATE TABLE sp_course (
  id          BIGINT PRIMARY KEY,
  subject_id  BIGINT       NOT NULL,
  title       VARCHAR(128) NOT NULL,
  description TEXT,
  cover_image VARCHAR(512),
  difficulty  VARCHAR(32)  DEFAULT 'beginner',
  status      VARCHAR(32)  DEFAULT 'published',
  created_by  BIGINT,
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_course_subject (subject_id)
);

CREATE TABLE sp_unit (
  id          BIGINT PRIMARY KEY,
  course_id   BIGINT       NOT NULL,
  title       VARCHAR(128) NOT NULL,
  description TEXT,
  order_index INT DEFAULT 0,
  INDEX idx_unit_course (course_id)
);

CREATE TABLE sp_lesson (
  id           BIGINT PRIMARY KEY,
  unit_id      BIGINT       NOT NULL,
  title        VARCHAR(128) NOT NULL,
  video_url    VARCHAR(512),
  content_json LONGTEXT     COMMENT '{"sections":[{"type":"text","content":"...","tags":["重点"]}]}',
  status       VARCHAR(32)  DEFAULT 'draft',
  order_index  INT DEFAULT 0,
  created_by   BIGINT,
  created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_lesson_unit (unit_id)
);

CREATE TABLE sp_exercise (
  id            BIGINT PRIMARY KEY,
  lesson_id     BIGINT NOT NULL,
  question_text TEXT   NOT NULL,
  question_type VARCHAR(32) DEFAULT 'single' COMMENT 'single|multiple|judge',
  options_json  TEXT   COMMENT '[{"key":"A","text":"选项内容"}]',
  answer        VARCHAR(512) NOT NULL,
  explanation   TEXT,
  difficulty    INT DEFAULT 1 COMMENT '1-3',
  kp_tags       VARCHAR(255),
  order_index   INT DEFAULT 0,
  INDEX idx_exercise_lesson (lesson_id)
);

CREATE TABLE sp_user_kp_mastery (
  id                BIGINT PRIMARY KEY,
  user_id           BIGINT NOT NULL,
  lesson_id         BIGINT NOT NULL,
  mastery_level     INT DEFAULT 0 COMMENT '0-4',
  practice_count    INT DEFAULT 0,
  last_practiced_at DATETIME,
  UNIQUE KEY uk_user_lesson (user_id, lesson_id)
);
```

- [ ] **Step 3: 追加班级/作业/进度/审核/能力/推荐表到 `sql/migrate-v2.sql`**

```sql
-- ===== 班级管理 =====
CREATE TABLE sp_class (
  id          BIGINT PRIMARY KEY,
  name        VARCHAR(128) NOT NULL,
  teacher_id  BIGINT       NOT NULL,
  invite_code VARCHAR(32)  NOT NULL UNIQUE,
  description TEXT,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_class_teacher (teacher_id)
);

CREATE TABLE sp_class_member (
  id         BIGINT PRIMARY KEY,
  class_id   BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_class_student (class_id, student_id)
);

-- ===== 作业 =====
CREATE TABLE sp_assignment (
  id              BIGINT PRIMARY KEY,
  class_id        BIGINT       NOT NULL,
  title           VARCHAR(128) NOT NULL,
  lesson_ids_json TEXT         COMMENT '[1,2,3]',
  description     TEXT,
  due_at          DATETIME,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_assignment_class (class_id)
);

CREATE TABLE sp_assignment_submission (
  id            BIGINT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  student_id    BIGINT NOT NULL,
  status        VARCHAR(32) DEFAULT 'pending' COMMENT 'pending|submitted|graded',
  score         INT,
  feedback      TEXT,
  submitted_at  DATETIME,
  UNIQUE KEY uk_assign_student (assignment_id, student_id)
);

-- ===== 课时进度 =====
CREATE TABLE sp_lesson_progress (
  id           BIGINT PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  lesson_id    BIGINT NOT NULL,
  status       VARCHAR(32) DEFAULT 'not_started' COMMENT 'not_started|in_progress|completed',
  completed_at DATETIME,
  UNIQUE KEY uk_user_lesson_prog (user_id, lesson_id)
);

-- ===== AI 内容审核 =====
CREATE TABLE sp_content_review (
  id            BIGINT PRIMARY KEY,
  lesson_id     BIGINT NOT NULL,
  ai_draft_json LONGTEXT,
  status        VARCHAR(32) DEFAULT 'pending' COMMENT 'pending|approved|rejected',
  reviewer_id   BIGINT,
  reviewed_at   DATETIME,
  INDEX idx_review_lesson (lesson_id)
);

-- ===== 六维能力 =====
CREATE TABLE sp_student_ability (
  id               BIGINT PRIMARY KEY,
  user_id          BIGINT NOT NULL,
  breadth_score    INT DEFAULT 0,
  depth_score      INT DEFAULT 0,
  problem_score    INT DEFAULT 0,
  activity_score   INT DEFAULT 0,
  transfer_score   INT DEFAULT 0,
  resilience_score INT DEFAULT 0,
  diagnosis        TEXT,
  evaluated_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ability_user (user_id)
);

-- ===== 资源推荐 =====
CREATE TABLE sp_resource_recommendation (
  id             BIGINT PRIMARY KEY,
  user_id        BIGINT NOT NULL,
  lesson_id      BIGINT NOT NULL,
  resources_json LONGTEXT,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_rec_user (user_id)
);
```

- [ ] **Step 4: 追加初始数据（用户 + 学科 + 课程 + 单元）到 `sql/migrate-v2.sql`**

```sql
-- ===== 初始数据 =====
-- 密码 123456 的 BCrypt 哈希
INSERT INTO sp_user (id, username, password, role, display_name) VALUES
(1, 'admin',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ROLE_TEACHER', '管理员'),
(2, 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ROLE_TEACHER', '张老师'),
(3, 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'ROLE_STUDENT', '李同学');

INSERT INTO sp_subject (id, name, icon, color, description, order_index) VALUES
(1, '计算机科学', 'computer', '#4F46E5', '涵盖编程基础、数据结构、算法等核心课程', 1);

INSERT INTO sp_course (id, subject_id, title, description, difficulty, status, created_by) VALUES
(1, 1, '编程入门', '从零开始学习编程，掌握基本语法与逻辑思维', 'beginner', 'published', 2);

INSERT INTO sp_unit (id, course_id, title, description, order_index) VALUES
(1, 1, '第一单元：编程基础概念', '了解什么是程序、变量、数据类型', 1),
(2, 1, '第二单元：流程控制',     '掌握条件判断与循环结构',         2),
(3, 1, '第三单元：函数与模块',   '学习函数定义、参数传递与模块化', 3);
```

- [ ] **Step 5: 追加 9 个课时（含 content_json）到 `sql/migrate-v2.sql`**

```sql
INSERT INTO sp_lesson (id, unit_id, title, content_json, status, order_index, created_by) VALUES
(1, 1, '什么是程序', '{"sections":[{"type":"text","content":"程序是一组让计算机执行特定任务的指令集合。就像菜谱告诉厨师如何做菜，程序告诉计算机如何完成工作。","tags":["重点"]},{"type":"text","content":"程序由三个基本要素组成：输入（Input）、处理（Process）、输出（Output），简称 IPO 模型。","tags":["重点","考点"]},{"type":"text","content":"常见的编程语言有 Python、Java、C++ 等，它们各有适用场景。Python 语法简洁，适合初学者。","tags":[]},{"type":"text","content":"易错点：程序不等于软件，软件是程序加上文档和数据的集合。","tags":["易错点"]}]}', 'published', 1, 2),
(2, 1, '变量与数据类型', '{"sections":[{"type":"text","content":"变量是存储数据的容器，可以把它想象成一个贴了标签的盒子。变量名就是标签，变量值就是盒子里的内容。","tags":["重点"]},{"type":"text","content":"Python 基本数据类型：整数(int)、浮点数(float)、字符串(str)、布尔值(bool)。用 type() 函数可以查看变量类型。","tags":["重点","考点"]},{"type":"text","content":"变量命名规则：只能包含字母、数字、下划线；不能以数字开头；区分大小写；不能使用关键字。","tags":["难点"]},{"type":"text","content":"易错点：Python 中整数除法 5/2=2.5，而不是 2；若要整除需用 5//2=2。","tags":["易错点"]}]}', 'published', 2, 2),
(3, 1, '输入与输出', '{"sections":[{"type":"text","content":"input() 函数从键盘读取用户输入，返回值始终是字符串类型。若需要数字，必须用 int() 或 float() 转换。","tags":["重点"]},{"type":"text","content":"print() 函数输出内容到屏幕。支持多个参数（用逗号分隔），可用 sep 指定分隔符，end 指定结尾字符。","tags":["重点"]},{"type":"text","content":"格式化输出三种方式：% 格式化、str.format()、f-string（推荐）。示例：f\"姓名：{name}，年龄：{age}\"","tags":["考点"]},{"type":"text","content":"易错点：input() 返回的是字符串，直接做数学运算会报 TypeError，必须先转换类型。","tags":["易错点"]}]}', 'published', 3, 2),
(4, 2, '条件判断 if-else', '{"sections":[{"type":"text","content":"if 语句根据条件真假决定执行哪段代码。Python 用缩进（4个空格）表示代码块，不用花括号。","tags":["重点"]},{"type":"text","content":"完整结构：if 条件1: ... elif 条件2: ... else: ...。elif 可以有多个，else 最多一个且在最后。","tags":["重点","考点"]},{"type":"text","content":"比较运算符：==（等于）、!=（不等于）、>、<、>=、<=。逻辑运算符：and、or、not。","tags":["考点"]},{"type":"text","content":"易错点：条件判断用 ==，赋值用 =，混淆会导致逻辑错误或语法错误。","tags":["易错点"]}]}', 'published', 1, 2),
(5, 2, 'for 循环', '{"sections":[{"type":"text","content":"for 循环用于遍历序列（列表、字符串、range等）。语法：for 变量 in 序列: 循环体。","tags":["重点"]},{"type":"text","content":"range() 函数生成数字序列：range(5) 生成 0-4；range(1,6) 生成 1-5；range(0,10,2) 生成偶数 0,2,4,6,8。","tags":["重点","考点"]},{"type":"text","content":"break 立即退出循环；continue 跳过本次循环剩余代码进入下一次；else 子句在循环正常结束（未被break）时执行。","tags":["难点"]},{"type":"text","content":"易错点：range(n) 不包含 n 本身，range(1,n+1) 才能遍历 1 到 n。","tags":["易错点"]}]}', 'published', 2, 2),
(6, 2, 'while 循环', '{"sections":[{"type":"text","content":"while 循环在条件为真时反复执行循环体。适合不知道循环次数、需要根据条件决定是否继续的场景。","tags":["重点"]},{"type":"text","content":"无限循环：while True: ... 配合 break 使用，常用于菜单交互程序。","tags":["考点"]},{"type":"text","content":"循环变量更新：必须在循环体内修改循环条件相关的变量，否则会造成死循环。","tags":["难点"]},{"type":"text","content":"易错点：忘记更新循环变量导致死循环，程序卡死。调试时可加 print 观察变量变化。","tags":["易错点"]}]}', 'published', 3, 2),
(7, 3, '函数定义与调用', '{"sections":[{"type":"text","content":"函数是可复用的代码块。用 def 关键字定义：def 函数名(参数列表): 函数体。调用时直接写函数名加括号。","tags":["重点"]},{"type":"text","content":"参数类型：位置参数、关键字参数、默认参数、可变参数(*args)、关键字可变参数(**kwargs)。","tags":["重点","考点"]},{"type":"text","content":"return 语句返回值并结束函数。没有 return 或 return 后无值时，函数返回 None。","tags":["考点"]},{"type":"text","content":"易错点：默认参数不能使用可变对象（如列表）作为默认值，否则多次调用会共享同一对象。","tags":["易错点","难点"]}]}', 'published', 1, 2),
(8, 3, '作用域与变量生命周期', '{"sections":[{"type":"text","content":"作用域决定变量在哪里可以被访问。Python 遵循 LEGB 规则：Local（局部）→ Enclosing（嵌套）→ Global（全局）→ Built-in（内置）。","tags":["重点","难点"]},{"type":"text","content":"函数内部可以读取全局变量，但若要修改全局变量，必须用 global 关键字声明。","tags":["考点"]},{"type":"text","content":"局部变量在函数调用时创建，函数返回后销毁。全局变量在程序运行期间一直存在。","tags":["重点"]},{"type":"text","content":"易错点：在函数内对全局变量赋值而不加 global，会创建同名局部变量，不会修改全局变量。","tags":["易错点"]}]}', 'published', 2, 2),
(9, 3, '模块与导入', '{"sections":[{"type":"text","content":"模块是包含 Python 代码的 .py 文件。用 import 语句导入模块，用 from...import 导入特定内容。","tags":["重点"]},{"type":"text","content":"标准库常用模块：math（数学）、random（随机）、datetime（日期时间）、os（操作系统）、sys（系统）。","tags":["考点"]},{"type":"text","content":"自定义模块：将函数写在单独的 .py 文件中，在其他文件中 import 即可复用。__name__ == \"__main__\" 用于区分直接运行和被导入。","tags":["重点"]},{"type":"text","content":"易错点：import 模块名时，Python 按 sys.path 顺序查找，自定义模块需放在当前目录或添加到 sys.path。","tags":["易错点"]}]}', 'published', 3, 2);
```

- [ ] **Step 6: 追加 15 道练习题到 `sql/migrate-v2.sql`**

```sql
INSERT INTO sp_exercise (id, lesson_id, question_text, question_type, options_json, answer, explanation, difficulty, kp_tags, order_index) VALUES
(1,  1, '以下哪项最准确地描述了"程序"？', 'single', '[{"key":"A","text":"一种硬件设备"},{"key":"B","text":"让计算机执行特定任务的指令集合"},{"key":"C","text":"操作系统的别称"},{"key":"D","text":"编程语言本身"}]', 'B', '程序是指令的集合，告诉计算机如何完成特定任务，与硬件、操作系统、语言本身不同。', 1, '程序定义', 1),
(2,  1, 'IPO 模型中，P 代表什么？', 'single', '[{"key":"A","text":"Print（打印）"},{"key":"B","text":"Program（程序）"},{"key":"C","text":"Process（处理）"},{"key":"D","text":"Python"}]', 'C', 'IPO 是 Input（输入）、Process（处理）、Output（输出）的缩写，P 代表处理。', 1, 'IPO模型', 2),
(3,  2, '以下变量名合法的是？', 'single', '[{"key":"A","text":"2name"},{"key":"B","text":"my-var"},{"key":"C","text":"_score"},{"key":"D","text":"class"}]', 'C', '_score 以下划线开头，合法。2name 以数字开头不合法；my-var 含连字符不合法；class 是关键字不合法。', 2, '变量命名', 1),
(4,  2, 'Python 中 5/2 的结果是？', 'single', '[{"key":"A","text":"2"},{"key":"B","text":"2.5"},{"key":"C","text":"2.0"},{"key":"D","text":"报错"}]', 'B', 'Python 3 中 / 是真除法，5/2=2.5。若要整除需用 //，5//2=2。', 1, '数据类型,运算符', 2),
(5,  3, 'input() 函数返回值的类型是？', 'single', '[{"key":"A","text":"int"},{"key":"B","text":"float"},{"key":"C","text":"str"},{"key":"D","text":"取决于用户输入的内容"}]', 'C', 'input() 始终返回字符串(str)类型，无论用户输入什么内容。', 1, 'input函数', 1),
(6,  3, '以下哪种 f-string 写法正确？', 'single', '[{"key":"A","text":"f(\"年龄：{age}\")"},{"key":"B","text":"f\"年龄：{age}\""},{"key":"C","text":"\"年龄：f{age}\""},{"key":"D","text":"f\"年龄：age\"}"}]', 'B', 'f-string 语法是在字符串前加 f，变量用花括号包裹：f"文字{变量}"。', 1, 'f-string', 2),
(7,  4, '以下 if 语句语法正确的是？', 'single', '[{"key":"A","text":"if x > 0 { print(x) }"},{"key":"B","text":"if (x > 0): print(x)"},{"key":"C","text":"if x > 0:\n    print(x)"},{"key":"D","text":"if x > 0 then print(x)"}]', 'C', 'Python 的 if 语句不用花括号，用冒号+缩进表示代码块。括号可选但不是必须的。', 1, 'if语句', 1),
(8,  4, '判断 x 在 1 到 10 之间（含两端）的正确写法是？', 'single', '[{"key":"A","text":"if 1 <= x <= 10:"},{"key":"B","text":"if x >= 1 and x <= 10:"},{"key":"C","text":"以上两种都正确"},{"key":"D","text":"if x in range(1,11):"}]', 'C', 'Python 支持链式比较 1<=x<=10，也可用 and 连接两个条件，两种写法都正确。', 2, '条件判断', 2),
(9,  5, 'range(1, 10, 2) 生成的序列是？', 'single', '[{"key":"A","text":"1,3,5,7,9"},{"key":"B","text":"1,3,5,7,9,10"},{"key":"C","text":"2,4,6,8"},{"key":"D","text":"1,2,3,4,5,6,7,8,9"}]', 'A', 'range(start, stop, step) 从1开始，步长2，不含10，生成 1,3,5,7,9。', 1, 'range函数', 1),
(10, 5, 'for 循环中 break 的作用是？', 'single', '[{"key":"A","text":"跳过本次循环，继续下一次"},{"key":"B","text":"立即退出整个循环"},{"key":"C","text":"退出当前函数"},{"key":"D","text":"暂停循环"}]', 'B', 'break 立即退出整个循环结构；continue 才是跳过本次循环继续下一次。', 1, 'break,continue', 2),
(11, 6, '以下 while 循环会执行几次？\nn = 0\nwhile n < 3:\n    n += 1', 'single', '[{"key":"A","text":"0次"},{"key":"B","text":"2次"},{"key":"C","text":"3次"},{"key":"D","text":"无限次"}]', 'C', 'n 从0开始，每次加1，当 n=0,1,2 时条件成立执行，n=3 时条件不成立退出，共执行3次。', 1, 'while循环', 1),
(12, 7, '以下函数定义中，哪个参数是默认参数？\ndef greet(name, greeting="你好"):', 'single', '[{"key":"A","text":"name"},{"key":"B","text":"greeting"},{"key":"C","text":"两个都是"},{"key":"D","text":"两个都不是"}]', 'B', 'greeting="你好" 有默认值，是默认参数。调用时可以不传 greeting，它会使用默认值"你好"。', 1, '函数参数', 1),
(13, 7, '函数没有 return 语句时，返回值是？', 'single', '[{"key":"A","text":"0"},{"key":"B","text":"空字符串"},{"key":"C","text":"None"},{"key":"D","text":"报错"}]', 'C', 'Python 函数没有 return 语句或 return 后无值时，自动返回 None。', 1, 'return语句', 2),
(14, 8, '在函数内修改全局变量，需要使用哪个关键字？', 'single', '[{"key":"A","text":"global"},{"key":"B","text":"nonlocal"},{"key":"C","text":"extern"},{"key":"D","text":"public"}]', 'A', 'global 关键字声明变量为全局变量，允许在函数内修改它。nonlocal 用于嵌套函数中修改外层函数的变量。', 2, '作用域,global', 1),
(15, 9, '导入 math 模块中 sqrt 函数的正确写法是？', 'single', '[{"key":"A","text":"import sqrt from math"},{"key":"B","text":"from math import sqrt"},{"key":"C","text":"include math.sqrt"},{"key":"D","text":"using math.sqrt"}]', 'B', 'Python 中从模块导入特定函数的语法是 from 模块名 import 函数名。', 1, 'import,模块', 1);
```

- [ ] **Step 7: 执行 SQL 验证**

```bash
mysql -u root -p smartprep < sql/migrate-v2.sql
# 验证表数量
mysql -u root -p -e "USE smartprep; SHOW TABLES;" | wc -l
# 期望输出：至少 16 行（15张表 + 1行表头）
mysql -u root -p -e "USE smartprep; SELECT COUNT(*) FROM sp_lesson; SELECT COUNT(*) FROM sp_exercise;"
# 期望：sp_lesson=9, sp_exercise=15
```

- [ ] **Step 8: 将 `sql/migrate-v2.sql` 内容同步到 `sql/init.sql`**

用 `sql/migrate-v2.sql` 的完整内容替换 `sql/init.sql`（保留 `CREATE DATABASE` 头部）：

```bash
cp sql/migrate-v2.sql sql/init.sql
# 在 init.sql 头部加上建库语句（如果 migrate-v2.sql 没有）
```

- [ ] **Step 9: git commit**

```bash
git add sql/migrate-v2.sql sql/init.sql
git commit -m "feat(db): v2 全量 DDL，新增15张表+初始数据(9课时/15练习题/3账号)"
```

---

## Task 2: 角色简化 + 拦截器更新

**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/config/RoleCheckInterceptor.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/config/AuthInterceptor.java`

- [ ] **Step 1: 读取 RoleCheckInterceptor.java 当前内容**

```bash
cat backend/src/main/java/com/iflytek/smartprep/config/RoleCheckInterceptor.java
```

当前逻辑：遍历 `@RequireRole` 注解的 value 数组，与 `loginUser.getRole()` 做 equalsIgnoreCase 比较。

- [ ] **Step 2: 修改 RoleCheckInterceptor.java，让 ROLE_TEACHER 自动通过 ROLE_ADMIN 检查**

```java
// backend/src/main/java/com/iflytek/smartprep/config/RoleCheckInterceptor.java
package com.iflytek.smartprep.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录\"}");
            return false;
        }
        String userRole = loginUser.getRole();
        for (String required : requireRole.value()) {
            // ROLE_TEACHER 兼具 ROLE_ADMIN 权限
            if (required.equalsIgnoreCase(userRole)) return true;
            if ("ROLE_ADMIN".equalsIgnoreCase(required)
                    && "ROLE_TEACHER".equalsIgnoreCase(userRole)) return true;
        }
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"无权限\"}");
        return false;
    }
}
```

- [ ] **Step 3: 修改 AuthInterceptor.java，开放新公开路径**

将 `OPEN_PATHS` 中添加 `/api/subjects` 和 `/api/courses/public`：

```java
// backend/src/main/java/com/iflytek/smartprep/config/AuthInterceptor.java
// 修改 OPEN_PATHS 常量（其余代码不变）
    private static final Set<String> OPEN_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/common/health",
            "/api/common/portal",
            "/api/common/datacenter",
            "/api/agent",
            "/api/course",
            "/api/subjects",
            "/api/courses/public",
            "/api/knowledge-graph/full",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui/index.html",
            "/error"
    );
```

- [ ] **Step 4: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：无错误输出，BUILD SUCCESS
```

- [ ] **Step 5: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/config/RoleCheckInterceptor.java
git add backend/src/main/java/com/iflytek/smartprep/config/AuthInterceptor.java
git commit -m "feat(auth): ROLE_TEACHER 兼具 ROLE_ADMIN 权限；开放 /api/subjects 公开路径"
```

---

## Task 3: Domain 实体类

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/Subject.java` (重构)
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/ZhiyuClass.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/ClassMember.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/Assignment.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/AssignmentSubmission.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/LessonProgress.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/ContentReview.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/StudentAbility.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/ResourceRecommendation.java`

- [ ] **Step 1: 重构 Subject.java（添加 Lombok Builder 注解）**

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Subject.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_subject")
public class Subject {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String icon;
    private String color;
    private String description;
    private Integer orderIndex;
}
```

- [ ] **Step 2: 创建 ZhiyuClass.java**

```java
// backend/src/main/java/com/iflytek/smartprep/domain/ZhiyuClass.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_class")
public class ZhiyuClass {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long teacherId;
    private String inviteCode;
    private String description;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 ClassMember.java、Assignment.java、AssignmentSubmission.java**

```java
// backend/src/main/java/com/iflytek/smartprep/domain/ClassMember.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_class_member")
public class ClassMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private Long studentId;
    private LocalDateTime joinedAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Assignment.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_assignment")
public class Assignment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private String title;
    private String lessonIdsJson;
    private String description;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/AssignmentSubmission.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_assignment_submission")
public class AssignmentSubmission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String status;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
```

- [ ] **Step 4: 创建 LessonProgress.java、ContentReview.java、StudentAbility.java、ResourceRecommendation.java**

```java
// backend/src/main/java/com/iflytek/smartprep/domain/LessonProgress.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_lesson_progress")
public class LessonProgress {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long lessonId;
    private String status;
    private LocalDateTime completedAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/ContentReview.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_content_review")
public class ContentReview {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long lessonId;
    private String aiDraftJson;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/StudentAbility.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_student_ability")
public class StudentAbility {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer breadthScore;
    private Integer depthScore;
    private Integer problemScore;
    private Integer activityScore;
    private Integer transferScore;
    private Integer resilienceScore;
    private String diagnosis;
    private LocalDateTime evaluatedAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/ResourceRecommendation.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_resource_recommendation")
public class ResourceRecommendation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long lessonId;
    private String resourcesJson;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS，无错误
```

- [ ] **Step 6: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/domain/
git commit -m "feat(domain): 新增9个实体类，重构 Subject/Lesson/Unit/Exercise/UserKpMastery"
```

---

## Task 4: Mapper 接口

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ZhiyuClassMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ClassMemberMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/AssignmentMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/AssignmentSubmissionMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/LessonProgressMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ContentReviewMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/StudentAbilityMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ResourceRecommendationMapper.java`

- [ ] **Step 1: 创建所有 Mapper 文件**

```java
// ZhiyuClassMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.ZhiyuClass;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface ZhiyuClassMapper extends BaseMapper<ZhiyuClass> {}
```

```java
// ClassMemberMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.ClassMember;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface ClassMemberMapper extends BaseMapper<ClassMember> {}
```

```java
// AssignmentMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.Assignment;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface AssignmentMapper extends BaseMapper<Assignment> {}
```

```java
// AssignmentSubmissionMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.AssignmentSubmission;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface AssignmentSubmissionMapper extends BaseMapper<AssignmentSubmission> {}
```

```java
// LessonProgressMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.LessonProgress;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface LessonProgressMapper extends BaseMapper<LessonProgress> {}
```

```java
// ContentReviewMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.ContentReview;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface ContentReviewMapper extends BaseMapper<ContentReview> {}
```

```java
// StudentAbilityMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.StudentAbility;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface StudentAbilityMapper extends BaseMapper<StudentAbility> {}
```

```java
// ResourceRecommendationMapper.java
package com.iflytek.smartprep.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.ResourceRecommendation;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface ResourceRecommendationMapper extends BaseMapper<ResourceRecommendation> {}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 3: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/mapper/
git commit -m "feat(mapper): 新增8个 Mapper 接口"
```

---

## Task 5: SubjectCourseController — 课程体系 CRUD API

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/dto/LessonContentDto.java`

- [ ] **Step 1: 创建 LessonContentDto.java**

```java
// backend/src/main/java/com/iflytek/smartprep/dto/LessonContentDto.java
package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class LessonContentDto {
    private Long unitId;
    private String title;
    private String videoUrl;
    private String contentJson;
    private String status;
    private Integer orderIndex;
}
```

- [ ] **Step 2: 创建 SubjectCourseController.java（第一部分：学科/课程/单元查询）**

```java
// backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.LessonContentDto;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.LLMClient;
import com.iflytek.smartprep.service.agent.WebSearchAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SubjectCourseController {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final UnitMapper unitMapper;
    private final LessonMapper lessonMapper;
    private final ExerciseMapper exerciseMapper;
    private final LLMClient llmClient;
    private final WebSearchAgent webSearchAgent;
    private final ResourceRecommendationMapper resourceRecommendationMapper;

    /** GET /api/subjects — 获取所有学科（公开） */
    @GetMapping("/api/subjects")
    public ApiResponse<List<Subject>> listSubjects() {
        return ApiResponse.ok(subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getOrderIndex)));
    }

    /** GET /api/subjects/{id}/courses — 获取学科下课程（公开） */
    @GetMapping("/api/subjects/{id}/courses")
    public ApiResponse<List<Course>> listCoursesBySubject(@PathVariable Long id) {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getSubjectId, id)
                        .eq(Course::getStatus, "published")));
    }

    /** GET /api/courses/public — 获取所有已发布课程（公开） */
    @GetMapping("/api/courses/public")
    public ApiResponse<List<Course>> listPublicCourses() {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, "published")));
    }

    /** GET /api/courses/{id}/units — 获取课程下单元（公开） */
    @GetMapping("/api/courses/{id}/units")
    public ApiResponse<List<Unit>> listUnitsByCourse(@PathVariable Long id) {
        return ApiResponse.ok(unitMapper.selectList(
                new LambdaQueryWrapper<Unit>()
                        .eq(Unit::getCourseId, id)
                        .orderByAsc(Unit::getOrderIndex)));
    }
```

- [ ] **Step 3: 追加课时/练习题查询方法到 SubjectCourseController.java**

```java
    /** GET /api/units/{id}/lessons — 获取单元下课时（需登录） */
    @GetMapping("/api/units/{id}/lessons")
    public ApiResponse<List<Lesson>> listLessonsByUnit(@PathVariable Long id) {
        return ApiResponse.ok(lessonMapper.selectList(
                new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getUnitId, id)
                        .orderByAsc(Lesson::getOrderIndex)));
    }

    /** GET /api/lessons/{id} — 获取课时详情（需登录） */
    @GetMapping("/api/lessons/{id}")
    public ApiResponse<Lesson> getLessonDetail(@PathVariable Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");
        return ApiResponse.ok(lesson);
    }

    /** GET /api/lessons/{id}/exercises — 获取课时练习题（需登录） */
    @GetMapping("/api/lessons/{id}/exercises")
    public ApiResponse<List<Exercise>> listExercises(@PathVariable Long id) {
        return ApiResponse.ok(exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getLessonId, id)
                        .orderByAsc(Exercise::getOrderIndex)));
    }

    /** POST /api/lessons — 教师创建课时 */
    @PostMapping("/api/lessons")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<Lesson> createLesson(@RequestBody LessonContentDto dto) {
        Lesson lesson = new Lesson();
        lesson.setUnitId(dto.getUnitId());
        lesson.setTitle(dto.getTitle());
        lesson.setVideoUrl(dto.getVideoUrl());
        lesson.setContentJson(dto.getContentJson());
        lesson.setStatus(dto.getStatus() != null ? dto.getStatus() : "draft");
        lesson.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        lesson.setCreatedBy(LoginUserHolder.get().getUserId());
        lesson.setCreatedAt(LocalDateTime.now());
        lessonMapper.insert(lesson);
        return ApiResponse.ok(lesson);
    }

    /** PUT /api/lessons/{id} — 教师更新课时 */
    @PutMapping("/api/lessons/{id}")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<Lesson> updateLesson(@PathVariable Long id, @RequestBody LessonContentDto dto) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");
        if (dto.getTitle() != null) lesson.setTitle(dto.getTitle());
        if (dto.getVideoUrl() != null) lesson.setVideoUrl(dto.getVideoUrl());
        if (dto.getContentJson() != null) lesson.setContentJson(dto.getContentJson());
        if (dto.getStatus() != null) lesson.setStatus(dto.getStatus());
        if (dto.getOrderIndex() != null) lesson.setOrderIndex(dto.getOrderIndex());
        lessonMapper.updateById(lesson);
        return ApiResponse.ok(lesson);
    }
```

- [ ] **Step 4: 追加 AI 生成课时内容 + 资源推荐方法，关闭类**

```java
    /** POST /api/lessons/{id}/ai-generate — 触发 AI 生成课时内容 */
    @PostMapping("/api/lessons/{id}/ai-generate")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<Map<String, String>> aiGenerateLesson(@PathVariable Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");
        String prompt = String.format(
            "你是一位专业教师，请为课时「%s」生成结构化讲义内容。" +
            "格式为JSON：{\"sections\":[{\"type\":\"text\",\"content\":\"内容\",\"tags\":[\"重点\"]}]}" +
            "要求：3-4个section，包含重点/难点/易错点标注，内容详实，只返回JSON。",
            lesson.getTitle());
        String aiContent = llmClient.chat(prompt);
        if (aiContent == null) aiContent = "{\"sections\":[{\"type\":\"text\",\"content\":\"AI生成失败，请手动填写\",\"tags\":[]}]}";
        lesson.setContentJson(LLMClient.extractJson(aiContent));
        lesson.setStatus("draft");
        lessonMapper.updateById(lesson);
        return ApiResponse.ok(Map.of("contentJson", lesson.getContentJson(), "status", "draft"));
    }

    /** POST /api/lessons/{id}/recommend-resources — 触发资源推荐 */
    @PostMapping("/api/lessons/{id}/recommend-resources")
    public ApiResponse<Object> recommendResources(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) return ApiResponse.fail("课时不存在");
        Long userId = LoginUserHolder.get().getUserId();
        String weakDimension = body != null ? body.getOrDefault("weakDimension", "综合") : "综合";
        String result = webSearchAgent.recommend(lesson.getTitle(), weakDimension, userId, id);
        return ApiResponse.ok(result);
    }
}
```

- [ ] **Step 5: 重构现有 Lesson.java 和 Unit.java 实体，添加 v2 字段**

现有 `Lesson.java` 缺少 `contentJson`、`status`、`orderIndex`、`createdBy`、`createdAt` 字段；`Unit.java` 缺少 `courseId`、`orderIndex`。用以下内容完整替换：

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Lesson.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_lesson")
public class Lesson {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long unitId;
    private String title;
    private String videoUrl;
    private String contentJson;
    private String status;
    private Integer orderIndex;
    private Long createdBy;
    private LocalDateTime createdAt;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Unit.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_unit")
public class Unit {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;
}
```

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Exercise.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_exercise")
public class Exercise {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long lessonId;
    private String questionText;
    private String questionType;
    private String optionsJson;
    private String answer;
    private String explanation;
    private Integer difficulty;
    private String kpTags;
    private Integer orderIndex;
}
```

- [ ] **Step 6: 重构 Course.java 添加 subjectId 字段**

```java
// backend/src/main/java/com/iflytek/smartprep/domain/Course.java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("sp_course")
public class Course {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long subjectId;
    private String title;
    private String description;
    private String coverImage;
    private String difficulty;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 8: 启动后端并测试**

```bash
cd backend && mvn spring-boot:run &
sleep 15
curl -s http://localhost:8080/api/subjects | python3 -m json.tool
# 期望输出：
# [{"id":1,"name":"计算机科学","icon":"computer","color":"#4F46E5",...}]
```

- [ ] **Step 9: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java
git add backend/src/main/java/com/iflytek/smartprep/dto/LessonContentDto.java
git add backend/src/main/java/com/iflytek/smartprep/domain/
git commit -m "feat(api): SubjectCourseController 课程体系 CRUD，重构 Lesson/Unit/Exercise/Course 实体"
```

---

## Task 6: LessonProgressController — 课时进度追踪

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/controller/LessonProgressController.java`

- [ ] **Step 1: 创建 LessonProgressController.java**

```java
// backend/src/main/java/com/iflytek/smartprep/controller/LessonProgressController.java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.Lesson;
import com.iflytek.smartprep.domain.LessonProgress;
import com.iflytek.smartprep.domain.Unit;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.LessonMapper;
import com.iflytek.smartprep.mapper.LessonProgressMapper;
import com.iflytek.smartprep.mapper.UnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class LessonProgressController {

    private final LessonProgressMapper lessonProgressMapper;
    private final LessonMapper lessonMapper;
    private final UnitMapper unitMapper;

    /** GET /api/progress/lesson/{lessonId} — 获取当前用户对某课时的进度 */
    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<LessonProgress> getLessonProgress(@PathVariable Long lessonId) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress progress = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, lessonId));
        if (progress == null) {
            progress = LessonProgress.builder()
                    .userId(userId).lessonId(lessonId).status("not_started").build();
        }
        return ApiResponse.ok(progress);
    }

    /** POST /api/progress/lesson/{lessonId}/complete — 标记课时完成 */
    @PostMapping("/lesson/{lessonId}/complete")
    public ApiResponse<LessonProgress> completeLesson(@PathVariable Long lessonId) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress existing = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getLessonId, lessonId));
        if (existing == null) {
            existing = LessonProgress.builder()
                    .userId(userId).lessonId(lessonId)
                    .status("completed").completedAt(LocalDateTime.now()).build();
            lessonProgressMapper.insert(existing);
        } else {
            existing.setStatus("completed");
            existing.setCompletedAt(LocalDateTime.now());
            lessonProgressMapper.updateById(existing);
        }
        return ApiResponse.ok(existing);
    }

    /** GET /api/progress/course/{courseId} — 获取课程整体进度 */
    @GetMapping("/course/{courseId}")
    public ApiResponse<Map<String, Object>> getCourseProgress(@PathVariable Long courseId) {
        Long userId = LoginUserHolder.get().getUserId();
        List<Unit> units = unitMapper.selectList(
                new LambdaQueryWrapper<Unit>().eq(Unit::getCourseId, courseId));
        List<Long> unitIds = units.stream().map(Unit::getId).toList();
        long totalLessons = 0;
        long completedLessons = 0;
        if (!unitIds.isEmpty()) {
            List<Lesson> lessons = lessonMapper.selectList(
                    new LambdaQueryWrapper<Lesson>().in(Lesson::getUnitId, unitIds));
            totalLessons = lessons.size();
            List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
            if (!lessonIds.isEmpty()) {
                completedLessons = lessonProgressMapper.selectCount(
                        new LambdaQueryWrapper<LessonProgress>()
                                .eq(LessonProgress::getUserId, userId)
                                .in(LessonProgress::getLessonId, lessonIds)
                                .eq(LessonProgress::getStatus, "completed"));
            }
        }
        int percent = totalLessons == 0 ? 0 : (int) (completedLessons * 100 / totalLessons);
        return ApiResponse.ok(Map.of(
                "courseId", courseId,
                "totalLessons", totalLessons,
                "completedLessons", completedLessons,
                "progressPercent", percent));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 3: 测试（需先登录获取 token）**

```bash
# 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 标记课时1完成
curl -s -X POST http://localhost:8080/api/progress/lesson/1/complete \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# 期望：{"success":true,"data":{"status":"completed",...}}

# 验证数据库
mysql -u root -p -e "SELECT * FROM smartprep.sp_lesson_progress WHERE user_id=3;"
# 期望：有一行 lesson_id=1, status=completed
```

- [ ] **Step 4: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/LessonProgressController.java
git commit -m "feat(api): LessonProgressController 课时进度追踪"
```

---

## Task 7: ClassController — 班级管理

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/controller/ClassController.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/dto/ClassDto.java`

- [ ] **Step 1: 创建 ClassDto.java**

```java
// backend/src/main/java/com/iflytek/smartprep/dto/ClassDto.java
package com.iflytek.smartprep.dto;

import lombok.Data;

@Data
public class ClassDto {
    private String name;
    private String description;
    private String inviteCode; // 用于加入班级
}
```

- [ ] **Step 2: 创建 ClassController.java**

```java
// backend/src/main/java/com/iflytek/smartprep/controller/ClassController.java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.ClassMember;
import com.iflytek.smartprep.domain.User;
import com.iflytek.smartprep.domain.ZhiyuClass;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.ClassDto;
import com.iflytek.smartprep.mapper.ClassMemberMapper;
import com.iflytek.smartprep.mapper.UserMapper;
import com.iflytek.smartprep.mapper.ZhiyuClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ZhiyuClassMapper classMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;

    /** POST /api/classes — 教师创建班级 */
    @PostMapping
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<ZhiyuClass> createClass(@RequestBody ClassDto dto) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        ZhiyuClass cls = ZhiyuClass.builder()
                .name(dto.getName())
                .teacherId(LoginUserHolder.get().getUserId())
                .inviteCode(inviteCode)
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        classMapper.insert(cls);
        return ApiResponse.ok(cls);
    }

    /** GET /api/classes/my — 教师获取自己的班级列表 */
    @GetMapping("/my")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<List<ZhiyuClass>> myClasses() {
        Long teacherId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(classMapper.selectList(
                new LambdaQueryWrapper<ZhiyuClass>().eq(ZhiyuClass::getTeacherId, teacherId)));
    }

    /** POST /api/classes/join — 学生通过邀请码加入班级 */
    @PostMapping("/join")
    public ApiResponse<ZhiyuClass> joinClass(@RequestBody ClassDto dto) {
        ZhiyuClass cls = classMapper.selectOne(
                new LambdaQueryWrapper<ZhiyuClass>().eq(ZhiyuClass::getInviteCode, dto.getInviteCode()));
        if (cls == null) return ApiResponse.fail("邀请码无效");
        Long studentId = LoginUserHolder.get().getUserId();
        long exists = classMemberMapper.selectCount(
                new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, cls.getId())
                        .eq(ClassMember::getStudentId, studentId));
        if (exists > 0) return ApiResponse.fail("已在该班级中");
        ClassMember member = ClassMember.builder()
                .classId(cls.getId()).studentId(studentId).joinedAt(LocalDateTime.now()).build();
        classMemberMapper.insert(member);
        return ApiResponse.ok(cls);
    }

    /** GET /api/classes/{id}/members — 获取班级成员列表（teacher only） */
    @GetMapping("/{id}/members")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<List<User>> getMembers(@PathVariable Long id) {
        List<ClassMember> members = classMemberMapper.selectList(
                new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getClassId, id));
        List<Long> studentIds = members.stream().map(ClassMember::getStudentId).toList();
        if (studentIds.isEmpty()) return ApiResponse.ok(List.of());
        return ApiResponse.ok(userMapper.selectBatchIds(studentIds));
    }

    /** DELETE /api/classes/{id}/members/{studentId} — 移除学生（teacher only） */
    @DeleteMapping("/{id}/members/{studentId}")
    @RequireRole("ROLE_TEACHER")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long studentId) {
        classMemberMapper.delete(
                new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, id)
                        .eq(ClassMember::getStudentId, studentId));
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
# 期望：BUILD SUCCESS
```

- [ ] **Step 4: 测试**

```bash
# teacher 创建班级
TEACHER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

INVITE=$(curl -s -X POST http://localhost:8080/api/classes \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"编程入门班","description":"2026春季班"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['inviteCode'])")
echo "邀请码: $INVITE"

# student 加入班级
STUDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

curl -s -X POST http://localhost:8080/api/classes/join \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"inviteCode\":\"$INVITE\"}" | python3 -m json.tool
# 期望：{"success":true,"data":{"name":"编程入门班",...}}
```

- [ ] **Step 5: git commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/ClassController.java
git add backend/src/main/java/com/iflytek/smartprep/dto/ClassDto.java
git commit -m "feat(api): ClassController 班级管理（创建/加入/成员管理）"
```
