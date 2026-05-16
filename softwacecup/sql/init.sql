CREATE DATABASE IF NOT EXISTS smartprep DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smartprep;

DROP TABLE IF EXISTS sp_exam_assignment;
DROP TABLE IF EXISTS sp_ppt_template;
DROP TABLE IF EXISTS sp_question_bank;
DROP TABLE IF EXISTS sp_wrong_question;
DROP TABLE IF EXISTS sp_exam_record;
DROP TABLE IF EXISTS sp_exam_question;
DROP TABLE IF EXISTS sp_exam;
DROP TABLE IF EXISTS sp_learning_assessment;
DROP TABLE IF EXISTS sp_study_path;
DROP TABLE IF EXISTS sp_learning_resource;
DROP TABLE IF EXISTS sp_qa_history;
DROP TABLE IF EXISTS sp_course_schedule;
DROP TABLE IF EXISTS sp_student_profile;
DROP TABLE IF EXISTS sp_knowledge_doc;
DROP TABLE IF EXISTS sp_user;

CREATE TABLE sp_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  role VARCHAR(32) NOT NULL,
  display_name VARCHAR(64) NOT NULL
);

CREATE TABLE sp_student_profile (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  major VARCHAR(64),
  course VARCHAR(64),
  knowledge_base VARCHAR(64),
  cognitive_style VARCHAR(64),
  weak_points VARCHAR(255),
  interest_preference VARCHAR(128),
  pace_preference VARCHAR(64),
  exam_goal VARCHAR(64),
  profile_json TEXT,
  updated_at DATETIME,
  INDEX idx_profile_user (user_id)
);

CREATE TABLE sp_learning_resource (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  resource_type VARCHAR(64),
  title VARCHAR(128),
  content LONGTEXT,
  links_json TEXT,
  confidence INT,
  created_at DATETIME,
  INDEX idx_res_user (user_id)
);

CREATE TABLE sp_study_path (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(128),
  steps_json TEXT,
  push_json TEXT,
  created_at DATETIME,
  INDEX idx_path_user (user_id)
);

CREATE TABLE sp_learning_assessment (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  score_dimension_json TEXT,
  diagnosis TEXT,
  optimize_suggestion TEXT,
  created_at DATETIME,
  INDEX idx_assess_user (user_id)
);

CREATE TABLE sp_knowledge_doc (
  id BIGINT PRIMARY KEY,
  course VARCHAR(64),
  title VARCHAR(128),
  content LONGTEXT,
  tag VARCHAR(64)
);

CREATE TABLE sp_exam (
  id BIGINT PRIMARY KEY,
  creator_user_id BIGINT,
  exam_name VARCHAR(128),
  course VARCHAR(64),
  duration INT,
  topic VARCHAR(255),
  status VARCHAR(32),
  question_count INT,
  created_at DATETIME
);

CREATE TABLE sp_exam_question (
  id BIGINT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  question_no INT,
  question_type VARCHAR(32),
  title VARCHAR(255),
  score INT,
  answer_key TEXT,
  INDEX idx_exam_question_exam (exam_id)
);

CREATE TABLE sp_exam_record (
  id BIGINT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT,
  review TEXT,
  answers_json TEXT,
  submitted_at DATETIME,
  INDEX idx_exam_record_exam (exam_id),
  INDEX idx_exam_record_user (user_id)
);

CREATE TABLE sp_wrong_question (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  exam_record_id BIGINT,
  exam_question_id BIGINT,
  question_title VARCHAR(255),
  my_answer TEXT,
  correct_answer TEXT,
  analysis TEXT,
  created_at DATETIME,
  INDEX idx_wrong_user (user_id),
  INDEX idx_wrong_record (exam_record_id)
);

CREATE TABLE sp_question_bank (
  id BIGINT PRIMARY KEY,
  creator_user_id BIGINT,
  course VARCHAR(64),
  title VARCHAR(255),
  category1 VARCHAR(64),
  category2 VARCHAR(64),
  category3 VARCHAR(64),
  question_type VARCHAR(32),
  difficulty VARCHAR(32),
  options_json TEXT,
  correct_answer TEXT,
  analysis TEXT,
  image_url VARCHAR(255),
  created_at DATETIME,
  INDEX idx_question_course (course)
);

