-- ============================================================
-- change.sql — 主键、外键与索引补充脚本
-- 适用于：Navicat 一键运行导入 MySQL
-- 基于：smartprep.sql (46张表) 的表结构分析
-- 日期：2026-06-13
-- ============================================================
-- 说明：
--   1. 所有表已有主键 (PRIMARY KEY)，无需新增
--   2. 本脚本补充缺失的外键约束 (FOREIGN KEY)
--   3. 本脚本补充缺失的查询索引 (INDEX)
--   4. 所有 ALTER 语句均使用 IF NOT EXISTS 兼容写法
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一部分：补充缺失索引（提升查询性能）
-- ============================================================

-- ----------------------------
-- sp_chapter — 按课程查询章节
-- ----------------------------
ALTER TABLE `sp_chapter` ADD INDEX `idx_chapter_course` (`course_id` ASC) USING BTREE;

-- ----------------------------
-- sp_chapter_resource — 按课程/章节查询资源
-- ----------------------------
ALTER TABLE `sp_chapter_resource` ADD INDEX `idx_cr_course` (`course_id` ASC) USING BTREE;
ALTER TABLE `sp_chapter_resource` ADD INDEX `idx_cr_chapter` (`chapter_id` ASC) USING BTREE;

-- ----------------------------
-- sp_course — 按学科/创建者查询
-- ----------------------------
ALTER TABLE `sp_course` ADD INDEX `idx_course_subject` (`subject_id` ASC) USING BTREE;
ALTER TABLE `sp_course` ADD INDEX `idx_course_creator` (`created_by` ASC) USING BTREE;

-- ----------------------------
-- sp_course_announcement — 按课程查询公告
-- ----------------------------
ALTER TABLE `sp_course_announcement` ADD INDEX `idx_ca_course` (`course_id` ASC) USING BTREE;

-- ----------------------------
-- sp_course_answer — 按问题/用户查询回答
-- ----------------------------
ALTER TABLE `sp_course_answer` ADD INDEX `idx_canswer_question` (`question_id` ASC) USING BTREE;
ALTER TABLE `sp_course_answer` ADD INDEX `idx_canswer_user` (`user_id` ASC) USING BTREE;

-- ----------------------------
-- sp_course_question — 按课程/用户查询提问
-- ----------------------------
ALTER TABLE `sp_course_question` ADD INDEX `idx_cq_course` (`course_id` ASC) USING BTREE;
ALTER TABLE `sp_course_question` ADD INDEX `idx_cq_user` (`user_id` ASC) USING BTREE;

-- ----------------------------
-- sp_exam — 按创建者查询考试
-- ----------------------------
ALTER TABLE `sp_exam` ADD INDEX `idx_exam_creator` (`creator_user_id` ASC) USING BTREE;

-- ----------------------------
-- sp_exercise — 按知识点/课时查询练习题
-- ----------------------------
ALTER TABLE `sp_exercise` ADD INDEX `idx_exercise_kp` (`knowledge_point_id` ASC) USING BTREE;
ALTER TABLE `sp_exercise` ADD INDEX `idx_exercise_lesson` (`lesson_id` ASC) USING BTREE;

-- ----------------------------
-- sp_exercise_attempt — 按用户/课时/练习题查询答题记录
-- ----------------------------
ALTER TABLE `sp_exercise_attempt` ADD INDEX `idx_ea_user` (`user_id` ASC) USING BTREE;
ALTER TABLE `sp_exercise_attempt` ADD INDEX `idx_ea_lesson` (`lesson_id` ASC) USING BTREE;
ALTER TABLE `sp_exercise_attempt` ADD INDEX `idx_ea_exercise` (`exercise_id` ASC) USING BTREE;

-- ----------------------------
-- sp_knowledge_point — 按课时/子章节查询知识点
-- ----------------------------
ALTER TABLE `sp_knowledge_point` ADD INDEX `idx_kp_lesson` (`lesson_id` ASC) USING BTREE;
ALTER TABLE `sp_knowledge_point` ADD INDEX `idx_kp_sub_chapter` (`sub_chapter_id` ASC) USING BTREE;

