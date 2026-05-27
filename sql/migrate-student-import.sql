-- 学生端B站导入：sp_lesson新增user_id字段
ALTER TABLE sp_lesson ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT 'NULL=公共课程库，有值=该用户个人导入';
CREATE INDEX idx_lesson_user_id ON sp_lesson(user_id);