CREATE TABLE sp_ppt_template (
  id BIGINT PRIMARY KEY,
  template_name VARCHAR(128),
  template_code VARCHAR(64),
  cover_url VARCHAR(255),
  scene_tag VARCHAR(64),
  description VARCHAR(255),
  preview_json TEXT,
  theme_color VARCHAR(32),
  created_at DATETIME
);

CREATE TABLE sp_exam_assignment (
  id BIGINT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  created_at DATETIME,
  INDEX idx_assignment_exam (exam_id),
  INDEX idx_assignment_student (student_id)
);

-- 课程表
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

-- AI问答历史记录
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

-- 学习活跃度记录
CREATE TABLE sp_learning_activity (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  activity_date DATE NOT NULL COMMENT '活动日期',
  activity_score INT DEFAULT 0 COMMENT '活跃值分数',
  login_count INT DEFAULT 0 COMMENT '登录次数',
  study_minutes INT DEFAULT 0 COMMENT '学习时长(分钟)',
  question_count INT DEFAULT 0 COMMENT '提问次数',
  resource_view_count INT DEFAULT 0 COMMENT '资源查看次数',
  created_at DATETIME,
  INDEX idx_activity_user_date (user_id, activity_date)
);

-- ============================================
-- 课程体系（学科→单元→课时）
-- ============================================
CREATE TABLE sp_subject (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL COMMENT '学科名称',
  icon VARCHAR(32) COMMENT '图标',
  color VARCHAR(16) COMMENT '主题色',
  description VARCHAR(512) COMMENT '学科描述',
  sort_order INT DEFAULT 0 COMMENT '排序'
);

CREATE TABLE sp_unit (
  id BIGINT PRIMARY KEY,
  subject_id BIGINT NOT NULL COMMENT '所属学科',
  name VARCHAR(128) NOT NULL COMMENT '单元名称',
  description VARCHAR(512) COMMENT '单元描述',
  sort_order INT DEFAULT 0 COMMENT '排序',
  prerequisite_unit_id BIGINT COMMENT '前置单元ID',
  INDEX idx_unit_subject (subject_id)
);

CREATE TABLE sp_lesson (
  id BIGINT PRIMARY KEY,
  unit_id BIGINT NOT NULL COMMENT '所属单元',
  name VARCHAR(128) NOT NULL COMMENT '课时名称',
  type VARCHAR(32) DEFAULT 'doc' COMMENT '类型: video/doc/quiz',
  video_url VARCHAR(512) COMMENT '视频地址',
  duration INT COMMENT '时长(秒)',
  content LONGTEXT COMMENT '课时正文(markdown)',
  sort_order INT DEFAULT 0 COMMENT '排序',
  INDEX idx_lesson_unit (unit_id)
);

-- ============================================
-- 知识图谱
-- ============================================
CREATE TABLE sp_knowledge_point (
  id BIGINT PRIMARY KEY,
  lesson_id BIGINT COMMENT '关联课时',
  name VARCHAR(128) NOT NULL COMMENT '知识点名称',
  description VARCHAR(512) COMMENT '简述',
  difficulty_level INT DEFAULT 1 COMMENT '难度 1-5',
  tags VARCHAR(255) COMMENT '标签(逗号分隔)',
  INDEX idx_kp_lesson (lesson_id)
);

CREATE TABLE sp_kp_dependency (
  id BIGINT PRIMARY KEY,
  prerequisite_id BIGINT NOT NULL COMMENT '前驱知识点',
  successor_id BIGINT NOT NULL COMMENT '后继知识点',
  relation_type VARCHAR(32) DEFAULT 'requires' COMMENT '关系类型',
  INDEX idx_dep_prereq (prerequisite_id),
  INDEX idx_dep_succ (successor_id)
);