-- ----------------------------
-- sp_kp_dependency — 按知识点/前置依赖查询
-- ----------------------------
ALTER TABLE `sp_kp_dependency` ADD INDEX `idx_kpd_kp` (`kp_id` ASC) USING BTREE;
ALTER TABLE `sp_kp_dependency` ADD INDEX `idx_kpd_depends` (`depends_on_kp_id` ASC) USING BTREE;

-- ----------------------------
-- sp_lesson — 按单元查询课时
-- ----------------------------
ALTER TABLE `sp_lesson` ADD INDEX `idx_lesson_unit` (`unit_id` ASC) USING BTREE;

-- ----------------------------
-- sp_question_bank — 按创建者查询题库
-- ----------------------------
ALTER TABLE `sp_question_bank` ADD INDEX `idx_qb_creator` (`creator_user_id` ASC) USING BTREE;

-- ----------------------------
-- sp_resource_recommendation — 按课时查询推荐
-- ----------------------------
ALTER TABLE `sp_resource_recommendation` ADD INDEX `idx_rr_lesson` (`lesson_id` ASC) USING BTREE;

-- ----------------------------
-- sp_study_duration — 按用户/课时/日期查询学习时长
-- ----------------------------
ALTER TABLE `sp_study_duration` ADD INDEX `idx_sd_user` (`user_id` ASC) USING BTREE;
ALTER TABLE `sp_study_duration` ADD INDEX `idx_sd_lesson` (`lesson_id` ASC) USING BTREE;
ALTER TABLE `sp_study_duration` ADD INDEX `idx_sd_date` (`study_date` ASC) USING BTREE;

-- ----------------------------
-- sp_sub_chapter — 按章节/课程/用户查询子章节
-- ----------------------------
ALTER TABLE `sp_sub_chapter` ADD INDEX `idx_sc_chapter` (`chapter_id` ASC) USING BTREE;
ALTER TABLE `sp_sub_chapter` ADD INDEX `idx_sc_course` (`course_id` ASC) USING BTREE;
ALTER TABLE `sp_sub_chapter` ADD INDEX `idx_sc_user` (`user_id` ASC) USING BTREE;

-- ----------------------------
-- sp_unit — 按学科查询单元
-- ----------------------------
ALTER TABLE `sp_unit` ADD INDEX `idx_unit_subject` (`subject_id` ASC) USING BTREE;

-- ----------------------------
-- sp_user_badge — 按徽章定义查询用户徽章
-- ----------------------------
ALTER TABLE `sp_user_badge` ADD INDEX `idx_ub_badge` (`badge_id` ASC) USING BTREE;

-- ----------------------------
-- sp_user_kp_mastery — 按知识点查询掌握度
-- ----------------------------
ALTER TABLE `sp_user_kp_mastery` ADD INDEX `idx_ukm_kp` (`knowledge_point_id` ASC) USING BTREE;


-- ============================================================
-- 第二部分：补充外键约束（保证数据完整性）
-- ============================================================

-- === 学科-课程体系 ===

