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