CREATE TABLE sp_user_kp_mastery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL,
  mastery DECIMAL(4,3) DEFAULT 0.000 COMMENT '掌握度 0.000-1.000',
  practice_count INT DEFAULT 0 COMMENT '练习次数',
  correct_count INT DEFAULT 0 COMMENT '正确次数',
  last_practice_at DATETIME COMMENT '最后练习时间',
  INDEX idx_mastery_user (user_id),
  UNIQUE KEY uk_user_kp (user_id, knowledge_point_id)
);

CREATE TABLE sp_exercise (
  id BIGINT PRIMARY KEY,
  knowledge_point_id BIGINT NOT NULL COMMENT '关联知识点',
  type VARCHAR(32) DEFAULT 'choice' COMMENT '类型: choice/judge/fill/code',
  difficulty INT DEFAULT 1 COMMENT '难度 1-5',
  content_json TEXT COMMENT '题目内容JSON',
  answer TEXT COMMENT '正确答案',
  explanation TEXT COMMENT '解析',
  INDEX idx_exercise_kp (knowledge_point_id)
);

-- ============================================
-- 种子数据：编程基础学科
-- ============================================
INSERT INTO sp_subject (id, name, icon, color, description, sort_order) VALUES
(1001, '编程基础', '💻', '#3b82f6', '从零开始学习编程，掌握核心概念与基本技能', 1),
(1002, '数据结构与算法', '🔢', '#8b5cf6', '深入理解经典数据结构与算法设计思想', 2),
(1003, 'Web开发', '🌐', '#10b981', '学习前端与后端技术，构建完整的Web应用', 3),
(1004, '人工智能', '🤖', '#f59e0b', '从机器学习到深度学习，探索AI核心技术', 4);

-- 编程基础 → 单元
INSERT INTO sp_unit (id, subject_id, name, description, sort_order, prerequisite_unit_id) VALUES
(1101, 1001, '编程入门', '了解什么是编程，安装开发环境，编写第一个程序', 1, NULL),
(1102, 1001, '变量与数据类型', '掌握变量声明、基本数据类型与类型转换', 2, 1101),
(1103, 1001, '控制流程', '条件判断、循环结构与流程控制', 3, 1102),
(1104, 1001, '函数与模块', '函数定义、参数传递、作用域与模块化编程', 4, 1103);

-- 数据结构 → 单元
INSERT INTO sp_unit (id, subject_id, name, description, sort_order, prerequisite_unit_id) VALUES
(1201, 1002, '线性结构', '数组、链表、栈与队列', 1, NULL),
(1202, 1002, '树结构', '二叉树、二叉搜索树、堆与哈夫曼树', 2, 1201),
(1203, 1002, '图结构', '图的表示、遍历与最短路径', 3, 1202),
(1204, 1002, '排序与搜索', '经典排序算法与搜索策略', 4, 1201);

-- AI → 单元
INSERT INTO sp_unit (id, subject_id, name, description, sort_order, prerequisite_unit_id) VALUES
(1401, 1004, '机器学习基础', '监督学习、无监督学习、模型评估', 1, NULL),
(1402, 1004, '神经网络', '感知机、多层网络、激活函数', 2, 1401),
(1403, 1004, '深度学习', 'CNN、RNN、Transformer架构', 3, 1402);

-- 编程入门 → 课时
INSERT INTO sp_lesson (id, unit_id, name, type, video_url, duration, content, sort_order) VALUES
(11101, 1101, '什么是编程', 'video', '', 480, '## 什么是编程\n\n编程是用计算机能理解的语言，告诉计算机该做什么。\n\n### 核心概念\n- **程序**：一系列指令的集合\n- **编程语言**：人类与计算机沟通的桥梁\n- **算法**：解决问题的步骤\n\n### 为什么学编程？\n1. 培养逻辑思维\n2. 自动化重复工作\n3. 创造有用的工具\n', 1),
(11102, 1101, '开发环境搭建', 'doc', '', 300, '## 开发环境搭建\n\n### 步骤\n1. 下载并安装 Python 3.10+\n2. 安装 VS Code\n3. 配置 Python 扩展\n4. 编写 Hello World\n\n```python\nprint("Hello, World!")\n```\n', 2),
(11103, 1101, '编程初体验', 'quiz', '', 0, '## 编程初体验测验\n\n完成以下测试检验你的理解。', 3);