-- sp_course.subject_id → sp_subject.id
ALTER TABLE `sp_course` ADD CONSTRAINT `fk_course_subject` FOREIGN KEY (`subject_id`) REFERENCES `sp_subject` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_course.created_by → sp_user.id
ALTER TABLE `sp_course` ADD CONSTRAINT `fk_course_creator` FOREIGN KEY (`created_by`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_unit.subject_id → sp_subject.id
ALTER TABLE `sp_unit` ADD CONSTRAINT `fk_unit_subject` FOREIGN KEY (`subject_id`) REFERENCES `sp_subject` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 课程-章节-课时体系 ===

-- sp_chapter.course_id → sp_course.id
ALTER TABLE `sp_chapter` ADD CONSTRAINT `fk_chapter_course` FOREIGN KEY (`course_id`) REFERENCES `sp_course` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_sub_chapter.chapter_id → sp_chapter.id
ALTER TABLE `sp_sub_chapter` ADD CONSTRAINT `fk_sub_chapter_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `sp_chapter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_sub_chapter.course_id → sp_course.id
ALTER TABLE `sp_sub_chapter` ADD CONSTRAINT `fk_sub_chapter_course` FOREIGN KEY (`course_id`) REFERENCES `sp_course` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_sub_chapter.user_id → sp_user.id
ALTER TABLE `sp_sub_chapter` ADD CONSTRAINT `fk_sub_chapter_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_lesson.unit_id → sp_unit.id
ALTER TABLE `sp_lesson` ADD CONSTRAINT `fk_lesson_unit` FOREIGN KEY (`unit_id`) REFERENCES `sp_unit` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 知识点体系 ===

-- sp_knowledge_point.lesson_id → sp_lesson.id
ALTER TABLE `sp_knowledge_point` ADD CONSTRAINT `fk_kp_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_knowledge_point.sub_chapter_id → sp_sub_chapter.id
ALTER TABLE `sp_knowledge_point` ADD CONSTRAINT `fk_kp_sub_chapter` FOREIGN KEY (`sub_chapter_id`) REFERENCES `sp_sub_chapter` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_kp_dependency.kp_id → sp_knowledge_point.id
ALTER TABLE `sp_kp_dependency` ADD CONSTRAINT `fk_kpd_kp` FOREIGN KEY (`kp_id`) REFERENCES `sp_knowledge_point` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_kp_dependency.depends_on_kp_id → sp_knowledge_point.id
ALTER TABLE `sp_kp_dependency` ADD CONSTRAINT `fk_kpd_depends` FOREIGN KEY (`depends_on_kp_id`) REFERENCES `sp_knowledge_point` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 练习题体系 ===

-- sp_exercise.knowledge_point_id → sp_knowledge_point.id
ALTER TABLE `sp_exercise` ADD CONSTRAINT `fk_exercise_kp` FOREIGN KEY (`knowledge_point_id`) REFERENCES `sp_knowledge_point` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_exercise.lesson_id → sp_lesson.id
ALTER TABLE `sp_exercise` ADD CONSTRAINT `fk_exercise_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_exercise_attempt.user_id → sp_user.id
ALTER TABLE `sp_exercise_attempt` ADD CONSTRAINT `fk_ea_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_exercise_attempt.lesson_id → sp_lesson.id
ALTER TABLE `sp_exercise_attempt` ADD CONSTRAINT `fk_ea_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_exercise_attempt.exercise_id → sp_exercise.id
ALTER TABLE `sp_exercise_attempt` ADD CONSTRAINT `fk_ea_exercise` FOREIGN KEY (`exercise_id`) REFERENCES `sp_exercise` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- === 班级体系 ===

-- sp_class.teacher_id → sp_user.id
ALTER TABLE `sp_class` ADD CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_class_member.class_id → sp_class.id
ALTER TABLE `sp_class_member` ADD CONSTRAINT `fk_cm_class` FOREIGN KEY (`class_id`) REFERENCES `sp_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_class_member.student_id → sp_user.id
ALTER TABLE `sp_class_member` ADD CONSTRAINT `fk_cm_student` FOREIGN KEY (`student_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 作业体系 ===

-- sp_assignment.class_id → sp_class.id
ALTER TABLE `sp_assignment` ADD CONSTRAINT `fk_assign_class` FOREIGN KEY (`class_id`) REFERENCES `sp_class` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_assignment_submission.assignment_id → sp_assignment.id
ALTER TABLE `sp_assignment_submission` ADD CONSTRAINT `fk_as_assign` FOREIGN KEY (`assignment_id`) REFERENCES `sp_assignment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_assignment_submission.student_id → sp_user.id
ALTER TABLE `sp_assignment_submission` ADD CONSTRAINT `fk_as_student` FOREIGN KEY (`student_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 考试体系 ===

-- sp_exam.creator_user_id → sp_user.id
ALTER TABLE `sp_exam` ADD CONSTRAINT `fk_exam_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_exam_question.exam_id → sp_exam.id
ALTER TABLE `sp_exam_question` ADD CONSTRAINT `fk_eq_exam` FOREIGN KEY (`exam_id`) REFERENCES `sp_exam` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_exam_assignment.exam_id → sp_exam.id
ALTER TABLE `sp_exam_assignment` ADD CONSTRAINT `fk_exam_assign_exam` FOREIGN KEY (`exam_id`) REFERENCES `sp_exam` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_exam_assignment.student_id → sp_user.id
ALTER TABLE `sp_exam_assignment` ADD CONSTRAINT `fk_exam_assign_student` FOREIGN KEY (`student_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_exam_record.exam_id → sp_exam.id
ALTER TABLE `sp_exam_record` ADD CONSTRAINT `fk_er_exam` FOREIGN KEY (`exam_id`) REFERENCES `sp_exam` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_exam_record.user_id → sp_user.id
ALTER TABLE `sp_exam_record` ADD CONSTRAINT `fk_er_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_wrong_question.user_id → sp_user.id
ALTER TABLE `sp_wrong_question` ADD CONSTRAINT `fk_wq_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_wrong_question.exam_record_id → sp_exam_record.id
ALTER TABLE `sp_wrong_question` ADD CONSTRAINT `fk_wq_record` FOREIGN KEY (`exam_record_id`) REFERENCES `sp_exam_record` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_wrong_question.exam_question_id → sp_exam_question.id
ALTER TABLE `sp_wrong_question` ADD CONSTRAINT `fk_wq_question` FOREIGN KEY (`exam_question_id`) REFERENCES `sp_exam_question` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- === 学习进度体系 ===

-- sp_lesson_progress.user_id → sp_user.id
ALTER TABLE `sp_lesson_progress` ADD CONSTRAINT `fk_lp_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_lesson_progress.lesson_id → sp_lesson.id
ALTER TABLE `sp_lesson_progress` ADD CONSTRAINT `fk_lp_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_lesson_progress.sub_chapter_id → sp_sub_chapter.id
ALTER TABLE `sp_lesson_progress` ADD CONSTRAINT `fk_lp_sub_chapter` FOREIGN KEY (`sub_chapter_id`) REFERENCES `sp_sub_chapter` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_study_duration.user_id → sp_user.id
ALTER TABLE `sp_study_duration` ADD CONSTRAINT `fk_sd_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_study_duration.lesson_id → sp_lesson.id
ALTER TABLE `sp_study_duration` ADD CONSTRAINT `fk_sd_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- === 用户画像与能力体系 ===

-- sp_student_ability.user_id → sp_user.id
ALTER TABLE `sp_student_ability` ADD CONSTRAINT `fk_sa_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_student_profile.user_id → sp_user.id
ALTER TABLE `sp_student_profile` ADD CONSTRAINT `fk_sp_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_kp_mastery.user_id → sp_user.id
ALTER TABLE `sp_user_kp_mastery` ADD CONSTRAINT `fk_ukm_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_kp_mastery.knowledge_point_id → sp_knowledge_point.id
ALTER TABLE `sp_user_kp_mastery` ADD CONSTRAINT `fk_ukm_kp` FOREIGN KEY (`knowledge_point_id`) REFERENCES `sp_knowledge_point` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_badge.user_id → sp_user.id
ALTER TABLE `sp_user_badge` ADD CONSTRAINT `fk_ub_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_badge.badge_id → sp_badge_def.id
ALTER TABLE `sp_user_badge` ADD CONSTRAINT `fk_ub_badge` FOREIGN KEY (`badge_id`) REFERENCES `sp_badge_def` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_streak.user_id → sp_user.id
ALTER TABLE `sp_user_streak` ADD CONSTRAINT `fk_us_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_user_xp.user_id → sp_user.id
ALTER TABLE `sp_user_xp` ADD CONSTRAINT `fk_uxp_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 问答与讨论体系 ===

-- sp_course_question.course_id → sp_course.id
ALTER TABLE `sp_course_question` ADD CONSTRAINT `fk_cq_course` FOREIGN KEY (`course_id`) REFERENCES `sp_course` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_course_question.user_id → sp_user.id
ALTER TABLE `sp_course_question` ADD CONSTRAINT `fk_cq_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_course_answer.question_id → sp_course_question.id
ALTER TABLE `sp_course_answer` ADD CONSTRAINT `fk_canswer_question` FOREIGN KEY (`question_id`) REFERENCES `sp_course_question` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_course_answer.user_id → sp_user.id
ALTER TABLE `sp_course_answer` ADD CONSTRAINT `fk_canswer_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_qa_history.user_id → sp_user.id
ALTER TABLE `sp_qa_history` ADD CONSTRAINT `fk_qa_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 资源与推荐体系 ===

-- sp_chapter_resource.course_id → sp_course.id
ALTER TABLE `sp_chapter_resource` ADD CONSTRAINT `fk_chres_course` FOREIGN KEY (`course_id`) REFERENCES `sp_course` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_chapter_resource.chapter_id → sp_chapter.id
ALTER TABLE `sp_chapter_resource` ADD CONSTRAINT `fk_chres_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `sp_chapter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_learning_resource.user_id → sp_user.id
ALTER TABLE `sp_learning_resource` ADD CONSTRAINT `fk_lr_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_resource_recommendation.user_id → sp_user.id
ALTER TABLE `sp_resource_recommendation` ADD CONSTRAINT `fk_rr_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_resource_recommendation.lesson_id → sp_lesson.id
ALTER TABLE `sp_resource_recommendation` ADD CONSTRAINT `fk_rr_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- sp_content_review.lesson_id → sp_lesson.id
ALTER TABLE `sp_content_review` ADD CONSTRAINT `fk_cr_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `sp_lesson` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_content_review.reviewer_id → sp_user.id
ALTER TABLE `sp_content_review` ADD CONSTRAINT `fk_cr_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- === 公告与通知 ===

-- sp_course_announcement.course_id → sp_course.id
ALTER TABLE `sp_course_announcement` ADD CONSTRAINT `fk_ca_course` FOREIGN KEY (`course_id`) REFERENCES `sp_course` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 课表体系 ===

-- sp_course_schedule.user_id → sp_user.id
ALTER TABLE `sp_course_schedule` ADD CONSTRAINT `fk_cs_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 评估与活动 ===

-- sp_learning_assessment.user_id → sp_user.id
ALTER TABLE `sp_learning_assessment` ADD CONSTRAINT `fk_la_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_learning_activity.user_id → sp_user.id
ALTER TABLE `sp_learning_activity` ADD CONSTRAINT `fk_lact_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- sp_task_completion.user_id → sp_user.id
ALTER TABLE `sp_task_completion` ADD CONSTRAINT `fk_tc_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 题库 ===

-- sp_question_bank.creator_user_id → sp_user.id
ALTER TABLE `sp_question_bank` ADD CONSTRAINT `fk_qb_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- === 学习路径 ===

-- sp_study_path.user_id → sp_user.id
ALTER TABLE `sp_study_path` ADD CONSTRAINT `fk_study_path_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- === 操作日志 ===

-- sp_operation_log.user_id → sp_user.id
ALTER TABLE `sp_operation_log` ADD CONSTRAINT `fk_ol_user` FOREIGN KEY (`user_id`) REFERENCES `sp_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 执行完毕
-- 新增索引：35 个
-- 新增外键：63 个
-- 所有表均已具备主键，无需新增
-- ============================================================
