-- ============================================
-- migrate-v3: 重构学习层级为 Subject→Course→Chapter→SubChapter
-- 迁移路径: sp_unit→sp_chapter, sp_lesson→sp_sub_chapter
-- 新增: sp_chapter_resource, sp_course_announcement, sp_course_question, sp_course_answer
-- ============================================

-- ============================================
-- 1. 为 sp_course 添加新字段（安全添加，已存在则跳过）
-- ============================================

-- subject_id: 关联学科
SET @col_subject = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_course' AND COLUMN_NAME = 'subject_id');
SET @alter_subject = IF(@col_subject = 0,
  'ALTER TABLE sp_course ADD COLUMN subject_id BIGINT AFTER id',
  'SELECT 1 AS skipped_subject_id'
);
PREPARE stmt FROM @alter_subject; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- background: 课程背景
SET @col_bg = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_course' AND COLUMN_NAME = 'background');
SET @alter_bg = IF(@col_bg = 0,
  'ALTER TABLE sp_course ADD COLUMN background VARCHAR(512) AFTER target_audience',
  'SELECT 1 AS skipped_background'
);
PREPARE stmt FROM @alter_bg; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- target: 课程目标
SET @col_target = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_course' AND COLUMN_NAME = 'target');
SET @alter_target = IF(@col_target = 0,
  'ALTER TABLE sp_course ADD COLUMN target VARCHAR(512) AFTER background',
  'SELECT 1 AS skipped_target'
);
PREPARE stmt FROM @alter_target; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- principle: 课程理念
SET @col_principle = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_course' AND COLUMN_NAME = 'principle');
SET @alter_principle = IF(@col_principle = 0,
  'ALTER TABLE sp_course ADD COLUMN principle VARCHAR(512) AFTER target',
  'SELECT 1 AS skipped_principle'
);
PREPARE stmt FROM @alter_principle; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 为 subject_id 创建索引
SET @idx_subject = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_course' AND INDEX_NAME = 'idx_course_subject');
SET @create_idx_subject = IF(@idx_subject = 0,
  'ALTER TABLE sp_course ADD INDEX idx_course_subject (subject_id)',
  'SELECT 1 AS skipped_idx_course_subject'
);
PREPARE stmt FROM @create_idx_subject; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================
-- 2. 创建 sp_chapter（合并原 sp_unit 功能）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_chapter (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL COMMENT '所属课程',
  title VARCHAR(256) NOT NULL COMMENT '章节标题',
  description TEXT COMMENT '章节描述',
  sort_order INT DEFAULT 0 COMMENT '排序',
  prerequisite_chapter_id BIGINT COMMENT '前置章节ID',
  created_at DATETIME,
  INDEX idx_chapter_course (course_id)
);


-- ============================================
-- 3. 创建 sp_sub_chapter（合并原 sp_lesson 功能）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_sub_chapter (
  id BIGINT PRIMARY KEY,
  chapter_id BIGINT NOT NULL COMMENT '所属章节',
  title VARCHAR(256) NOT NULL COMMENT '子章节标题',
  description TEXT COMMENT '子章节描述',
  sort_order INT DEFAULT 0 COMMENT '排序',
  type VARCHAR(32) DEFAULT 'doc' COMMENT '类型: video/doc/quiz',
  video_url VARCHAR(512) COMMENT '视频地址',
  duration INT COMMENT '时长(秒)',
  content LONGTEXT COMMENT '正文(markdown)',
  cover_url VARCHAR(512) COMMENT '封面图',
  status VARCHAR(32) DEFAULT 'published' COMMENT '状态',
  user_id BIGINT COMMENT '创建者',
  INDEX idx_subchapter_chapter (chapter_id)
);


-- ============================================
-- 4. 创建 sp_chapter_resource（章节资源）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_chapter_resource (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL COMMENT '所属课程',
  chapter_id BIGINT NOT NULL COMMENT '所属章节',
  type VARCHAR(32) COMMENT '资源类型: 视频/课件/文档',
  title VARCHAR(256) NOT NULL COMMENT '资源名称',
  description TEXT COMMENT '资源描述',
  url VARCHAR(512) COMMENT '资源地址',
  size VARCHAR(32) COMMENT '文件大小',
  created_at DATETIME,
  INDEX idx_res_course (course_id),
  INDEX idx_res_chapter (chapter_id)
);


-- ============================================
-- 5. 创建 sp_course_announcement（课程公告）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_course_announcement (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL COMMENT '所属课程',
  type VARCHAR(32) DEFAULT '普通公告' COMMENT '普通公告/重要公告/警示公告',
  title VARCHAR(256) NOT NULL COMMENT '公告标题',
  content TEXT COMMENT '公告内容',
  created_at DATETIME,
  INDEX idx_ann_course (course_id)
);