INSERT INTO sp_lesson (id, unit_id, name, type, video_url, duration, content, sort_order) VALUES
(11201, 1102, '变量声明与赋值', 'video', '', 420, '## 变量声明与赋值\n\n变量是存储数据的容器。\n\n### Python 变量\n```python\nname = "张三"\nage = 20\nscore = 95.5\nis_pass = True\n```\n\n### 命名规则\n- 只能包含字母、数字、下划线\n- 不能以数字开头\n- 区分大小写\n', 1),
(11202, 1102, '基本数据类型', 'doc', '', 360, '## 基本数据类型\n\n| 类型 | 示例 | 说明 |\n|------|------|------|\n| int | 42 | 整数 |\n| float | 3.14 | 浮点数 |\n| str | "hello" | 字符串 |\n| bool | True | 布尔值 |\n| list | [1,2,3] | 列表 |\n| dict | {"key":"val"} | 字典 |\n', 2),
(11203, 1102, '数据类型练习', 'quiz', '', 0, '', 3);

INSERT INTO sp_lesson (id, unit_id, name, type, video_url, duration, content, sort_order) VALUES
(11301, 1103, '条件判断', 'video', '', 500, '## 条件判断\n\n### if-elif-else\n```python\nscore = 85\nif score >= 90:\n    grade = "A"\nelif score >= 80:\n    grade = "B"\nelif score >= 70:\n    grade = "C"\nelse:\n    grade = "D"\n```\n', 1),
(11302, 1103, '循环结构', 'video', '', 480, '## 循环结构\n\n### for 循环\n```python\nfor i in range(5):\n    print(f"第{i+1}次")\n```\n\n### while 循环\n```python\ncount = 0\nwhile count < 5:\n    print(count)\n    count += 1\n```\n', 2);

-- 知识点
INSERT INTO sp_knowledge_point (id, lesson_id, name, description, difficulty_level, tags) VALUES
(2001, 11101, '程序概念', '理解程序、编程语言、算法的基本定义', 1, '编程,基础'),
(2002, 11101, '编程语言分类', '了解编译型与解释型语言的区别', 1, '编程,基础'),
(2003, 11102, '开发环境', '能独立搭建 Python 开发环境', 1, '工具,环境'),
(2004, 11102, 'Hello World', '理解并运行第一个程序', 1, '编程,入门'),
(2005, 11201, '变量概念', '理解变量的作用和声明方式', 1, '变量,基础'),
(2006, 11201, '赋值语句', '掌握赋值运算符和多重赋值', 1, '变量,运算符'),
(2007, 11202, '整数与浮点数', '掌握整数和浮点数的运算与转换', 1, '数据类型,数字'),
(2008, 11202, '字符串操作', '掌握字符串的拼接、切片与格式化', 2, '数据类型,字符串'),
(2009, 11202, '列表与字典', '理解列表和字典的创建与基本操作', 2, '数据类型,容器'),
(2010, 11301, 'if语句', '掌握单分支、双分支、多分支条件判断', 2, '控制流程,条件'),
(2011, 11301, '布尔表达式', '理解比较运算符和逻辑运算符', 2, '控制流程,运算符'),
(2012, 11302, 'for循环', '掌握 for 循环遍历序列', 2, '控制流程,循环'),
(2013, 11302, 'while循环', '掌握 while 循环与循环控制', 2, '控制流程,循环');

-- 知识点依赖关系（前驱→后继）
INSERT INTO sp_kp_dependency (id, prerequisite_id, successor_id, relation_type) VALUES
(3001, 2001, 2004, 'requires'),
(3002, 2001, 2005, 'requires'),
(3003, 2004, 2005, 'requires'),
(3004, 2005, 2006, 'requires'),
(3005, 2005, 2007, 'requires'),
(3006, 2006, 2008, 'requires'),
(3007, 2007, 2008, 'requires'),
(3008, 2007, 2009, 'requires'),
(3009, 2008, 2009, 'requires'),
(3010, 2006, 2010, 'requires'),
(3011, 2008, 2011, 'requires'),
(3012, 2009, 2012, 'requires'),
(3013, 2012, 2013, 'requires');

