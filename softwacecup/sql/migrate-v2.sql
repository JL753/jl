-- ============================================
-- 知域 v2 增量迁移
-- 基于现有 v1 表结构，新增班级/作业/进度/能力等模块
-- ============================================

-- ============================================
-- 1. 新增表
-- ============================================

-- 课时进度
CREATE TABLE IF NOT EXISTS sp_lesson_progress (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT NOT NULL,
  status VARCHAR(32) DEFAULT 'not_started',
  completed_at DATETIME,
  UNIQUE KEY uk_lesson_progress (user_id, lesson_id),
  INDEX idx_lp_user (user_id),
  INDEX idx_lp_lesson (lesson_id)
);

-- 班级
CREATE TABLE IF NOT EXISTS sp_class (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  teacher_id BIGINT NOT NULL,
  invite_code VARCHAR(32) UNIQUE,
  description TEXT,
  created_at DATETIME,
  INDEX idx_class_teacher (teacher_id)
);

-- 班级成员
CREATE TABLE IF NOT EXISTS sp_class_member (
  id BIGINT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at DATETIME,
  UNIQUE KEY uk_class_student (class_id, student_id),
  INDEX idx_cm_class (class_id),
  INDEX idx_cm_student (student_id)
);

-- 作业
CREATE TABLE IF NOT EXISTS sp_assignment (
  id BIGINT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  lesson_ids_json TEXT,
  description TEXT,
  due_at DATETIME,
  created_at DATETIME,
  INDEX idx_assign_class (class_id)
);

-- 作业提交
CREATE TABLE IF NOT EXISTS sp_assignment_submission (
  id BIGINT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(32) DEFAULT 'pending',
  score INT,
  feedback TEXT,
  submitted_at DATETIME,
  UNIQUE KEY uk_assign_submit (assignment_id, student_id),
  INDEX idx_as_assign (assignment_id),
  INDEX idx_as_student (student_id)
);

-- AI 内容审核
CREATE TABLE IF NOT EXISTS sp_content_review (
  id BIGINT PRIMARY KEY,
  lesson_id BIGINT NOT NULL,
  ai_draft_json LONGTEXT,
  status VARCHAR(32) DEFAULT 'pending',
  reviewer_id BIGINT,
  reviewed_at DATETIME,
  INDEX idx_cr_lesson (lesson_id),
  INDEX idx_cr_status (status)
);

-- 六维能力历史
CREATE TABLE IF NOT EXISTS sp_student_ability (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  breadth_score INT DEFAULT 0,
  depth_score INT DEFAULT 0,
  problem_score INT DEFAULT 0,
  activity_score INT DEFAULT 0,
  transfer_score INT DEFAULT 0,
  resilience_score INT DEFAULT 0,
  diagnosis TEXT,
  evaluated_at DATETIME,
  INDEX idx_sa_user (user_id)
);

-- AI 推荐资源
CREATE TABLE IF NOT EXISTS sp_resource_recommendation (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT,
  resources_json LONGTEXT,
  created_at DATETIME,
  INDEX idx_rr_user (user_id)
);

-- ============================================
-- 2. 练习题增加课时关联字段
-- ============================================
-- 使用预处理语句避免重复执行报错
SET @db_name = (SELECT DATABASE());
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sp_exercise' AND COLUMN_NAME = 'lesson_id');

SET @alter_sql = IF(@col_exists = 0,
  'ALTER TABLE sp_exercise ADD COLUMN lesson_id BIGINT COMMENT ''关联课时''',
  'SELECT 1'
);
PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. 课时增加 status 字段（draft/published）
-- ============================================
SET @col_exists2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = (SELECT DATABASE()) AND TABLE_NAME = 'sp_lesson' AND COLUMN_NAME = 'status');
SET @alter_sql2 = IF(@col_exists2 = 0,
  'ALTER TABLE sp_lesson ADD COLUMN status VARCHAR(32) DEFAULT ''published''',
  'SELECT 1'
);
PREPARE stmt2 FROM @alter_sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 更新已有时课状态
UPDATE sp_lesson SET status = 'published' WHERE status IS NULL;

