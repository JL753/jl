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