-- 练习题
INSERT INTO sp_exercise (id, knowledge_point_id, type, difficulty, content_json, answer, explanation) VALUES
(4001, 2001, 'judge', 1, '{"question":"编程语言是人与计算机沟通的桥梁，这句话对吗？"}', '对', '编程语言本质上就是人与计算机之间的沟通工具。'),
(4002, 2004, 'fill', 1, '{"question":"在Python中，使用 _____ 函数可以在控制台输出文字。"}', 'print', 'print()是Python内置的输出函数。'),
(4003, 2005, 'choice', 1, '{"question":"以下哪个是合法的Python变量名？","options":["1name","my-var","_count","class"]}', '_count', '变量名不能以数字开头、不能包含连字符、不能是关键字。'),
(4004, 2007, 'choice', 1, '{"question":"表达式 5/2 的结果是什么？","options":["2","2.5","2.0","3"]}', '2.5', 'Python中 / 执行浮点除法，返回精确结果。'),
(4005, 2008, 'fill', 2, '{"question":"想要获取字符串 s=\\"hello\\" 的前3个字符，应写 ____"}', 's[0:3]', 'Python字符串切片语法：s[start:end]，左闭右开。'),
(4006, 2010, 'choice', 2, '{"question":"以下代码输出什么？\\n```python\\nx = 10\\nif x > 5:\\n    print(\"A\")\\nelif x > 8:\\n    print(\"B\")\\nelse:\\n    print(\"C\")\\n```","options":["A","B","C","AB"]}', 'A', 'x>5为True，执行第一个分支后不再继续判断。'),
(4007, 2012, 'fill', 2, '{"question":"for i in _____(5):  # 在横线上填写，让循环执行5次"}', 'range', 'range(5)生成0-4的整数序列。'),
(4008, 2013, 'choice', 2, '{"question":"以下哪个关键字用于提前结束当前循环迭代？","options":["break","continue","pass","return"]}', 'continue', 'continue跳过本次迭代，继续下一次循环。break完全退出循环。');

-- 为学生账号添加部分知识点掌握度数据
INSERT INTO sp_user_kp_mastery (user_id, knowledge_point_id, mastery, practice_count, correct_count, last_practice_at) VALUES
(3, 2001, 0.92, 5, 4, NOW()),
(3, 2004, 1.00, 3, 3, NOW()),
(3, 2005, 0.85, 6, 4, NOW()),
(3, 2006, 0.78, 4, 3, NOW()),
(3, 2007, 0.70, 5, 3, NOW()),
(3, 2008, 0.45, 3, 1, NOW()),
(3, 2009, 0.20, 2, 0, NOW());

-- 学习任务完成度
CREATE TABLE sp_task_completion (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  week_start_date DATE NOT NULL COMMENT '周开始日期',
  task_type VARCHAR(64) NOT NULL COMMENT '任务类型：阅读/练习/视频/测验/项目/讨论',
  completion_rate INT DEFAULT 0 COMMENT '完成率 0-100',
  total_count INT DEFAULT 0 COMMENT '总任务数',
  completed_count INT DEFAULT 0 COMMENT '已完成数',
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_task_user_week (user_id, week_start_date)
);