-- ============================================
-- 6. 创建 sp_course_question（课程问答）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_course_question (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL COMMENT '所属课程',
  user_id BIGINT NOT NULL COMMENT '提问者',
  title VARCHAR(256) NOT NULL COMMENT '问题标题',
  content TEXT COMMENT '问题内容',
  created_at DATETIME,
  INDEX idx_q_course (course_id),
  INDEX idx_q_user (user_id)
);


-- ============================================
-- 7. 创建 sp_course_answer（问答回复）
-- ============================================
CREATE TABLE IF NOT EXISTS sp_course_answer (
  id BIGINT PRIMARY KEY,
  question_id BIGINT NOT NULL COMMENT '所属问题',
  user_id BIGINT COMMENT '回答者(NULL=AI)',
  content TEXT NOT NULL COMMENT '回答内容',
  is_ai TINYINT(1) DEFAULT 0 COMMENT '是否AI生成',
  created_at DATETIME,
  INDEX idx_answer_question (question_id)
);


-- ============================================
-- 8. 数据迁移：sp_unit → sp_chapter
-- 策略：为每个没有对应 Course 的学科创建占位 Course
-- ============================================

-- 8.1 为没有 Course 的学科创建占位 Course
INSERT IGNORE INTO sp_course (id, subject_id, title, category, description, status, created_at, updated_at)
SELECT
  s.id + 7000 AS id,
  s.id AS subject_id,
  CONCAT(s.name, ' - 课程') AS title,
  s.name AS category,
  s.description AS description,
  '已发布' AS status,
  NOW() AS created_at,
  NOW() AS updated_at
FROM sp_subject s
WHERE NOT EXISTS (
  SELECT 1 FROM sp_course c WHERE c.subject_id = s.id
);

-- 8.2 将已有 Course 关联到学科（通过 category 匹配）
-- 对于已有 category 但 subject_id 为 NULL 的 Course，尝试匹配学科名
UPDATE sp_course c
JOIN sp_subject s ON c.category = s.name
SET c.subject_id = s.id
WHERE c.subject_id IS NULL;

-- 8.3 迁移 Unit → Chapter
INSERT IGNORE INTO sp_chapter (id, course_id, title, description, sort_order, prerequisite_chapter_id, created_at)
SELECT
  u.id AS id,
  COALESCE(
    (SELECT c.id FROM sp_course c WHERE c.subject_id = u.subject_id LIMIT 1),
    u.subject_id + 7000
  ) AS course_id,
  u.name AS title,
  u.description,
  u.sort_order,
  u.prerequisite_unit_id,
  NOW() AS created_at
FROM sp_unit u
WHERE NOT EXISTS (
  SELECT 1 FROM sp_chapter ch WHERE ch.id = u.id
);


-- ============================================
-- 9. 数据迁移：sp_lesson → sp_sub_chapter
-- ============================================
INSERT IGNORE INTO sp_sub_chapter (id, chapter_id, title, description, sort_order, type, video_url, duration, content, status, user_id)
SELECT
  l.id AS id,
  l.unit_id AS chapter_id,
  l.name AS title,
  '' AS description,
  l.sort_order,
  COALESCE(l.type, 'doc') AS type,
  l.video_url,
  l.duration,
  l.content,
  COALESCE(l.status, 'published') AS status,
  NULL AS user_id
FROM sp_lesson l
WHERE NOT EXISTS (
  SELECT 1 FROM sp_sub_chapter sc WHERE sc.id = l.id
);


-- ============================================
-- 10. 更新依赖表外键（lesson_id → sub_chapter_id）
-- ============================================

-- 10.1 sp_lesson_progress：lesson_id → sub_chapter_id
SET @col_lp = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_lesson_progress' AND COLUMN_NAME = 'sub_chapter_id');
SET @alter_lp = IF(@col_lp = 0,
  'ALTER TABLE sp_lesson_progress ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id',
  'SELECT 1 AS skipped_lp_subchapter'
);
PREPARE stmt FROM @alter_lp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 填充数据（仅当新列为 NULL 时更新）
UPDATE sp_lesson_progress SET sub_chapter_id = lesson_id WHERE sub_chapter_id IS NULL AND lesson_id IS NOT NULL;