-- ============================================
-- 4. 习题关联课时（基于知识点→课时映射）
-- ============================================
UPDATE sp_exercise SET lesson_id = 11101 WHERE knowledge_point_id = 2001;
UPDATE sp_exercise SET lesson_id = 11102 WHERE knowledge_point_id = 2004;
UPDATE sp_exercise SET lesson_id = 11201 WHERE knowledge_point_id = 2005;
UPDATE sp_exercise SET lesson_id = 11202 WHERE knowledge_point_id = 2007;
UPDATE sp_exercise SET lesson_id = 11202 WHERE knowledge_point_id = 2008;
UPDATE sp_exercise SET lesson_id = 11301 WHERE knowledge_point_id = 2010;
UPDATE sp_exercise SET lesson_id = 11302 WHERE knowledge_point_id = 2012;
UPDATE sp_exercise SET lesson_id = 11302 WHERE knowledge_point_id = 2013;

-- ============================================
-- 5. 种子数据：计算机科学预置课程
-- ============================================
-- 仅在学科表为空时插入
INSERT IGNORE INTO sp_subject (id, name, icon, color, description, sort_order) VALUES
(1001, '计算机科学', 'computer', '#4F46E5', '涵盖编程基础、数据结构、算法等核心课程', 1);

INSERT IGNORE INTO sp_course (id, title, category, description, cover_image, difficulty, price, tag, status, total_hours, target_audience, chapters_json, created_by, created_at, updated_at) VALUES
(1001, '编程入门', '编程基础', '从零开始学习编程，掌握基本语法与逻辑思维', '', 'beginner', '免费', '编程', '已发布', 20, '零基础初学者', '[{"title":"第一单元：编程基础概念","description":"了解什么是程序、变量、数据类型"},{"title":"第二单元：流程控制","description":"掌握条件判断与循环结构"},{"title":"第三单元：函数与模块","description":"学习函数定义、参数传递与模块化"}]', 2, NOW(), NOW());

INSERT IGNORE INTO sp_unit (id, name, description, subject_id, sort_order) VALUES
(1001, '编程基础概念', '了解什么是程序、变量、数据类型', 1001, 1),
(1002, '流程控制', '掌握条件判断与循环结构', 1001, 2),
(1003, '函数与模块', '学习函数定义、参数传递与模块化', 1001, 3);

INSERT IGNORE INTO sp_lesson (id, unit_id, name, type, video_url, duration, content, status, sort_order) VALUES
(10001, 1001, '什么是程序', 'doc', '', 0, '## 什么是程序\n\n程序是一组让计算机执行特定任务的指令集合。\n\n### 核心概念\n- **指令**：计算机执行的基本操作\n- **程序**：一系列指令的集合\n- **算法**：解决问题的步骤\n\n程序由三个基本要素组成：输入（Input）、处理（Process）、输出（Output），简称 IPO 模型。', 'published', 1),
(10002, 1001, '变量与数据类型', 'doc', '', 0, '## 变量与数据类型\n\n变量是存储数据的容器。\n\n### Python 基本数据类型\n- `int`：整数，如 42\n- `float`：浮点数，如 3.14\n- `str`：字符串，如 "Hello"\n- `bool`：布尔值，如 True\n\n变量命名规则：只能包含字母、数字、下划线；不能以数字开头；区分大小写。', 'published', 2),
(10003, 1001, '输入与输出', 'doc', '', 0, '## 输入与输出\n\n### input() 函数\n从键盘读取用户输入，返回值始终是字符串。\n\n### print() 函数\n输出内容到屏幕。\n\n### 格式化输出\n推荐使用 f-string：`f"姓名：{name}"`', 'published', 3),
(10004, 1002, '条件判断 if-else', 'doc', '', 0, '## 条件判断\n\n### if-elif-else 结构\n```python\nif 条件1:\n    代码块1\nelif 条件2:\n    代码块2\nelse:\n    代码块3\n```\n\n比较运算符：==, !=, >, <, >=, <=\n逻辑运算符：and, or, not', 'published', 1),
(10005, 1002, 'for 循环', 'doc', '', 0, '## for 循环\n\n用于遍历序列。\n\n```python\nfor i in range(5):\n    print(i)\n```\n\n- `break`：退出循环\n- `continue`：跳过本次循环', 'published', 2),
(10006, 1002, 'while 循环', 'doc', '', 0, '## while 循环\n\n条件为真时反复执行循环体。\n\n```python\nn = 0\nwhile n < 5:\n    print(n)\n    n += 1\n```\n\n注意：必须更新循环变量，否则造成死循环。', 'published', 3),
(10007, 1003, '函数定义与调用', 'doc', '', 0, '## 函数\n\n可复用的代码块。\n\n```python\ndef greet(name):\n    return f"你好, {name}!"\n```\n\n参数类型：位置参数、默认参数、可变参数。', 'published', 1),
(10008, 1003, '作用域与变量生命周期', 'doc', '', 0, '## 作用域\n\nLEGB 规则：Local → Enclosing → Global → Built-in\n\n函数内部可以读取全局变量，修改需用 `global` 关键字。', 'published', 2),
(10009, 1003, '模块与导入', 'doc', '', 0, '## 模块\n\n模块是 .py 文件，用 import 导入。\n\n```python\nimport math\nprint(math.sqrt(16))\n```\n\n标准库：math, random, datetime, os, sys', 'published', 3);