-- 密码已 SHA-256 哈希，原始密码均为 123456
INSERT INTO sp_user (id, username, password, role, display_name) VALUES
(1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'admin', '系统管理员'),
(2, 'teacher', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'teacher', '王老师'),
(3, 'student', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'student', '张同学'),
(4, 'student02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'student', '李同学'),
(5, 'student03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'student', '陈同学');

INSERT INTO sp_knowledge_doc (id, course, title, content, tag) VALUES
(101, '人工智能导论', '机器学习基础', '监督学习、无监督学习、强化学习的核心定义与差异。', '基础'),
(102, '人工智能导论', '模型评估', '准确率、召回率、F1、ROC-AUC 的适用场景。', '评估'),
(103, '人工智能导论', '过拟合与欠拟合', '偏差-方差权衡与正则化策略。', '难点');

-- 为学生演示账号添加大三数据科学与大数据技术专业课表
INSERT INTO sp_course_schedule (id, user_id, day_of_week, start_time, end_time, course_name, location, teacher, weeks, created_at, updated_at) VALUES
-- 周一
(1001, 3, 1, '08:00', '09:40', '大数据处理技术', '教学楼A301', '张教授', '1-16', NOW(), NOW()),
(1002, 3, 1, '10:00', '11:40', '机器学习', '教学楼B205', '李教授', '1-16', NOW(), NOW()),
(1003, 3, 1, '14:00', '15:40', '数据挖掘', '实验楼C102', '王老师', '1-16', NOW(), NOW()),
-- 周二
(1004, 3, 2, '08:00', '09:40', 'Python数据分析', '实验楼C201', '刘老师', '1-16', NOW(), NOW()),
(1005, 3, 2, '10:00', '11:40', '深度学习', '教学楼A402', '陈教授', '1-16', NOW(), NOW()),
-- 周三
(1006, 3, 3, '08:00', '09:40', '数据可视化', '实验楼C105', '赵老师', '1-16', NOW(), NOW()),
(1007, 3, 3, '14:00', '15:40', '大数据系统架构', '教学楼B301', '孙教授', '1-16', NOW(), NOW()),
-- 周四
(1008, 3, 4, '08:00', '09:40', 'Spark大数据开发', '实验楼C203', '周老师', '1-16', NOW(), NOW()),
(1009, 3, 4, '10:00', '11:40', '自然语言处理', '教学楼A305', '吴教授', '1-16', NOW(), NOW()),
-- 周五
(1010, 3, 5, '08:00', '09:40', '计算机视觉', '教学楼B402', '郑教授', '1-16', NOW(), NOW()),
(1011, 3, 5, '14:00', '15:40', '数据科学综合实践', '实验楼C301', '钱老师', '1-16', NOW(), NOW());

-- 为学生演示账号添加近7日学习活跃度数据
INSERT INTO sp_learning_activity (id, user_id, activity_date, activity_score, login_count, study_minutes, question_count, resource_view_count, created_at) VALUES
(2001, 3, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 23, 2, 45, 3, 8, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2002, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 38, 3, 85, 5, 12, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2003, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 31, 2, 62, 4, 9, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2004, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 45, 4, 105, 7, 15, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2005, 3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 29, 2, 58, 3, 7, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2006, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 18, 1, 35, 2, 5, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2007, 3, CURDATE(), 12, 1, 25, 1, 3, NOW());

-- 为学生演示账号添加本周学习任务完成度数据
INSERT INTO sp_task_completion (id, user_id, week_start_date, task_type, completion_rate, total_count, completed_count, created_at, updated_at) VALUES
(3001, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '阅读', 85, 20, 17, NOW(), NOW()),
(3002, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '练习', 72, 25, 18, NOW(), NOW()),
(3003, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '视频', 90, 10, 9, NOW(), NOW()),
(3004, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '测验', 68, 15, 10, NOW(), NOW()),
(3005, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '项目', 78, 5, 4, NOW(), NOW()),
(3006, 3, DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '讨论', 82, 8, 7, NOW(), NOW());

-- ============================================
-- 游戏化系统（XP/徽章/打卡）
-- ============================================
CREATE TABLE sp_user_xp (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  current_xp INT DEFAULT 0 COMMENT '当前总经验值',
  level INT DEFAULT 1 COMMENT '当前等级',
  title VARCHAR(64) DEFAULT '初学者' COMMENT '等级称号',
  created_at DATETIME,
  updated_at DATETIME,
  UNIQUE KEY uk_xp_user (user_id)
);

CREATE TABLE sp_badge_def (
  id BIGINT PRIMARY KEY,
  name VARCHAR(64) NOT NULL COMMENT '徽章名称',
  description VARCHAR(255) COMMENT '获取条件描述',
  icon VARCHAR(32) COMMENT '图标',
  color VARCHAR(16) COMMENT '颜色',
  unlock_rule VARCHAR(255) COMMENT '解锁规则JSON',
  sort_order INT DEFAULT 0
);

CREATE TABLE sp_user_badge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  badge_id BIGINT NOT NULL,
  unlocked_at DATETIME COMMENT '解锁时间',
  INDEX idx_ub_user (user_id)
);

CREATE TABLE sp_user_streak (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  current_streak INT DEFAULT 0 COMMENT '当前连续天数',
  longest_streak INT DEFAULT 0 COMMENT '最长连续天数',
  last_checkin_date DATE COMMENT '最后签到日期',
  total_checkins INT DEFAULT 0 COMMENT '总签到次数',
  updated_at DATETIME,
  UNIQUE KEY uk_streak_user (user_id)
);

-- 徽章定义（种子数据）
INSERT INTO sp_badge_def (id, name, description, icon, color, unlock_rule, sort_order) VALUES
(1, '初识门径', '完成首次登录', '🌟', '#f59e0b', '{"type":"first_login"}', 1),
(2, '勤学不辍', '连续学习7天', '🔥', '#ef4444', '{"type":"streak","days":7}', 2),
(3, '刷题达人', '累计完成100道练习题', '✏️', '#3b82f6', '{"type":"quiz_count","count":100}', 3),
(4, '求知若渴', '向AI导师提问20次', '🤖', '#8b5cf6', '{"type":"qa_count","count":20}', 4),
(5, '全科掌握', '所有知识点掌握度≥60%', '🎯', '#10b981', '{"type":"all_mastery","threshold":0.6}', 5),
(6, '学霸之冠', '考试平均分≥85分', '👑', '#f59e0b', '{"type":"exam_avg","score":85}', 6),
(7, '视频达人', '累计观看视频10小时', '📹', '#ec4899', '{"type":"video_hours","hours":10}', 7),
(8, '完美主义', '一次练习全对', '💎', '#06b6d4', '{"type":"perfect_quiz"}', 8),
(9, '终身学习', '累计学习30天', '📚', '#6366f1', '{"type":"total_days","days":30}', 9),
(10, '社区贡献', '帮助其他同学解答5个问题', '🤝', '#14b8a6', '{"type":"help_count","count":5}', 10),
(11, '知识探索者', '解锁10个知识点', '🗺️', '#f97316', '{"type":"kp_unlocked","count":10}', 11),
(12, '速度之星', '在限时挑战中排名前3', '⚡', '#eab308', '{"type":"race_top3"}', 12);

-- 为学生演示账号添加游戏化数据
INSERT INTO sp_user_xp (id, user_id, current_xp, level, title, created_at, updated_at) VALUES
(1, 3, 8650, 12, '知识探索者', NOW(), NOW());

INSERT INTO sp_user_badge (id, user_id, badge_id, unlocked_at) VALUES
(1, 3, 1, NOW()), (2, 3, 2, NOW()), (3, 3, 3, NOW()),
(4, 3, 4, NOW()), (5, 3, 7, NOW()), (6, 3, 9, NOW()),
(7, 3, 11, NOW()), (8, 3, 12, NOW());

INSERT INTO sp_user_streak (id, user_id, current_streak, longest_streak, last_checkin_date, total_checkins, updated_at) VALUES
(1, 3, 7, 14, CURDATE(), 28, NOW());

-- ============================================
-- v2 新增表（课程重构扩展）
-- ============================================

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

CREATE TABLE IF NOT EXISTS sp_class (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  teacher_id BIGINT NOT NULL,
  invite_code VARCHAR(32) UNIQUE,
  description TEXT,
  created_at DATETIME,
  INDEX idx_class_teacher (teacher_id)
);

CREATE TABLE IF NOT EXISTS sp_class_member (
  id BIGINT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at DATETIME,
  UNIQUE KEY uk_class_student (class_id, student_id),
  INDEX idx_cm_class (class_id),
  INDEX idx_cm_student (student_id)
);

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

CREATE TABLE IF NOT EXISTS sp_resource_recommendation (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT,
  resources_json LONGTEXT,
  created_at DATETIME,
  INDEX idx_rr_user (user_id)
);