-- 删除旧列（先检查新列有数据）
SET @lp_has_data = (SELECT COUNT(*) FROM sp_lesson_progress WHERE sub_chapter_id IS NOT NULL);
SET @lp_keep_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_lesson_progress' AND COLUMN_NAME = 'lesson_id');
SET @drop_lp = IF(@lp_keep_col > 0 AND @lp_has_data > 0,
  'ALTER TABLE sp_lesson_progress DROP COLUMN lesson_id',
  'SELECT 1 AS skipped_drop_lp_lesson'
);
PREPARE stmt FROM @drop_lp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 确保新列有唯一约束
SET @uk_lp = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_lesson_progress' AND INDEX_NAME = 'uk_subchapter_progress');
SET @create_uk_lp = IF(@uk_lp = 0,
  'ALTER TABLE sp_lesson_progress ADD UNIQUE KEY uk_subchapter_progress (user_id, sub_chapter_id)',
  'SELECT 1 AS skipped_uk_subchapter_progress'
);
PREPARE stmt FROM @create_uk_lp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 确保新列有索引
SET @idx_lp = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_lesson_progress' AND INDEX_NAME = 'idx_scp_subchapter');
SET @create_idx_lp = IF(@idx_lp = 0,
  'ALTER TABLE sp_lesson_progress ADD INDEX idx_scp_subchapter (sub_chapter_id)',
  'SELECT 1 AS skipped_idx_scp_subchapter'
);
PREPARE stmt FROM @create_idx_lp; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- 10.2 sp_knowledge_point：lesson_id → sub_chapter_id
SET @col_kp = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_knowledge_point' AND COLUMN_NAME = 'sub_chapter_id');
SET @alter_kp = IF(@col_kp = 0,
  'ALTER TABLE sp_knowledge_point ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id',
  'SELECT 1 AS skipped_kp_subchapter'
);
PREPARE stmt FROM @alter_kp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sp_knowledge_point SET sub_chapter_id = lesson_id WHERE sub_chapter_id IS NULL AND lesson_id IS NOT NULL;

SET @kp_has_data = (SELECT COUNT(*) FROM sp_knowledge_point WHERE sub_chapter_id IS NOT NULL);
SET @kp_keep_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_knowledge_point' AND COLUMN_NAME = 'lesson_id');
SET @drop_kp = IF(@kp_keep_col > 0 AND @kp_has_data > 0,
  'ALTER TABLE sp_knowledge_point DROP COLUMN lesson_id',
  'SELECT 1 AS skipped_drop_kp_lesson'
);
PREPARE stmt FROM @drop_kp; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 确保新列有索引
SET @idx_kp = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_knowledge_point' AND INDEX_NAME = 'idx_kp_subchapter');
SET @create_idx_kp = IF(@idx_kp = 0,
  'ALTER TABLE sp_knowledge_point ADD INDEX idx_kp_subchapter (sub_chapter_id)',
  'SELECT 1 AS skipped_idx_kp_subchapter'
);
PREPARE stmt FROM @create_idx_kp; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- 10.3 sp_content_review：lesson_id → sub_chapter_id
SET @col_cr = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_content_review' AND COLUMN_NAME = 'sub_chapter_id');
SET @alter_cr = IF(@col_cr = 0,
  'ALTER TABLE sp_content_review ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id',
  'SELECT 1 AS skipped_cr_subchapter'
);
PREPARE stmt FROM @alter_cr; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sp_content_review SET sub_chapter_id = lesson_id WHERE sub_chapter_id IS NULL AND lesson_id IS NOT NULL;

SET @cr_has_data = (SELECT COUNT(*) FROM sp_content_review WHERE sub_chapter_id IS NOT NULL);
SET @cr_keep_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_content_review' AND COLUMN_NAME = 'lesson_id');
SET @drop_cr = IF(@cr_keep_col > 0 AND @cr_has_data > 0,
  'ALTER TABLE sp_content_review DROP COLUMN lesson_id',
  'SELECT 1 AS skipped_drop_cr_lesson'
);
PREPARE stmt FROM @drop_cr; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 确保新列有索引
SET @idx_cr = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_content_review' AND INDEX_NAME = 'idx_cr_subchapter');
SET @create_idx_cr = IF(@idx_cr = 0,
  'ALTER TABLE sp_content_review ADD INDEX idx_cr_subchapter (sub_chapter_id)',
  'SELECT 1 AS skipped_idx_cr_subchapter'
);
PREPARE stmt FROM @create_idx_cr; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- 10.4 sp_resource_recommendation：lesson_id → sub_chapter_id
SET @col_rr = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_resource_recommendation' AND COLUMN_NAME = 'sub_chapter_id');
SET @alter_rr = IF(@col_rr = 0,
  'ALTER TABLE sp_resource_recommendation ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id',
  'SELECT 1 AS skipped_rr_subchapter'
);
PREPARE stmt FROM @alter_rr; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sp_resource_recommendation SET sub_chapter_id = lesson_id WHERE sub_chapter_id IS NULL AND lesson_id IS NOT NULL;

SET @rr_has_data = (SELECT COUNT(*) FROM sp_resource_recommendation WHERE sub_chapter_id IS NOT NULL);
SET @rr_keep_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sp_resource_recommendation' AND COLUMN_NAME = 'lesson_id');
SET @drop_rr = IF(@rr_keep_col > 0 AND @rr_has_data > 0,
  'ALTER TABLE sp_resource_recommendation DROP COLUMN lesson_id',
  'SELECT 1 AS skipped_drop_rr_lesson'
);
PREPARE stmt FROM @drop_rr; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================
-- 11. 旧表清理（确认迁移数据完整后方可取消注释执行）
-- ============================================
-- DROP TABLE IF EXISTS sp_lesson;
-- DROP TABLE IF EXISTS sp_unit;