INSERT IGNORE INTO sp_exercise (id, lesson_id, knowledge_point_id, type, difficulty, content_json, answer, explanation) VALUES
(10001, 10001, NULL, 'choice', 1, '{"question":"以下哪项最准确地描述了\\"程序\\"?","options":["一种硬件设备","让计算机执行特定任务的指令集合","操作系统的别称","编程语言本身"]}', 'B', '程序是指令的集合，告诉计算机如何完成特定任务。'),
(10002, 10001, NULL, 'single', 1, '{"question":"IPO 模型中，P 代表什么？","options":["Print","Program","Process","Python"]}', 'C', 'IPO 是 Input（输入）、Process（处理）、Output（输出）的缩写。'),
(10003, 10002, NULL, 'choice', 1, '{"question":"以下变量名合法的是？","options":["2name","my-var","_score","class"]}', '_count', '变量名不能以数字开头、不能包含连字符、不能是关键字。'),
(10004, 10002, NULL, 'choice', 1, '{"question":"Python 中 5/2 的结果是？","options":["2","2.5","2.0","报错"]}', 'B', 'Python 3 中 / 是真除法，5/2=2.5。'),
(10005, 10003, NULL, 'single', 1, '{"question":"input() 函数返回值的类型是？","options":["int","float","str","取决于用户输入的内容"]}', 'str', 'input() 始终返回字符串类型。'),
(10006, 10004, NULL, 'choice', 1, '{"question":"判断 x 在 1 到 10 之间的正确写法是？","options":["if 1 <= x <= 10:","if x >= 1 and x <= 10:","以上两种都正确","if x in range(1,11):"]}', 'C', 'Python 支持链式比较和 and 连接两种方式。'),
(10007, 10005, NULL, 'single', 1, '{"question":"range(1, 10, 2) 生成的序列是？","options":["1,3,5,7,9","1,3,5,7,9,10","2,4,6,8","1,2,3,4,5,6,7,8,9"]}', '1,3,5,7,9', '从1开始，步长2，不含10。'),
(10008, 10005, NULL, 'single', 1, '{"question":"break 在循环中的作用是？","options":["跳过本次循环","立即退出整个循环","退出当前函数","暂停循环"]}', 'B', 'break 立即退出整个循环结构。'),
(10009, 10006, NULL, 'choice', 1, '{"question":"以下 while 循环执行几次？\\nn=0\\nwhile n<3:\\n    n+=1","options":["0次","2次","3次","无限次"]}', 'C', 'n 从0开始，当 n=0,1,2 时条件成立。'),
(10010, 10007, NULL, 'single', 1, '{"question":"函数没有 return 语句时，返回值是？","options":["0","空字符串","None","报错"]}', 'None', 'Python 函数默认返回 None。'),
(10011, 10008, NULL, 'single', 2, '{"question":"在函数内修改全局变量，需要使用哪个关键字？","options":["global","nonlocal","extern","public"]}', 'global', 'global 声明变量为全局变量，允许在函数内修改。'),
(10012, 10009, NULL, 'single', 1, '{"question":"导入 math 模块中 sqrt 函数的正确写法是？","options":["import sqrt from math","from math import sqrt","include math.sqrt","using math.sqrt"]}', 'from math import sqrt', 'Python 中从模块导入特定函数的正确语法。');

-- 为用户 3（student）添加部分掌握度数据
INSERT IGNORE INTO sp_user_kp_mastery (id, user_id, knowledge_point_id, mastery, practice_count, correct_count, last_practice_at)
SELECT 1, 3, kp.id, 0.85, 3, 2, NOW() FROM sp_knowledge_point kp WHERE kp.lesson_id = 11101 LIMIT 1;
