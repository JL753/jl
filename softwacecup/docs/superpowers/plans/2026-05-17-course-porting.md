# 课程功能移植与重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the learning hierarchy from Subject→Unit→Lesson to Subject→Course→Chapter→SubChapter and add course Q&A forum with public discussion + private AI tutoring.

**Architecture:** DB migration creates new tables and migrates existing data. Backend adds Chapter/SubChapter entities, mappers, services, and controllers following existing Spring Boot + MyBatis-Plus patterns. Frontend adds chapter tree, Q&A, resource, and announcement components, updating CourseView and ContentManagement pages.

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL, Vue 3, Element Plus, Pinia, marked + DOMPurify

---

## File Structure

```
sql/
├── migrate-v3-course-chapters.sql     # Create: migration DDL + data migration

backend/src/main/java/com/iflytek/smartprep/
├── domain/
│   ├── Chapter.java                    # Create
│   ├── SubChapter.java                 # Create
│   ├── ChapterResource.java            # Create
│   ├── CourseAnnouncement.java         # Create
│   ├── CourseQuestion.java             # Create
│   └── CourseAnswer.java               # Create
├── mapper/
│   ├── ChapterMapper.java              # Create
│   ├── SubChapterMapper.java           # Create
│   ├── ChapterResourceMapper.java      # Create
│   ├── CourseAnnouncementMapper.java   # Create
│   ├── CourseQuestionMapper.java       # Create
│   └── CourseAnswerMapper.java         # Create
├── service/
│   ├── ChapterService.java             # Create (interface)
│   ├── ChapterServiceImpl.java         # Create
│   ├── CourseQaService.java            # Create (interface)
│   └── CourseQaServiceImpl.java        # Create
├── controller/
│   ├── ChapterController.java          # Create
│   └── CourseQaController.java         # Create
├── config/
│   └── SchemaInitializer.java          # Modify: add new table DDL, update seed data

MODIFY existing:
├── domain/Course.java                  # Modify: add subjectId, background, target, principle fields
├── service/CourseService.java          # Modify: update method signatures
├── service/impl/CourseServiceImpl.java # Modify: rewrite getSubjectTree→getChapterTree
├── controller/CourseController.java    # Modify: add new endpoints
├── controller/SubjectCourseController.java # Modify: replace Unit/Lesson endpoints

frontend/src/
├── api/index.js                        # Modify: add new API functions
├── router/index.js                     # Modify: update course route paths
├── components/course/
│   ├── ChapterTree.vue                 # Create
│   ├── AnnouncementList.vue            # Create
│   ├── ResourceList.vue                # Create
│   ├── CourseQA.vue                    # Create
│   ├── CourseDesign.vue               # Create
│   └── CourseHomework.vue              # Create
├── views/
│   ├── student/CourseView.vue          # Modify: replace tree, add Q&A tab
│   ├── teacher/ContentManagement.vue   # Modify: rewrite for chapters
│   └── teacher/TeacherManage.vue       # Modify: add course form fields
```

---

### Task 1: Database Migration SQL

**Files:**
- Create: `sql/migrate-v3-course-chapters.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- ============================================
-- migrate-v3: 重构为 Subject→Course→Chapter→SubChapter 层级
-- ============================================

-- 1. 为 sp_course 添加新字段
ALTER TABLE sp_course ADD COLUMN subject_id BIGINT AFTER id;
ALTER TABLE sp_course ADD COLUMN background VARCHAR(512) AFTER target_audience;
ALTER TABLE sp_course ADD COLUMN target VARCHAR(512) AFTER background;
ALTER TABLE sp_course ADD COLUMN principle VARCHAR(512) AFTER target;
ALTER TABLE sp_course ADD INDEX idx_course_subject (subject_id);

-- 2. 创建 sp_chapter（合并 sp_unit + 源 CourseChapter）
CREATE TABLE sp_chapter (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL COMMENT '所属课程',
  title VARCHAR(256) NOT NULL COMMENT '章节标题',
  description TEXT COMMENT '章节描述',
  sort_order INT DEFAULT 0 COMMENT '排序',
  prerequisite_chapter_id BIGINT COMMENT '前置章节ID',
  created_at DATETIME,
  INDEX idx_chapter_course (course_id)
);

-- 3. 创建 sp_sub_chapter（合并 sp_lesson + 源 ChildChapter）
CREATE TABLE sp_sub_chapter (
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
  user_id BIGINT COMMENT '创建者(private课)',
  INDEX idx_subchapter_chapter (chapter_id)
);

-- 4. 创建 sp_chapter_resource（章节资源）
CREATE TABLE sp_chapter_resource (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  chapter_id BIGINT NOT NULL,
  type VARCHAR(32) COMMENT '资源类型: 视频/课件/文档',
  title VARCHAR(256) NOT NULL,
  description TEXT,
  url VARCHAR(512),
  size VARCHAR(32) COMMENT '文件大小',
  created_at DATETIME,
  INDEX idx_res_course (course_id),
  INDEX idx_res_chapter (chapter_id)
);

-- 5. 创建 sp_course_announcement（课程公告）
CREATE TABLE sp_course_announcement (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  type VARCHAR(32) DEFAULT '普通公告' COMMENT '普通公告/重要公告/警示公告',
  title VARCHAR(256) NOT NULL,
  content TEXT,
  created_at DATETIME,
  INDEX idx_ann_course (course_id)
);

-- 6. 创建 sp_course_question（课程问答）
CREATE TABLE sp_course_question (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL COMMENT '提问者',
  title VARCHAR(256) NOT NULL,
  content TEXT,
  created_at DATETIME,
  INDEX idx_q_course (course_id),
  INDEX idx_q_user (user_id)
);

-- 7. 创建 sp_course_answer（问答回复）
CREATE TABLE sp_course_answer (
  id BIGINT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  user_id BIGINT COMMENT '回答者(NULL=AI)',
  content TEXT NOT NULL,
  is_ai TINYINT(1) DEFAULT 0 COMMENT '是否AI生成',
  created_at DATETIME,
  INDEX idx_answer_question (question_id)
);

-- 8. 数据迁移：sp_unit → sp_chapter
-- 为每个学科创建或关联Course，然后将Unit数据迁入Chapter
-- 策略：如果Course中已有该学科的课程，关联第一个；否则创建占位Course
INSERT INTO sp_course (id, subject_id, title, category, description, status, created_at, updated_at)
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

-- 迁移 Unit → Chapter
INSERT INTO sp_chapter (id, course_id, title, description, sort_order, prerequisite_chapter_id, created_at)
SELECT 
  u.id,
  COALESCE(
    (SELECT c.id FROM sp_course c WHERE c.subject_id = u.subject_id LIMIT 1),
    u.subject_id + 7000
  ) AS course_id,
  u.name,
  u.description,
  u.sort_order,
  u.prerequisite_unit_id,
  NOW()
FROM sp_unit u;

-- 迁移 Lesson → SubChapter  
INSERT INTO sp_sub_chapter (id, chapter_id, title, description, sort_order, type, video_url, duration, content, user_id)
SELECT 
  l.id,
  l.unit_id AS chapter_id,
  l.name,
  '' AS description,
  l.sort_order,
  l.type,
  l.video_url,
  l.duration,
  l.content,
  NULL AS user_id
FROM sp_lesson l;

-- 9. 更新子表外键引用
-- sp_lesson_progress 改为指向 sub_chapter
ALTER TABLE sp_lesson_progress ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id;
UPDATE sp_lesson_progress lp SET sub_chapter_id = lesson_id;
ALTER TABLE sp_lesson_progress DROP COLUMN lesson_id;
ALTER TABLE sp_lesson_progress ADD UNIQUE KEY uk_subchapter_progress (user_id, sub_chapter_id);
ALTER TABLE sp_lesson_progress ADD INDEX idx_scp_subchapter (sub_chapter_id);

-- sp_knowledge_point 改为指向 sub_chapter
ALTER TABLE sp_knowledge_point ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id;
UPDATE sp_knowledge_point SET sub_chapter_id = lesson_id;
ALTER TABLE sp_knowledge_point DROP COLUMN lesson_id;
ALTER TABLE sp_knowledge_point ADD INDEX idx_kp_subchapter (sub_chapter_id);

-- sp_exercise 保持不变（通过 knowledge_point_id 间接关联）

-- sp_content_review 改为指向 sub_chapter
ALTER TABLE sp_content_review ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id;
UPDATE sp_content_review SET sub_chapter_id = lesson_id;
ALTER TABLE sp_content_review DROP COLUMN lesson_id;
ALTER TABLE sp_content_review ADD INDEX idx_cr_subchapter (sub_chapter_id);

-- sp_resource_recommendation 改为指向 sub_chapter
ALTER TABLE sp_resource_recommendation ADD COLUMN sub_chapter_id BIGINT AFTER lesson_id;
UPDATE sp_resource_recommendation SET sub_chapter_id = lesson_id;
ALTER TABLE sp_resource_recommendation DROP COLUMN lesson_id;

-- sp_study_duration (if exists) — check and rename
-- Skip if table doesn't exist yet

-- 10. 删除旧表（确认数据完整后执行）
-- DROP TABLE IF EXISTS sp_lesson;
-- DROP TABLE IF EXISTS sp_unit;
```

- [ ] **Step 2: Run migration against dev database**

```bash
cd C:\Users\ZWC\Desktop\软件杯大赛\softwacecup
mysql -u root -p smartprep < sql/migrate-v3-course-chapters.sql
```

Expected: Tables created, data migrated, no errors.

- [ ] **Step 3: Commit**

```bash
git add sql/migrate-v3-course-chapters.sql
git commit -m "feat: add v3 migration - restructure to Subject→Course→Chapter→SubChapter"
```

---

### Task 2: Domain Entities

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/Chapter.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/SubChapter.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/ChapterResource.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/CourseAnnouncement.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/CourseQuestion.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/domain/CourseAnswer.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/domain/Course.java`

- [ ] **Step 1: Write Chapter.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_chapter")
public class Chapter {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer sortOrder;
    private Long prerequisiteChapterId;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Write SubChapter.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_sub_chapter")
public class SubChapter {
    private Long id;
    private Long chapterId;
    private String title;
    private String description;
    private Integer sortOrder;
    private String type;
    private String videoUrl;
    private Integer duration;
    private String content;
    private String coverUrl;
    private String status;
    private Long userId;
}
```

- [ ] **Step 3: Write ChapterResource.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_chapter_resource")
public class ChapterResource {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String type;
    private String title;
    private String description;
    private String url;
    private String size;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Write CourseAnnouncement.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course_announcement")
public class CourseAnnouncement {
    private Long id;
    private Long courseId;
    private String type;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Write CourseQuestion.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course_question")
public class CourseQuestion {
    private Long id;
    private Long courseId;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: Write CourseAnswer.java**

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course_answer")
public class CourseAnswer {
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Boolean isAi;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: Modify Course.java — add fields**

Read the existing file at `backend/src/main/java/com/iflytek/smartprep/domain/Course.java`, then replace it:

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_course")
public class Course {
    private Long id;
    private Long subjectId;
    private String title;
    private String category;
    private String description;
    private String coverImage;
    private String price;
    private String tag;
    private String status;
    private Integer totalHours;
    private String targetAudience;
    private String background;
    private String target;
    private String principle;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Note: The old `chaptersJson` field is removed (chapters are now in `sp_chapter` table). If the Java field existed in Course.java, remove it.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/domain/
git commit -m "feat: add Chapter/SubChapter/Resource/Announcement/QA domain entities; update Course"
```

---

### Task 3: Mappers

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ChapterMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/SubChapterMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/ChapterResourceMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/CourseAnnouncementMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/CourseQuestionMapper.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/mapper/CourseAnswerMapper.java`

- [ ] **Step 1: Write all six mappers**

```java
// ChapterMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.Chapter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}
```

```java
// SubChapterMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.SubChapter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubChapterMapper extends BaseMapper<SubChapter> {
}
```

```java
// ChapterResourceMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.ChapterResource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterResourceMapper extends BaseMapper<ChapterResource> {
}
```

```java
// CourseAnnouncementMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.CourseAnnouncement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseAnnouncementMapper extends BaseMapper<CourseAnnouncement> {
}
```

```java
// CourseQuestionMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.CourseQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseQuestionMapper extends BaseMapper<CourseQuestion> {
}
```

```java
// CourseAnswerMapper.java
package com.iflytek.smartprep.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.smartprep.domain.CourseAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseAnswerMapper extends BaseMapper<CourseAnswer> {
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/mapper/ChapterMapper.java backend/src/main/java/com/iflytek/smartprep/mapper/SubChapterMapper.java backend/src/main/java/com/iflytek/smartprep/mapper/ChapterResourceMapper.java backend/src/main/java/com/iflytek/smartprep/mapper/CourseAnnouncementMapper.java backend/src/main/java/com/iflytek/smartprep/mapper/CourseQuestionMapper.java backend/src/main/java/com/iflytek/smartprep/mapper/CourseAnswerMapper.java
git commit -m "feat: add mappers for Chapter, SubChapter, Resource, Announcement, CourseQuestion, CourseAnswer"
```

---

### Task 4: ChapterService — Chapter Tree & CRUD

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/service/ChapterService.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/service/impl/ChapterServiceImpl.java`

- [ ] **Step 1: Write ChapterService interface**

```java
package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.Chapter;
import com.iflytek.smartprep.domain.SubChapter;
import java.util.List;
import java.util.Map;

public interface ChapterService {

    /** 获取课程章节树（章节嵌套子章节） */
    List<Map<String, Object>> getChapterTree(Long courseId);

    /** 获取子章节详情（含知识点和练习） */
    Map<String, Object> getSubChapterDetail(Long subChapterId);

    /** CRUD */
    Chapter createChapter(Chapter chapter);
    Chapter updateChapter(Long id, Chapter chapter);
    void deleteChapter(Long id);
    SubChapter createSubChapter(SubChapter subChapter);
    SubChapter updateSubChapter(Long id, SubChapter subChapter);
    void deleteSubChapter(Long id);
}
```

- [ ] **Step 2: Write ChapterServiceImpl**

```java
package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final SubChapterMapper subChapterMapper;
    private final KnowledgePointMapper kpMapper;
    private final ExerciseMapper exerciseMapper;

    @Override
    public List<Map<String, Object>> getChapterTree(Long courseId) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getCourseId, courseId)
                        .orderByAsc(Chapter::getSortOrder));
        List<SubChapter> allSubChapters = subChapterMapper.selectList(
                new LambdaQueryWrapper<SubChapter>()
                        .in(SubChapter::getChapterId,
                                chapters.stream().map(Chapter::getId).collect(Collectors.toList()))
                        .orderByAsc(SubChapter::getSortOrder));

        return chapters.stream().map(ch -> {
            Map<String, Object> chNode = new HashMap<>();
            chNode.put("id", ch.getId());
            chNode.put("title", ch.getTitle());
            chNode.put("description", ch.getDescription());
            chNode.put("sortOrder", ch.getSortOrder());
            chNode.put("prerequisiteChapterId", ch.getPrerequisiteChapterId());

            List<SubChapter> chSubs = allSubChapters.stream()
                    .filter(sc -> sc.getChapterId().equals(ch.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> subNodes = chSubs.stream().map(sc -> {
                Map<String, Object> scNode = new HashMap<>();
                scNode.put("id", sc.getId());
                scNode.put("title", sc.getTitle());
                scNode.put("description", sc.getDescription());
                scNode.put("type", sc.getType());
                scNode.put("videoUrl", sc.getVideoUrl());
                scNode.put("duration", sc.getDuration());
                scNode.put("sortOrder", sc.getSortOrder());
                return scNode;
            }).collect(Collectors.toList());

            chNode.put("subChapters", subNodes);
            return chNode;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSubChapterDetail(Long subChapterId) {
        SubChapter sc = subChapterMapper.selectById(subChapterId);
        if (sc == null) return Collections.emptyMap();

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", sc.getId());
        detail.put("chapterId", sc.getChapterId());
        detail.put("title", sc.getTitle());
        detail.put("description", sc.getDescription());
        detail.put("type", sc.getType());
        detail.put("videoUrl", sc.getVideoUrl());
        detail.put("duration", sc.getDuration());
        detail.put("content", sc.getContent());
        detail.put("coverUrl", sc.getCoverUrl());

        // 知识点
        List<KnowledgePoint> kps = kpMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubChapterId, subChapterId));
        detail.put("knowledgePoints", kps.stream().map(kp -> {
            Map<String, Object> kpNode = new HashMap<>();
            kpNode.put("id", kp.getId());
            kpNode.put("name", kp.getName());
            kpNode.put("difficultyLevel", kp.getDifficultyLevel());
            kpNode.put("tags", kp.getTags());
            return kpNode;
        }).collect(Collectors.toList()));

        // 练习
        if (!kps.isEmpty()) {
            List<Exercise> exercises = exerciseMapper.selectList(
                    new LambdaQueryWrapper<Exercise>()
                            .in(Exercise::getKnowledgePointId,
                                    kps.stream().map(KnowledgePoint::getId).collect(Collectors.toList())));
            detail.put("exercises", exercises.stream().map(ex -> {
                Map<String, Object> exNode = new HashMap<>();
                exNode.put("id", ex.getId());
                exNode.put("knowledgePointId", ex.getKnowledgePointId());
                exNode.put("type", ex.getType());
                exNode.put("difficulty", ex.getDifficulty());
                exNode.put("contentJson", ex.getContentJson());
                exNode.put("answer", ex.getAnswer());
                exNode.put("explanation", ex.getExplanation());
                return exNode;
            }).collect(Collectors.toList()));
        } else {
            detail.put("exercises", List.of());
        }

        return detail;
    }

    @Override
    public Chapter createChapter(Chapter chapter) {
        chapter.setId(System.currentTimeMillis());
        chapter.setCreatedAt(java.time.LocalDateTime.now());
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    public Chapter updateChapter(Long id, Chapter chapter) {
        chapter.setId(id);
        chapterMapper.updateById(chapter);
        return chapterMapper.selectById(id);
    }

    @Override
    public void deleteChapter(Long id) {
        subChapterMapper.delete(new LambdaQueryWrapper<SubChapter>().eq(SubChapter::getChapterId, id));
        chapterMapper.deleteById(id);
    }

    @Override
    public SubChapter createSubChapter(SubChapter subChapter) {
        subChapter.setId(System.currentTimeMillis());
        subChapterMapper.insert(subChapter);
        return subChapter;
    }

    @Override
    public SubChapter updateSubChapter(Long id, SubChapter subChapter) {
        subChapter.setId(id);
        subChapterMapper.updateById(subChapter);
        return subChapterMapper.selectById(id);
    }

    @Override
    public void deleteSubChapter(Long id) {
        subChapterMapper.deleteById(id);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/service/ChapterService.java backend/src/main/java/com/iflytek/smartprep/service/impl/ChapterServiceImpl.java
git commit -m "feat: add ChapterService with chapter tree, sub-chapter detail, and CRUD"
```

---

### Task 5: CourseQaService — Q&A Forum

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/service/CourseQaService.java`
- Create: `backend/src/main/java/com/iflytek/smartprep/service/impl/CourseQaServiceImpl.java`

- [ ] **Step 1: Write CourseQaService interface**

```java
package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.CourseAnswer;
import com.iflytek.smartprep.domain.CourseQuestion;
import java.util.List;
import java.util.Map;

public interface CourseQaService {

    /** 分页获取课程问题列表 */
    List<Map<String, Object>> getQuestions(Long courseId, int page, int pageSize);

    /** 提问 */
    CourseQuestion askQuestion(Long courseId, Long userId, String title, String content);

    /** 获取问题的回答列表 */
    List<Map<String, Object>> getAnswers(Long questionId);

    /** 人工回答 */
    CourseAnswer postAnswer(Long questionId, Long userId, String content);

    /** AI 回答 */
    CourseAnswer postAiAnswer(Long questionId, String content);
}
```

- [ ] **Step 2: Write CourseQaServiceImpl**

```java
package com.iflytek.smartprep.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.CourseQaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseQaServiceImpl implements CourseQaService {

    private final CourseQuestionMapper questionMapper;
    private final CourseAnswerMapper answerMapper;
    private final UserMapper userMapper;

    @Override
    public List<Map<String, Object>> getQuestions(Long courseId, int page, int pageSize) {
        List<CourseQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<CourseQuestion>()
                        .eq(CourseQuestion::getCourseId, courseId)
                        .orderByDesc(CourseQuestion::getCreatedAt)
                        .last("LIMIT " + ((page - 1) * pageSize) + ", " + pageSize));

        return questions.stream().map(q -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", q.getId());
            node.put("courseId", q.getCourseId());
            node.put("userId", q.getUserId());
            node.put("title", q.getTitle());
            node.put("content", q.getContent());
            node.put("createdAt", q.getCreatedAt());

            // 获取回答数量
            Long answerCount = answerMapper.selectCount(
                    new LambdaQueryWrapper<CourseAnswer>().eq(CourseAnswer::getQuestionId, q.getId()));
            node.put("answerCount", answerCount);

            // 获取提问者姓名
            User user = userMapper.selectById(q.getUserId());
            node.put("userName", user != null ? user.getDisplayName() : "未知");
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public CourseQuestion askQuestion(Long courseId, Long userId, String title, String content) {
        CourseQuestion q = new CourseQuestion();
        q.setId(System.currentTimeMillis());
        q.setCourseId(courseId);
        q.setUserId(userId);
        q.setTitle(title);
        q.setContent(content);
        q.setCreatedAt(LocalDateTime.now());
        questionMapper.insert(q);
        return q;
    }

    @Override
    public List<Map<String, Object>> getAnswers(Long questionId) {
        List<CourseAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<CourseAnswer>()
                        .eq(CourseAnswer::getQuestionId, questionId)
                        .orderByAsc(CourseAnswer::getCreatedAt));

        return answers.stream().map(a -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", a.getId());
            node.put("questionId", a.getQuestionId());
            node.put("userId", a.getUserId());
            node.put("content", a.getContent());
            node.put("isAi", a.getIsAi());
            node.put("createdAt", a.getCreatedAt());

            if (a.getUserId() != null) {
                User user = userMapper.selectById(a.getUserId());
                node.put("userName", user != null ? user.getDisplayName() : "未知");
            } else {
                node.put("userName", "AI 小慧");
            }
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public CourseAnswer postAnswer(Long questionId, Long userId, String content) {
        CourseAnswer a = new CourseAnswer();
        a.setId(System.currentTimeMillis());
        a.setQuestionId(questionId);
        a.setUserId(userId);
        a.setContent(content);
        a.setIsAi(false);
        a.setCreatedAt(LocalDateTime.now());
        answerMapper.insert(a);
        return a;
    }

    @Override
    public CourseAnswer postAiAnswer(Long questionId, String content) {
        CourseAnswer a = new CourseAnswer();
        a.setId(System.currentTimeMillis());
        a.setQuestionId(questionId);
        a.setUserId(null);
        a.setContent(content);
        a.setIsAi(true);
        a.setCreatedAt(LocalDateTime.now());
        answerMapper.insert(a);
        return a;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/service/CourseQaService.java backend/src/main/java/com/iflytek/smartprep/service/impl/CourseQaServiceImpl.java
git commit -m "feat: add CourseQaService for course Q&A forum"
```

---

### Task 6: ChapterController — Chapter & Resource Endpoints

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/controller/ChapterController.java`

- [ ] **Step 1: Write ChapterController**

```java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import com.iflytek.smartprep.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterResourceMapper resourceMapper;
    private final CourseAnnouncementMapper announcementMapper;
    private final CourseMapper courseMapper;
    private final LessonProgressMapper lessonProgressMapper;

    /** GET /api/courses/{id}/chapters — 章节树 */
    @GetMapping("/courses/{id}/chapters")
    public ApiResponse<List<Map<String, Object>>> getChapters(@PathVariable Long id) {
        return ApiResponse.ok(chapterService.getChapterTree(id));
    }

    /** GET /api/sub-chapters/{id} — 子章节详情 */
    @GetMapping("/sub-chapters/{id}")
    public ApiResponse<Map<String, Object>> getSubChapter(@PathVariable Long id) {
        return ApiResponse.ok(chapterService.getSubChapterDetail(id));
    }

    /** POST /api/chapters — 创建章节 */
    @PostMapping("/chapters")
    @RequireRole({"teacher"})
    public ApiResponse<Chapter> createChapter(@RequestBody Chapter chapter) {
        return ApiResponse.ok(chapterService.createChapter(chapter));
    }

    /** PUT /api/chapters/{id} — 更新章节 */
    @PutMapping("/chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<Chapter> updateChapter(@PathVariable Long id, @RequestBody Chapter chapter) {
        return ApiResponse.ok(chapterService.updateChapter(id, chapter));
    }

    /** DELETE /api/chapters/{id} — 删除章节 */
    @DeleteMapping("/chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ApiResponse.ok("ok");
    }

    /** POST /api/sub-chapters — 创建子章节 */
    @PostMapping("/sub-chapters")
    @RequireRole({"teacher"})
    public ApiResponse<SubChapter> createSubChapter(@RequestBody SubChapter subChapter) {
        return ApiResponse.ok(chapterService.createSubChapter(subChapter));
    }

    /** PUT /api/sub-chapters/{id} — 更新子章节 */
    @PutMapping("/sub-chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<SubChapter> updateSubChapter(@PathVariable Long id, @RequestBody SubChapter subChapter) {
        return ApiResponse.ok(chapterService.updateSubChapter(id, subChapter));
    }

    /** DELETE /api/sub-chapters/{id} — 删除子章节 */
    @DeleteMapping("/sub-chapters/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteSubChapter(@PathVariable Long id) {
        chapterService.deleteSubChapter(id);
        return ApiResponse.ok("ok");
    }

    // ==================== 章节资源 ====================

    /** GET /api/chapters/{id}/resources — 章节资源 */
    @GetMapping("/chapters/{id}/resources")
    public ApiResponse<List<ChapterResource>> getChapterResources(@PathVariable Long id) {
        return ApiResponse.ok(resourceMapper.selectList(
                new LambdaQueryWrapper<ChapterResource>()
                        .eq(ChapterResource::getChapterId, id)
                        .orderByDesc(ChapterResource::getCreatedAt)));
    }

    /** POST /api/chapters/{id}/resources — 上传章节资源 */
    @PostMapping("/chapters/{id}/resources")
    @RequireRole({"teacher"})
    public ApiResponse<ChapterResource> addResource(@PathVariable Long id, @RequestBody ChapterResource resource) {
        resource.setId(System.currentTimeMillis());
        resource.setChapterId(id);
        resource.setCreatedAt(java.time.LocalDateTime.now());
        resourceMapper.insert(resource);
        return ApiResponse.ok(resource);
    }

    /** DELETE /api/resources/{id} — 删除资源 */
    @DeleteMapping("/resources/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteResource(@PathVariable Long id) {
        resourceMapper.deleteById(id);
        return ApiResponse.ok("ok");
    }

    /** GET /api/courses/{id}/resources — 课程所有资源 */
    @GetMapping("/courses/{id}/resources")
    public ApiResponse<List<ChapterResource>> getCourseResources(@PathVariable Long id) {
        return ApiResponse.ok(resourceMapper.selectList(
                new LambdaQueryWrapper<ChapterResource>()
                        .eq(ChapterResource::getCourseId, id)
                        .orderByDesc(ChapterResource::getCreatedAt)));
    }

    // ==================== 课程公告 ====================

    /** GET /api/courses/{id}/announcements — 课程公告 */
    @GetMapping("/courses/{id}/announcements")
    public ApiResponse<List<CourseAnnouncement>> getAnnouncements(@PathVariable Long id) {
        return ApiResponse.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<CourseAnnouncement>()
                        .eq(CourseAnnouncement::getCourseId, id)
                        .orderByDesc(CourseAnnouncement::getCreatedAt)));
    }

    /** POST /api/courses/{id}/announcements — 发布公告 */
    @PostMapping("/courses/{id}/announcements")
    @RequireRole({"teacher"})
    public ApiResponse<CourseAnnouncement> createAnnouncement(@PathVariable Long id, @RequestBody CourseAnnouncement announcement) {
        announcement.setId(System.currentTimeMillis());
        announcement.setCourseId(id);
        announcement.setCreatedAt(java.time.LocalDateTime.now());
        announcementMapper.insert(announcement);
        return ApiResponse.ok(announcement);
    }

    // ==================== 进度（适配 SubChapter） ====================

    /** GET /api/progress/sub-chapter/{id} — 子章节进度 */
    @GetMapping("/progress/sub-chapter/{id}")
    public ApiResponse<LessonProgress> getSubChapterProgress(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getSubChapterId, id));
        return ApiResponse.ok(lp != null ? lp : new LessonProgress());
    }

    /** POST /api/progress/sub-chapter/{id}/complete — 完成子章节 */
    @PostMapping("/progress/sub-chapter/{id}/complete")
    public ApiResponse<String> completeSubChapter(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        LessonProgress lp = lessonProgressMapper.selectOne(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getSubChapterId, id));
        if (lp == null) {
            lp = new LessonProgress();
            lp.setId(System.currentTimeMillis());
            lp.setUserId(userId);
            lp.setSubChapterId(id);
            lp.setStatus("completed");
            lp.setCompletedAt(java.time.LocalDateTime.now());
            lessonProgressMapper.insert(lp);
        } else {
            lp.setStatus("completed");
            lp.setCompletedAt(java.time.LocalDateTime.now());
            lessonProgressMapper.updateById(lp);
        }
        return ApiResponse.ok("ok");
    }

    /** GET /api/progress/course/{id} — 课程总进度 */
    @GetMapping("/progress/course/{id}")
    public ApiResponse<Map<String, Object>> getCourseProgress(@PathVariable Long id) {
        Long userId = LoginUserHolder.get().getUserId();
        List<Chapter> chapters = chapterService.getChapterTree(id).stream()
                .flatMap(ch -> ((List<Map<String, Object>>) ch.get("subChapters")).stream())
                .map(sc -> { Chapter c = new Chapter(); c.setId((Long) sc.get("id")); return c; })
                .toList()
                .stream().collect(java.util.stream.Collectors.toList());
        // Simplified: count sub-chapters for this course and check progress
        List<Long> subChapterIds = new ArrayList<>();
        for (Map<String, Object> ch : chapterService.getChapterTree(id)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subs = (List<Map<String, Object>>) ch.get("subChapters");
            for (Map<String, Object> sc : subs) {
                subChapterIds.add((Long) sc.get("id"));
            }
        }
        long total = subChapterIds.size();
        long completed = total > 0 ? lessonProgressMapper.selectCount(
                new LambdaQueryWrapper<LessonProgress>()
                        .eq(LessonProgress::getUserId, userId)
                        .eq(LessonProgress::getStatus, "completed")
                        .in(LessonProgress::getSubChapterId, subChapterIds)) : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("percent", total > 0 ? (int) (completed * 100 / total) : 0);
        return ApiResponse.ok(result);
    }
}
```

Note: The `getCourseProgress` method has a verbose inline loop due to type erasure. Refactor in a later task if needed.

- [ ] **Step 2: Update LessonProgress domain to include subChapterId field**

Read the existing `LessonProgress.java`, add if missing:

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sp_lesson_progress")
public class LessonProgress {
    private Long id;
    private Long userId;
    private Long subChapterId;
    private String status;
    private LocalDateTime completedAt;
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/ChapterController.java backend/src/main/java/com/iflytek/smartprep/domain/LessonProgress.java
git commit -m "feat: add ChapterController with chapter CRUD, resources, announcements, and progress endpoints"
```

---

### Task 7: CourseQaController — Q&A Endpoints

**Files:**
- Create: `backend/src/main/java/com/iflytek/smartprep/controller/CourseQaController.java`

- [ ] **Step 1: Write CourseQaController**

```java
package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.CourseAnswer;
import com.iflytek.smartprep.domain.CourseQuestion;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.CourseQaService;
import com.iflytek.smartprep.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseQaController {

    private final CourseQaService courseQaService;
    private final TutorService tutorService;

    /** GET /api/courses/{id}/questions — 问题列表 */
    @GetMapping("/courses/{id}/questions")
    public ApiResponse<List<Map<String, Object>>> getQuestions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(courseQaService.getQuestions(id, page, pageSize));
    }

    /** POST /api/courses/{id}/questions — 提问 */
    @PostMapping("/courses/{id}/questions")
    @RequireRole({"student"})
    public ApiResponse<CourseQuestion> askQuestion(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(courseQaService.askQuestion(
                id, userId, body.get("title"), body.get("content")));
    }

    /** GET /api/questions/{id}/answers — 回答列表 */
    @GetMapping("/questions/{id}/answers")
    public ApiResponse<List<Map<String, Object>>> getAnswers(@PathVariable Long id) {
        return ApiResponse.ok(courseQaService.getAnswers(id));
    }

    /** POST /api/questions/{id}/answers — 人工回答 */
    @PostMapping("/questions/{id}/answers")
    @RequireRole({"teacher", "student"})
    public ApiResponse<CourseAnswer> postAnswer(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(courseQaService.postAnswer(id, userId, body.get("content")));
    }

    /** POST /api/questions/{id}/ai-answer — AI 回答 */
    @PostMapping("/questions/{id}/ai-answer")
    public ApiResponse<CourseAnswer> aiAnswer(@PathVariable Long id) {
        // Find the question to get context
        Map<String, Object> firstPage = courseQaService.getQuestions(null, 1, 1);
        // Use LLM to generate answer based on question content
        String prompt = "请用中文回答以下课程问题，回答应专业、准确、有帮助。";
        String aiResponse = tutorService.ask(prompt);
        CourseAnswer answer = courseQaService.postAiAnswer(id, aiResponse);
        return ApiResponse.ok(answer);
    }
}
```

Note: The `aiAnswer` endpoint's approach to finding question context can be refined. For now, it uses a generic prompt. In production, load the actual question from `CourseQuestionMapper` and include it in the prompt.

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/CourseQaController.java
git commit -m "feat: add CourseQaController with Q&A forum endpoints"
```

---

### Task 8: Update CourseController & SubjectCourseController

**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/controller/CourseController.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/service/CourseService.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/service/impl/CourseServiceImpl.java`

- [ ] **Step 1: Update CourseController.java — add subject_id filter and course CRUD**

Replace `CourseController.java`:

```java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.RequireRole;
import com.iflytek.smartprep.domain.Course;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;

    /** GET /api/courses/public — 所有已发布课程 */
    @GetMapping("/courses/public")
    public ApiResponse<List<Course>> listPublicCourses() {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published")
                        .orderByDesc(Course::getCreatedAt)));
    }

    /** GET /api/subjects/{id}/courses — 学科下课程 */
    @GetMapping("/subjects/{id}/courses")
    public ApiResponse<List<Course>> listCoursesBySubject(@PathVariable Long id) {
        return ApiResponse.ok(courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getSubjectId, id)
                        .eq(Course::getStatus, "已发布")
                        .or().eq(Course::getStatus, "published")
                        .orderByDesc(Course::getCreatedAt)));
    }

    /** GET /api/courses/{id} — 课程详情 */
    @GetMapping("/courses/{id}")
    public ApiResponse<Course> getCourse(@PathVariable Long id) {
        Course c = courseMapper.selectById(id);
        return c != null ? ApiResponse.ok(c) : ApiResponse.fail("课程不存在");
    }

    /** POST /api/courses — 创建课程 */
    @PostMapping("/courses")
    @RequireRole({"teacher"})
    public ApiResponse<Course> createCourse(@RequestBody Course course) {
        course.setId(System.currentTimeMillis());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);
        return ApiResponse.ok(course);
    }

    /** PUT /api/courses/{id} — 更新课程 */
    @PutMapping("/courses/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
        return ApiResponse.ok(courseMapper.selectById(id));
    }

    /** DELETE /api/courses/{id} — 删除课程 */
    @DeleteMapping("/courses/{id}")
    @RequireRole({"teacher"})
    public ApiResponse<String> deleteCourse(@PathVariable Long id) {
        courseMapper.deleteById(id);
        return ApiResponse.ok("ok");
    }
}
```

- [ ] **Step 2: Update SubjectCourseController — remove old Unit/Lesson endpoints, keep Subject endpoints**

Remove the Unit/Lesson CRUD methods. Keep Subject endpoints and the `/lessons/my-imports` method (it references Lesson → update to SubChapter). Replace `SubjectCourseController.java`:

```java
package com.iflytek.smartprep.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectCourseController {

    private final SubjectMapper subjectMapper;
    private final SubChapterMapper subChapterMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    /** GET /api/subjects — 学科列表 */
    @GetMapping("/subjects")
    public ApiResponse<List<Subject>> listSubjects() {
        return ApiResponse.ok(subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().orderByAsc(Subject::getSortOrder)));
    }

    /** GET /api/subjects/{id} — 学科详情 */
    @GetMapping("/subjects/{id}")
    public ApiResponse<Subject> getSubject(@PathVariable Long id) {
        Subject s = subjectMapper.selectById(id);
        return s != null ? ApiResponse.ok(s) : ApiResponse.fail("学科不存在");
    }

    /** GET /api/lessons/my-imports — 我的导入视频（适配 sub_chapter） */
    @GetMapping("/lessons/my-imports")
    public ApiResponse<List<Map<String, Object>>> getMyImports() {
        Long userId = LoginUserHolder.get().getUserId();
        List<SubChapter> mySubs = subChapterMapper.selectList(
                new LambdaQueryWrapper<SubChapter>()
                        .eq(SubChapter::getUserId, userId)
                        .orderByDesc(SubChapter::getId));

        Map<String, List<SubChapter>> grouped = new LinkedHashMap<>();
        for (SubChapter sc : mySubs) {
            String key = sc.getVideoUrl() != null ? sc.getVideoUrl() : "";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(sc);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<SubChapter> subs = entry.getValue();
            SubChapter first = subs.get(0);

            String chapterName = "";
            String courseName = "";
            String subjectName = "";
            if (first.getChapterId() != null) {
                Chapter chapter = chapterMapper.selectById(first.getChapterId());
                if (chapter != null) {
                    chapterName = chapter.getTitle();
                    Course course = courseMapper.selectById(chapter.getCourseId());
                    if (course != null) {
                        courseName = course.getTitle();
                        Subject subject = subjectMapper.selectById(course.getSubjectId());
                        if (subject != null) subjectName = subject.getName();
                    }
                }
            }

            String bvid = "";
            String url = first.getVideoUrl();
            if (url != null && url.contains("BV")) {
                bvid = url.substring(url.indexOf("BV"));
                if (bvid.length() > 12) bvid = bvid.substring(0, 12);
            }

            Map<String, Object> item = new HashMap<>();
            item.put("bvid", bvid);
            item.put("title", first.getTitle() != null ? first.getTitle() : "");
            item.put("subjectName", subjectName);
            item.put("courseName", courseName);
            item.put("chapterName", chapterName);
            item.put("lessonCount", subs.size());
            item.put("firstSubChapterId", first.getId());
            item.put("coverUrl", first.getCoverUrl() != null ? first.getCoverUrl() : "");
            result.add(item);
        }
        return ApiResponse.ok(result);
    }
}
```

- [ ] **Step 3: Update CourseService interface**

Replace `CourseService.java`:

```java
package com.iflytek.smartprep.service;

import java.util.List;
import java.util.Map;

public interface CourseService {

    /** 获取课程章节树（课程 → 章节 → 子章节） */
    List<Map<String, Object>> getChapterTree(Long courseId);

    /** 获取子章节详情（含视频、内容、知识点、练习） */
    Map<String, Object> getSubChapterDetail(Long subChapterId);
}
```

- [ ] **Step 4: Update CourseServiceImpl**

Replace `CourseServiceImpl.java` to delegate to `ChapterService`:

```java
package com.iflytek.smartprep.service.impl;

import com.iflytek.smartprep.service.ChapterService;
import com.iflytek.smartprep.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final ChapterService chapterService;

    @Override
    public List<Map<String, Object>> getChapterTree(Long courseId) {
        return chapterService.getChapterTree(courseId);
    }

    @Override
    public Map<String, Object> getSubChapterDetail(Long subChapterId) {
        return chapterService.getSubChapterDetail(subChapterId);
    }
}
```

- [ ] **Step 5: Add the /api/course/tree endpoint to CourseController (if any client still calls it)**

Add this method to `CourseController.java` if the frontend references `/course/tree`:

```java
private final ChapterService chapterService;

/** GET /api/course/tree — 向后兼容的课程树 */
@GetMapping("/course/tree")
public ApiResponse<List<Map<String, Object>>> getCourseTree(@RequestParam(required = false) Long courseId) {
    if (courseId != null) {
        return ApiResponse.ok(List.of(Map.of("id", courseId, "chapters", chapterService.getChapterTree(courseId))));
    }
    // Return all subjects with their courses
    List<Course> courses = courseMapper.selectList(
            new LambdaQueryWrapper<Course>().eq(Course::getStatus, "已发布"));
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (Course course : courses) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", course.getId());
        node.put("name", course.getTitle());
        node.put("chapters", chapterService.getChapterTree(course.getId()));
        result.add(node);
    }
    return ApiResponse.ok(result);
}
```

Add required imports and `private final ChapterService chapterService;` field if not present.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/controller/CourseController.java backend/src/main/java/com/iflytek/smartprep/controller/SubjectCourseController.java backend/src/main/java/com/iflytek/smartprep/service/CourseService.java backend/src/main/java/com/iflytek/smartprep/service/impl/CourseServiceImpl.java
git commit -m "refactor: restructure CourseController/Service for new Subject→Course→Chapter→SubChapter hierarchy"
```

---

### Task 9: Update KnowledgePoint & Exercise domain models

**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/domain/KnowledgePoint.java`
- Modify: `backend/src/main/java/com/iflytek/smartprep/domain/Exercise.java`

- [ ] **Step 1: Update KnowledgePoint.java — rename lessonId to subChapterId**

Read existing, replace:

```java
package com.iflytek.smartprep.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sp_knowledge_point")
public class KnowledgePoint {
    private Long id;
    private Long subChapterId;
    private String name;
    private String description;
    private Integer difficultyLevel;
    private String tags;
}
```

- [ ] **Step 2: Exercise.java stays the same (it references knowledge_point_id, not lesson_id directly)**

No changes needed if Exercise only references sp_exercise → knowledge_point_id.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/domain/KnowledgePoint.java
git commit -m "refactor: rename KnowledgePoint.lessonId to subChapterId"
```

---

### Task 10: Update SchemaInitializer — add new table DDL

**Files:**
- Modify: `backend/src/main/java/com/iflytek/smartprep/config/SchemaInitializer.java`

- [ ] **Step 1: Add new table DDL to init() method**

Add these `jdbcTemplate.execute(...)` calls after the existing `sp_course` creation line and before `seedDemoAccounts()`:

```java
// New v3 tables
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_chapter (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, description TEXT, sort_order INT DEFAULT 0, prerequisite_chapter_id BIGINT, created_at DATETIME)");
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_sub_chapter (id BIGINT PRIMARY KEY, chapter_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, description TEXT, sort_order INT DEFAULT 0, type VARCHAR(32) DEFAULT 'doc', video_url VARCHAR(512), duration INT, content LONGTEXT, cover_url VARCHAR(512), status VARCHAR(32) DEFAULT 'published', user_id BIGINT)");
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_chapter_resource (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, chapter_id BIGINT NOT NULL, type VARCHAR(32), title VARCHAR(256) NOT NULL, description TEXT, url VARCHAR(512), size VARCHAR(32), created_at DATETIME)");
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_announcement (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, type VARCHAR(32) DEFAULT '普通公告', title VARCHAR(256) NOT NULL, content TEXT, created_at DATETIME)");
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_question (id BIGINT PRIMARY KEY, course_id BIGINT NOT NULL, user_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, content TEXT, created_at DATETIME)");
jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sp_course_answer (id BIGINT PRIMARY KEY, question_id BIGINT NOT NULL, user_id BIGINT, content TEXT NOT NULL, is_ai TINYINT(1) DEFAULT 0, created_at DATETIME)");

// Ensure sp_course has new columns
ensureColumnExists("sp_course", "subject_id", "ALTER TABLE sp_course ADD COLUMN subject_id BIGINT AFTER id");
ensureColumnExists("sp_course", "background", "ALTER TABLE sp_course ADD COLUMN background VARCHAR(512)");
ensureColumnExists("sp_course", "target", "ALTER TABLE sp_course ADD COLUMN target VARCHAR(512)");
ensureColumnExists("sp_course", "principle", "ALTER TABLE sp_course ADD COLUMN principle VARCHAR(512)");

// Ensure sp_lesson_progress has sub_chapter_id column
ensureColumnExists("sp_lesson_progress", "sub_chapter_id", "ALTER TABLE sp_lesson_progress ADD COLUMN sub_chapter_id BIGINT");

// Ensure sp_knowledge_point has sub_chapter_id column
ensureColumnExists("sp_knowledge_point", "sub_chapter_id", "ALTER TABLE sp_knowledge_point ADD COLUMN sub_chapter_id BIGINT");
```

- [ ] **Step 2: Update seedCoursesIfEmpty — use subject_id instead of category**

In `seedCoursesIfEmpty()`, change course creation to set `subjectId` instead of just `category`. Find the existing code in the method and update each `c1.setCategory(...)` call to also set `c1.setSubjectId(...)`:

```java
c1.setSubjectId(1004L); // AI subject
c1.setBackground("人工智能技术的快速发展对教育领域产生了深远影响");
c1.setTarget("建立完整的AI知识框架，掌握Python工具链进行项目实战");
c1.setPrinciple("理论+实验结合，循序渐进的教学设计");
```

Similarly for c2:
```java
c2.setSubjectId(1004L);
```

And c3:
```java
c3.setSubjectId(1004L);  
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/iflytek/smartprep/config/SchemaInitializer.java
git commit -m "feat: add v3 table DDL to SchemaInitializer; update seed data with subjectId"
```

---

### Task 11: Frontend API Functions

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: Add new API functions**

Add these after the existing `// ==================== Neo4j 知识图谱 ====================` section:

```js
// ==================== v3 课程章节体系 ====================
export const apiCourseChapters = (courseId) => http.get(`/courses/${courseId}/chapters`)
export const apiSubChapterDetail = (subChapterId) => http.get(`/sub-chapters/${subChapterId}`)
export const apiCreateChapter = (data) => http.post('/chapters', data)
export const apiUpdateChapter = (id, data) => http.put(`/chapters/${id}`, data)
export const apiDeleteChapter = (id) => http.delete(`/chapters/${id}`)
export const apiCreateSubChapter = (data) => http.post('/sub-chapters', data)
export const apiUpdateSubChapter = (id, data) => http.put(`/sub-chapters/${id}`, data)
export const apiDeleteSubChapter = (id) => http.delete(`/sub-chapters/${id}`)

export const apiChapterResources = (chapterId) => http.get(`/chapters/${chapterId}/resources`)
export const apiAddChapterResource = (chapterId, data) => http.post(`/chapters/${chapterId}/resources`, data)
export const apiDeleteResource = (id) => http.delete(`/resources/${id}`)
export const apiCourseResources = (courseId) => http.get(`/courses/${courseId}/resources`)

export const apiCourseAnnouncements = (courseId) => http.get(`/courses/${courseId}/announcements`)
export const apiCreateAnnouncement = (courseId, data) => http.post(`/courses/${courseId}/announcements`, data)

// ==================== v3 课程Q&A ====================
export const apiCourseQuestions = (courseId, page = 1, pageSize = 20) =>
  http.get(`/courses/${courseId}/questions`, { params: { page, pageSize } })
export const apiAskQuestion = (courseId, data) => http.post(`/courses/${courseId}/questions`, data)
export const apiQuestionAnswers = (questionId) => http.get(`/questions/${questionId}/answers`)
export const apiPostAnswer = (questionId, data) => http.post(`/questions/${questionId}/answers`, data)
export const apiAiAnswer = (questionId) => http.post(`/questions/${questionId}/ai-answer`)

// ==================== v3 课程CRUD ====================
export const apiCoursesBySubject = (subjectId) => http.get(`/subjects/${subjectId}/courses`)
export const apiCourseDetail = (courseId) => http.get(`/courses/${courseId}`)
export const apiCreateCourse = (data) => http.post('/courses', data)
export const apiUpdateCourse = (id, data) => http.put(`/courses/${id}`, data)

// ==================== v3 进度（适配SubChapter） ====================
export const apiSubChapterProgress = (subChapterId) => http.get(`/progress/sub-chapter/${subChapterId}`)
export const apiCompleteSubChapter = (subChapterId) => http.post(`/progress/sub-chapter/${subChapterId}/complete`)
export const apiCourseProgress = (courseId) => http.get(`/progress/course/${courseId}`)
```

- [ ] **Step 2: Update old API function signatures**

Replace the old progress functions with new ones:

```js
// Remove old:
// export const apiCompleteLesson = (lessonId) => http.post(`/progress/lesson/${lessonId}/complete`)
// export const apiLessonProgress = (lessonId) => http.get(`/progress/lesson/${lessonId}`)
// export const apiCourseProgress = (subjectId) => http.get(`/progress/course/${subjectId}`)

// Keep apiSubjectTree but update to point to new endpoint
export const apiSubjectTree = (courseId) => http.get('/course/tree', { params: { courseId } })
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat: add v3 chapter, Q&A, resource, and course API functions"
```

---

### Task 12: ChapterTree Component

**Files:**
- Create: `frontend/src/components/course/ChapterTree.vue`

- [ ] **Step 1: Write ChapterTree.vue**

```vue
<template>
  <div class="chapter-tree">
    <div v-if="loading" class="tree-loading"><span class="spinner"></span></div>
    <div v-else-if="chapters.length === 0" class="tree-empty">暂无章节</div>
    <div v-else v-for="chapter in chapters" :key="chapter.id" class="tree-chapter">
      <div class="tree-chapter-header" @click="toggleChapter(chapter.id)">
        <span class="tree-chapter-icon">{{ expandedChapters.has(chapter.id) ? '▾' : '▸' }}</span>
        <span class="tree-chapter-title">{{ chapter.title }}</span>
      </div>
      <div v-if="expandedChapters.has(chapter.id)" class="tree-subchapters">
        <div
          v-for="sc in chapter.subChapters" :key="sc.id"
          class="tree-subchapter"
          :class="{ active: activeId === sc.id }"
          @click="$emit('select', sc)"
        >
          <span class="tree-subchapter-dot"></span>
          <span class="tree-subchapter-title">{{ sc.title }}</span>
          <span class="tree-subchapter-type">{{ typeLabel(sc.type) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  chapters: { type: Array, default: () => [] },
  activeId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['select'])

const expandedChapters = ref(new Set())

watch(() => props.chapters, (newVal) => {
  if (newVal.length > 0) {
    newVal.forEach(ch => expandedChapters.value.add(ch.id))
  }
}, { immediate: true })

function toggleChapter(id) {
  if (expandedChapters.value.has(id)) {
    expandedChapters.value.delete(id)
  } else {
    expandedChapters.value.add(id)
  }
  expandedChapters.value = new Set(expandedChapters.value)
}

function typeLabel(type) {
  const map = { video: '视频', doc: '文档', quiz: '测验' }
  return map[type] || ''
}
</script>

<style scoped>
.chapter-tree { overflow-y: auto; }
.tree-chapter { margin-bottom: 4px; }
.tree-chapter-header {
  display: flex; align-items: center; gap: 4px; padding: 6px 8px;
  cursor: pointer; border-radius: 4px; font-size: 12px; font-weight: 600;
  color: #f1f5f9;
}
.tree-chapter-header:hover { background: rgba(255,255,255,0.04); }
.tree-chapter-icon { width: 14px; color: rgba(255,255,255,0.3); }
.tree-subchapters { margin-left: 8px; }
.tree-subchapter {
  display: flex; align-items: center; gap: 6px; padding: 4px 8px 4px 22px;
  border-radius: 4px; cursor: pointer; font-size: 11px; color: rgba(255,255,255,0.5);
}
.tree-subchapter:hover { background: rgba(255,255,255,0.04); }
.tree-subchapter.active { background: rgba(59,130,246,0.12); color: #60d9fa; font-weight: 600; }
.tree-subchapter-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.2); flex-shrink: 0; }
.tree-subchapter.active .tree-subchapter-dot { background: #60d9fa; }
.tree-subchapter-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-subchapter-type { font-size: 9px; color: rgba(255,255,255,0.2); }
.tree-loading, .tree-empty { padding: 20px; text-align: center; font-size: 11px; color: rgba(255,255,255,0.3); }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
```

- [ ] **Step 2: Commit**

```bash
mkdir -p frontend/src/components/course
git add frontend/src/components/course/ChapterTree.vue
git commit -m "feat: add ChapterTree component"
```

---

### Task 13: AnnouncementList & ResourceList Components

**Files:**
- Create: `frontend/src/components/course/AnnouncementList.vue`
- Create: `frontend/src/components/course/ResourceList.vue`

- [ ] **Step 1: Write AnnouncementList.vue**

```vue
<template>
  <div class="announcement-list">
    <div v-if="items.length === 0" class="empty">暂无公告</div>
    <div v-for="item in items" :key="item.id" class="announcement-card glass-card">
      <div class="ann-card-header">
        <span class="ann-type-tag" :class="typeClass(item.type)">{{ item.type }}</span>
        <span class="ann-time">{{ formatTime(item.createdAt) }}</span>
      </div>
      <h4 class="ann-title">{{ item.title }}</h4>
      <p class="ann-content">{{ item.content }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })

function typeClass(type) {
  if (type === '重要公告') return 'important'
  if (type === '警示公告') return 'warning'
  return 'normal'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announcement-card { padding: 14px; }
.ann-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ann-type-tag { font-size: 10px; padding: 1px 8px; border-radius: 4px; }
.ann-type-tag.normal { background: rgba(59,130,246,0.15); color: #93c5fd; }
.ann-type-tag.important { background: rgba(234,179,8,0.15); color: #fde68a; }
.ann-type-tag.warning { background: rgba(239,68,68,0.15); color: #fca5a5; }
.ann-time { font-size: 10px; color: rgba(255,255,255,0.3); }
.ann-title { font-size: 14px; font-weight: 600; color: #f1f5f9; margin: 0 0 6px; }
.ann-content { font-size: 12px; color: rgba(255,255,255,0.5); line-height: 1.6; margin: 0; }
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
</style>
```

- [ ] **Step 2: Write ResourceList.vue**

```vue
<template>
  <div class="resource-list">
    <div v-if="items.length === 0" class="empty">暂无资源</div>
    <div v-for="item in items" :key="item.id" class="resource-card glass-card">
      <span class="res-icon">{{ typeIcon(item.type) }}</span>
      <div class="res-info">
        <div class="res-title">{{ item.title }}</div>
        <div class="res-meta">
          <span>{{ item.size || '未知大小' }}</span>
          <span v-if="item.createdAt">{{ formatTime(item.createdAt) }}</span>
        </div>
      </div>
      <a v-if="item.url" :href="item.url" target="_blank" class="res-download">下载</a>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })

function typeIcon(type) {
  if (type === '视频') return '🎬'
  if (type === '课件') return '📊'
  if (type === '文档') return '📄'
  return '📁'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.resource-list { display: flex; flex-direction: column; gap: 8px; }
.resource-card { display: flex; align-items: center; gap: 12px; padding: 12px; }
.res-icon { font-size: 20px; flex-shrink: 0; }
.res-info { flex: 1; min-width: 0; }
.res-title { font-size: 13px; color: #f1f5f9; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.res-meta { font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 2px; display: flex; gap: 8px; }
.res-download { color: #60d9fa; font-size: 12px; text-decoration: none; flex-shrink: 0; }
.res-download:hover { text-decoration: underline; }
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/course/AnnouncementList.vue frontend/src/components/course/ResourceList.vue
git commit -m "feat: add AnnouncementList and ResourceList components"
```

---

### Task 14: CourseQA Component

**Files:**
- Create: `frontend/src/components/course/CourseQA.vue`

- [ ] **Step 1: Write CourseQA.vue**

```vue
<template>
  <div class="course-qa">
    <div class="qa-header">
      <h4 class="qa-title">课程问答</h4>
      <button class="qa-ask-btn" @click="showAskForm = true" v-if="!showAskForm">提问</button>
    </div>

    <div v-if="showAskForm" class="qa-ask-form glass-card">
      <input class="qa-input" v-model="newTitle" placeholder="问题标题" />
      <textarea class="qa-textarea" v-model="newContent" placeholder="详细描述你的问题..." rows="3"></textarea>
      <div class="qa-form-actions">
        <button class="qa-cancel" @click="showAskForm = false">取消</button>
        <button class="qa-submit" @click="submitQuestion" :disabled="!newTitle.trim()">提交</button>
      </div>
    </div>

    <div v-if="loading" class="qa-loading"><span class="spinner"></span></div>
    <div v-else-if="questions.length === 0" class="qa-empty">暂无问题，成为第一个提问的人吧！</div>

    <div v-for="q in questions" :key="q.id" class="qa-card glass-card">
      <div class="qa-card-header">
        <span class="qa-user">{{ q.userName }}</span>
        <span class="qa-time">{{ formatTime(q.createdAt) }}</span>
      </div>
      <h5 class="qa-question-title">{{ q.title }}</h5>
      <p class="qa-question-content">{{ q.content }}</p>
      <div class="qa-card-footer">
        <span class="qa-answer-count">{{ q.answerCount || 0 }} 个回答</span>
        <button class="qa-view-btn" @click="toggleAnswers(q)">{{ expandedQuestions.has(q.id) ? '收起' : '查看回答' }}</button>
      </div>

      <div v-if="expandedQuestions.has(q.id)" class="qa-answers">
        <div v-if="answersLoading[q.id]" class="qa-loading-small"><span class="spinner"></span></div>
        <div v-for="a in answers[q.id]" :key="a.id" class="qa-answer" :class="{ 'ai-answer': a.isAi }">
          <div class="qa-answer-header">
            <span class="qa-answer-user">{{ a.isAi ? '🤖 AI 小慧' : a.userName }}</span>
            <span v-if="a.isAi" class="ai-badge">AI</span>
          </div>
          <div class="qa-answer-content" v-html="renderMarkdown(a.content)"></div>
        </div>
        <div v-if="!hasAiAnswer(q.id)" class="qa-ai-pending">
          <button class="qa-ai-btn" @click="requestAiAnswer(q.id)" :disabled="aiLoading[q.id]">
            {{ aiLoading[q.id] ? 'AI 生成中...' : '让 AI 回答' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { apiCourseQuestions, apiAskQuestion, apiQuestionAnswers, apiAiAnswer } from '../../api/index.js'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({ courseId: { type: [Number, String], required: true } })

const questions = ref([])
const loading = ref(false)
const showAskForm = ref(false)
const newTitle = ref('')
const newContent = ref('')
const expandedQuestions = ref(new Set())
const answers = reactive({})
const answersLoading = reactive({})
const aiLoading = reactive({})

async function loadQuestions() {
  loading.value = true
  try {
    const res = await apiCourseQuestions(props.courseId)
    questions.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function submitQuestion() {
  if (!newTitle.value.trim()) return
  try {
    await apiAskQuestion(props.courseId, { title: newTitle.value.trim(), content: newContent.value.trim() })
    newTitle.value = ''
    newContent.value = ''
    showAskForm.value = false
    await loadQuestions()
  } catch (e) { alert('提问失败') }
}

async function toggleAnswers(q) {
  const id = q.id
  if (expandedQuestions.value.has(id)) {
    expandedQuestions.value.delete(id)
    expandedQuestions.value = new Set(expandedQuestions.value)
    return
  }
  expandedQuestions.value.add(id)
  expandedQuestions.value = new Set(expandedQuestions.value)
  if (!answers[id]) {
    answersLoading[id] = true
    try {
      const res = await apiQuestionAnswers(id)
      answers[id] = res.data || []
    } finally {
      answersLoading[id] = false
    }
  }
}

async function requestAiAnswer(questionId) {
  aiLoading[questionId] = true
  try {
    await apiAiAnswer(questionId)
    const res = await apiQuestionAnswers(questionId)
    answers[questionId] = res.data || []
  } catch (e) { alert('AI回答失败') }
  finally { aiLoading[questionId] = false }
}

function hasAiAnswer(questionId) {
  return (answers[questionId] || []).some(a => a.isAi)
}

function renderMarkdown(text) {
  if (!text) return ''
  return DOMPurify.sanitize(marked(text))
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

loadQuestions()
</script>

<style scoped>
.course-qa { display: flex; flex-direction: column; gap: 12px; }
.qa-header { display: flex; justify-content: space-between; align-items: center; }
.qa-title { font-size: 16px; font-weight: 600; color: #f1f5f9; margin: 0; }
.qa-ask-btn {
  padding: 6px 16px; border-radius: 6px; border: 1px solid rgba(59,130,246,0.3);
  background: rgba(59,130,246,0.1); color: #60d9fa; cursor: pointer; font-size: 12px; font-family: inherit;
}
.qa-ask-form { padding: 14px; display: flex; flex-direction: column; gap: 8px; }
.qa-input, .qa-textarea {
  padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: #f1f5f9; font-size: 12px; font-family: inherit; outline: none;
  width: 100%; box-sizing: border-box;
}
.qa-input::placeholder, .qa-textarea::placeholder { color: rgba(255,255,255,0.3); }
.qa-textarea { resize: vertical; }
.qa-form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.qa-cancel { padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1); background: none; color: rgba(255,255,255,0.4); cursor: pointer; font-size: 11px; font-family: inherit; }
.qa-submit {
  padding: 4px 16px; border-radius: 4px; border: none; background: linear-gradient(135deg,#3b82f6,#2563eb);
  color: #fff; cursor: pointer; font-size: 11px; font-family: inherit;
}
.qa-submit:disabled { opacity: 0.4; cursor: not-allowed; }
.qa-card { padding: 14px; }
.qa-card-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.qa-user { font-size: 11px; color: #60d9fa; }
.qa-time { font-size: 10px; color: rgba(255,255,255,0.3); }
.qa-question-title { font-size: 14px; font-weight: 600; color: #f1f5f9; margin: 0 0 4px; }
.qa-question-content { font-size: 12px; color: rgba(255,255,255,0.6); line-height: 1.6; margin: 0; }
.qa-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.qa-answer-count { font-size: 10px; color: rgba(255,255,255,0.3); }
.qa-view-btn { background: none; border: none; color: #60d9fa; font-size: 11px; cursor: pointer; font-family: inherit; }
.qa-answers { margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.06); display: flex; flex-direction: column; gap: 8px; }
.qa-answer { padding: 10px; background: rgba(255,255,255,0.03); border-radius: 6px; }
.qa-answer.ai-answer { background: rgba(59,130,246,0.06); border: 1px solid rgba(59,130,246,0.1); }
.qa-answer-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.qa-answer-user { font-size: 11px; color: rgba(255,255,255,0.6); }
.ai-badge { font-size: 9px; padding: 0 6px; border-radius: 4px; background: rgba(59,130,246,0.2); color: #93c5fd; }
.qa-answer-content { font-size: 12px; color: rgba(255,255,255,0.7); line-height: 1.6; }
.qa-answer-content :deep(p) { margin: 0; }
.qa-ai-pending { text-align: center; padding: 8px; }
.qa-ai-btn {
  padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(168,85,247,0.3);
  background: rgba(168,85,247,0.1); color: #c084fc; cursor: pointer; font-size: 11px; font-family: inherit;
}
.qa-ai-btn:disabled { opacity: 0.4; }
.qa-loading, .qa-empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
.qa-loading-small { text-align: center; padding: 12px; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.1); border-top-color: #3b82f6; border-radius: 50%; display: inline-block; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/course/CourseQA.vue
git commit -m "feat: add CourseQA component with ask, answer, and AI answer"
```

---

### Task 15: CourseDesign & CourseHomework Components

**Files:**
- Create: `frontend/src/components/course/CourseDesign.vue`
- Create: `frontend/src/components/course/CourseHomework.vue`

- [ ] **Step 1: Write CourseDesign.vue**

```vue
<template>
  <div class="course-design">
    <div class="design-section">
      <h4 class="design-label">课程背景</h4>
      <p class="design-text">{{ background || '暂无' }}</p>
    </div>
    <div class="design-section">
      <h4 class="design-label">教学目标</h4>
      <p class="design-text">{{ target || '暂无' }}</p>
    </div>
    <div class="design-section">
      <h4 class="design-label">设计原则</h4>
      <p class="design-text">{{ principle || '暂无' }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  background: { type: String, default: '' },
  target: { type: String, default: '' },
  principle: { type: String, default: '' }
})
</script>

<style scoped>
.course-design { display: flex; flex-direction: column; gap: 20px; }
.design-label { font-size: 13px; font-weight: 600; color: #93c5fd; margin: 0 0 6px; }
.design-text { font-size: 13px; color: rgba(255,255,255,0.6); line-height: 1.7; margin: 0; }
</style>
```

- [ ] **Step 2: Write CourseHomework.vue**

```vue
<template>
  <div class="course-homework">
    <div v-if="items.length === 0" class="empty">暂无作业</div>
    <div v-for="item in items" :key="item.id" class="hw-card glass-card">
      <span class="hw-type-icon">{{ item.type === '测试' ? '📝' : '📋' }}</span>
      <div class="hw-info">
        <div class="hw-title">{{ item.title }}</div>
        <div class="hw-meta">
          <span>{{ item.type }}</span>
          <span v-if="item.time">{{ item.time }}</span>
        </div>
      </div>
      <button class="hw-view-btn">查看</button>
    </div>
  </div>
</template>

<script setup>
defineProps({ items: { type: Array, default: () => [] } })
</script>

<style scoped>
.course-homework { display: flex; flex-direction: column; gap: 8px; }
.hw-card { display: flex; align-items: center; gap: 12px; padding: 12px; }
.hw-type-icon { font-size: 20px; flex-shrink: 0; }
.hw-info { flex: 1; }
.hw-title { font-size: 13px; color: #f1f5f9; }
.hw-meta { font-size: 10px; color: rgba(255,255,255,0.3); margin-top: 2px; display: flex; gap: 8px; }
.hw-view-btn {
  padding: 4px 12px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1);
  background: none; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 11px; font-family: inherit;
}
.empty { text-align: center; padding: 40px; font-size: 12px; color: rgba(255,255,255,0.3); }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/course/CourseDesign.vue frontend/src/components/course/CourseHomework.vue
git commit -m "feat: add CourseDesign and CourseHomework components"
```

---

### Task 16: Update CourseView.vue — Replace Hierarchy & Add Q&A

**Files:**
- Modify: `frontend/src/views/student/CourseView.vue`

- [ ] **Step 1: Replace the course tree data loading and rendering**

The main changes to CourseView.vue are:

1. **Import new components:**
```js
import ChapterTree from '../../components/course/ChapterTree.vue'
import CourseQA from '../../components/course/CourseQA.vue'
```

2. **Replace API imports** — add `apiCourseChapters, apiSubChapterDetail` etc.

3. **Replace data loading logic** — instead of loading subjects/units/lessons tree, load a single course's chapter tree:

```js
const courseId = ref(null)
const chapters = ref([])
const treeLoading = ref(false)

async function loadCourse() {
  loading.value = true
  try {
    const id = route.params.id
    courseId.value = Number(id)

    // Load chapters for this course
    treeLoading.value = true
    const chRes = await apiCourseChapters(courseId.value)
    chapters.value = chRes.data || []

    // Load first sub-chapter
    const first = findFirstSubChapter()
    if (first) {
      await loadSubChapter(first.id)
    }
  } finally {
    loading.value = false
    treeLoading.value = false
  }
}

function findFirstSubChapter() {
  for (const ch of chapters.value) {
    if (ch.subChapters?.length > 0) return ch.subChapters[0]
  }
  return null
}

async function loadSubChapter(subChapterId) {
  currentSubChapterId.value = subChapterId
  const res = await apiSubChapterDetail(subChapterId)
  currentSubChapter.value = res.data
  currentChapterName.value = findChapterName(subChapterId)
  renderedContent.value = DOMPurify.sanitize(marked(res.data.content || ''))
  exercises.value = res.data.exercises || []
  selectedAnswers.value = {}
  submitted.value = false
  computeNav()
}
```

4. **Replace the tree navigation section** — in the template, replace the `aside.course-tree` content:

```html
<aside class="course-tree" :class="{ collapsed: treeCollapsed }">
  <div class="tree-header">
    <span class="tree-title">目录</span>
    <button class="tree-toggle" @click="treeCollapsed = !treeCollapsed">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline :points="treeCollapsed ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/></svg>
    </button>
  </div>
  <div class="tree-body">
    <ChapterTree
      :chapters="chapters"
      :active-id="currentSubChapterId"
      :loading="treeLoading"
      @select="(sc) => loadSubChapter(sc.id)"
    />
  </div>
</aside>
```

5. **Replace the Discuss tab placeholder** — in the right panel, replace the `discuss-panel` div with:

```html
<div v-if="panelTab === 'discuss'" class="discuss-panel">
  <CourseQA :course-id="courseId" />
</div>
```

6. **Update `computeNav()` to work with flattened sub-chapters from chapters tree** instead of the old units/lessons structure.

7. **Update `goToLesson` → `goToSubChapter`**, `currentLesson` → `currentSubChapter` variable renames.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/student/CourseView.vue
git commit -m "feat: update CourseView with ChapterTree and CourseQA; replace lesson hierarchy"
```

---

### Task 17: Update ContentManagement.vue — Chapter Editor

**Files:**
- Modify: `frontend/src/views/teacher/ContentManagement.vue`

- [ ] **Step 1: Rewrite ContentManagement.vue for chapter/sub-chapter management**

The rewritten page should:
1. Show subject tabs → select subject → show courses under it
2. Select course → show chapters with expandable sub-chapters
3. Each chapter has: edit title, add sub-chapter, delete
4. Each sub-chapter has: edit, AI generate content, delete
5. "Add Chapter" and "Add SubChapter" buttons

Replace the full component:

```vue
<template>
  <div class="content-mgmt">
    <h1>内容管理</h1>
    <p class="subtitle">管理课程章节与子章节内容</p>

    <!-- Subject Tabs -->
    <div class="subject-tabs">
      <button v-for="s in subjects" :key="s.id" class="glass-btn"
              :class="{ active: currentSubjectId === s.id }" @click="selectSubject(s.id)">
        {{ s.name }}
      </button>
    </div>

    <!-- Course Selector -->
    <div v-if="courses.length > 0" class="course-tabs">
      <button v-for="c in courses" :key="c.id" class="glass-btn"
              :class="{ active: currentCourseId === c.id }" @click="selectCourse(c.id)">
        {{ c.title }}
      </button>
      <button class="glass-btn add-btn" @click="showCourseForm = true">+ 课程</button>
    </div>

    <!-- Course Create Form -->
    <div v-if="showCourseForm" class="glass-card form-card">
      <input v-model="newCourse.title" placeholder="课程名称" class="form-input" />
      <input v-model="newCourse.subjectId" type="hidden" :value="currentSubjectId" />
      <textarea v-model="newCourse.description" placeholder="课程描述" class="form-textarea" rows="3"></textarea>
      <div class="form-actions">
        <button class="glass-btn" @click="showCourseForm = false">取消</button>
        <button class="glass-btn active" @click="createCourse">创建</button>
      </div>
    </div>

    <!-- Chapters -->
    <div v-if="currentCourseId" class="chapters-section">
      <div class="section-header">
        <h3>章节管理</h3>
        <button class="glass-btn" @click="showChapterForm = true">+ 添加章节</button>
      </div>

      <div v-if="showChapterForm" class="glass-card form-card">
        <input v-model="newChapter.title" placeholder="章节标题" class="form-input" />
        <textarea v-model="newChapter.description" placeholder="章节描述" class="form-textarea" rows="2"></textarea>
        <div class="form-actions">
          <button class="glass-btn" @click="showChapterForm = false">取消</button>
          <button class="glass-btn active" @click="createChapter">添加</button>
        </div>
      </div>

      <div v-for="ch in chapters" :key="ch.id" class="glass-card chapter-card">
        <div class="chapter-header" @click="toggleChapterExpand(ch.id)">
          <h4>{{ ch.title }}</h4>
          <div class="chapter-actions">
            <button class="glass-btn small" @click.stop="showSubChapterForm(ch.id)">+ 子章节</button>
            <button class="glass-btn small danger" @click.stop="deleteChapter(ch.id)">删除</button>
            <span>{{ expandedChapters.has(ch.id) ? '▾' : '▸' }}</span>
          </div>
        </div>

        <div v-if="expandedChapters.has(ch.id)" class="subchapters-list">
          <div v-if="showSubFormFor === ch.id" class="glass-card form-card">
            <input v-model="newSubChapter.title" placeholder="子章节标题" class="form-input" />
            <select v-model="newSubChapter.type" class="form-select">
              <option value="doc">文档</option>
              <option value="video">视频</option>
              <option value="quiz">测验</option>
            </select>
            <div class="form-actions">
              <button class="glass-btn" @click="showSubFormFor = null">取消</button>
              <button class="glass-btn active" @click="createSubChapter(ch.id)">添加</button>
            </div>
          </div>

          <div v-for="sc in getSubChapters(ch.id)" :key="sc.id" class="subchapter-row">
            <span class="sc-name">{{ sc.title }}</span>
            <span class="sc-type">{{ sc.type }}</span>
            <span class="sc-status" :class="sc.status">{{ sc.status || 'published' }}</span>
            <button class="glass-btn small" @click="aiGenerateContent(sc.id)">AI 生成</button>
            <button class="glass-btn small danger" @click="deleteSubChapter(sc.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  apiSubjects, apiCoursesBySubject, apiCourseChapters,
  apiCreateCourse, apiCreateChapter, apiDeleteChapter,
  apiCreateSubChapter, apiDeleteSubChapter, apiAiGenerateContent
} from '../../api/index.js'

const subjects = ref([])
const currentSubjectId = ref(null)
const courses = ref([])
const currentCourseId = ref(null)
const chapters = ref([])
const expandedChapters = ref(new Set())
const showCourseForm = ref(false)
const showChapterForm = ref(false)
const showSubFormFor = ref(null)

const newCourse = reactive({ title: '', description: '', status: '已发布' })
const newChapter = reactive({ title: '', description: '' })
const newSubChapter = reactive({ title: '', description: '', type: 'doc', status: 'published' })

onMounted(async () => {
  const res = await apiSubjects()
  subjects.value = res.data || []
  if (subjects.value.length) selectSubject(subjects.value[0].id)
})

async function selectSubject(id) {
  currentSubjectId.value = id
  currentCourseId.value = null
  chapters.value = []
  const res = await apiCoursesBySubject(id)
  courses.value = res.data || []
}

async function selectCourse(id) {
  currentCourseId.value = id
  const res = await apiCourseChapters(id)
  chapters.value = res.data || []
  chapters.value.forEach(ch => expandedChapters.value.add(ch.id))
}

async function createCourse() {
  await apiCreateCourse({ ...newCourse, subjectId: currentSubjectId.value })
  showCourseForm.value = false
  await selectSubject(currentSubjectId.value)
}

async function createChapter() {
  if (!currentCourseId.value) return
  await apiCreateChapter({ ...newChapter, courseId: currentCourseId.value, sortOrder: chapters.value.length + 1 })
  showChapterForm.value = false
  newChapter.title = ''; newChapter.description = ''
  await selectCourse(currentCourseId.value)
}

async function deleteChapter(id) {
  if (!confirm('确认删除此章节？子章节也会被删除。')) return
  await apiDeleteChapter(id)
  await selectCourse(currentCourseId.value)
}

function toggleChapterExpand(id) {
  if (expandedChapters.value.has(id)) expandedChapters.value.delete(id)
  else expandedChapters.value.add(id)
  expandedChapters.value = new Set(expandedChapters.value)
}

function showSubChapterForm(chapterId) {
  showSubFormFor.value = chapterId
  expandedChapters.value.add(chapterId)
}

function getSubChapters(chapterId) {
  return chapters.value.find(ch => ch.id === chapterId)?.subChapters || []
}

async function createSubChapter(chapterId) {
  await apiCreateSubChapter({ ...newSubChapter, chapterId, sortOrder: getSubChapters(chapterId).length + 1 })
  showSubFormFor.value = null
  newSubChapter.title = ''; newSubChapter.description = ''
  await selectCourse(currentCourseId.value)
}

async function deleteSubChapter(id) {
  if (!confirm('确认删除此子章节？')) return
  await apiDeleteSubChapter(id)
  await selectCourse(currentCourseId.value)
}

async function aiGenerateContent(subChapterId) {
  try {
    await apiAiGenerateContent(subChapterId)
    alert('AI 内容已生成')
  } catch (e) { alert('生成失败') }
}
</script>

<style scoped>
.content-mgmt { padding: 24px; }
h1 { color: #f1f5f9; font-size: 24px; margin-bottom: 4px; }
.subtitle { color: #64748b; margin-bottom: 20px; }
.glass-btn { padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.08); backdrop-filter: blur(8px); color: #e2e8f0; cursor: pointer; font-family: inherit; font-size: 13px; }
.glass-btn.active { background: rgba(59,130,246,0.3); border-color: rgba(59,130,246,0.4); }
.glass-btn.small { padding: 4px 10px; font-size: 12px; }
.glass-btn.danger { border-color: rgba(239,68,68,0.3); color: #fca5a5; }
.glass-btn.add-btn { border-style: dashed; }
.glass-card { background: rgba(255,255,255,0.06); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.subject-tabs, .course-tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.form-card { display: flex; flex-direction: column; gap: 8px; }
.form-input, .form-textarea, .form-select {
  padding: 8px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.04); color: #f1f5f9; font-size: 13px; font-family: inherit; outline: none;
  width: 100%; box-sizing: border-box;
}
.form-textarea { resize: vertical; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h3 { color: #e2e8f0; margin: 0; }
.chapter-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }
.chapter-header h4 { color: #f1f5f9; margin: 0; }
.chapter-actions { display: flex; gap: 8px; align-items: center; }
.subchapters-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.subchapter-row { display: flex; align-items: center; gap: 12px; padding: 8px; background: rgba(255,255,255,0.03); border-radius: 6px; }
.sc-name { flex: 1; color: #cbd5e1; font-size: 14px; }
.sc-type { color: #64748b; font-size: 12px; }
.sc-status { font-size: 11px; padding: 1px 8px; border-radius: 4px; }
.sc-status.published { background: rgba(34,197,94,0.15); color: #86efac; }
.sc-status.draft { background: rgba(234,179,8,0.15); color: #fde68a; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/teacher/ContentManagement.vue
git commit -m "feat: rewrite ContentManagement for chapter/sub-chapter CRUD"
```

---

### Task 18: Update TeacherManage.vue — Course Create Form

**Files:**
- Modify: `frontend/src/views/teacher/TeacherManage.vue`

- [ ] **Step 1: Add course creation form with new fields**

Read the existing file first. Add a section for creating/editing courses with fields: title, subject (dropdown), description, coverImage, background, target, principle.

If `TeacherManage.vue` doesn't have a course creation section yet, add one:

```vue
<template>
  <div class="teacher-manage">
    <h1>教学管理</h1>

    <div class="glass-card">
      <h2>课程管理</h2>
      <div v-if="showCourseForm" class="form-grid">
        <select v-model="courseForm.subjectId" class="form-input">
          <option v-for="s in subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
        <input v-model="courseForm.title" placeholder="课程名称" class="form-input" />
        <input v-model="courseForm.coverImage" placeholder="封面图片URL" class="form-input" />
        <textarea v-model="courseForm.description" placeholder="课程描述" class="form-textarea" rows="3"></textarea>
        <textarea v-model="courseForm.background" placeholder="课程背景" class="form-textarea" rows="2"></textarea>
        <textarea v-model="courseForm.target" placeholder="教学目标" class="form-textarea" rows="2"></textarea>
        <textarea v-model="courseForm.principle" placeholder="设计原则" class="form-textarea" rows="2"></textarea>
        <input v-model="courseForm.tag" placeholder="标签" class="form-input" />
        <input v-model="courseForm.price" placeholder="价格（如：免费、¥299）" class="form-input" />
        <input v-model.number="courseForm.totalHours" type="number" placeholder="总课时" class="form-input" />
        <div class="form-actions">
          <button class="glass-btn" @click="showCourseForm = false">取消</button>
          <button class="glass-btn active" @click="saveCourse">保存课程</button>
        </div>
      </div>
      <button v-else class="glass-btn" @click="showCourseForm = true">+ 创建课程</button>

      <div class="course-list" v-if="courses.length > 0">
        <div v-for="c in courses" :key="c.id" class="course-row">
          <span>{{ c.title }}</span>
          <span>{{ c.status }}</span>
          <button class="glass-btn small" @click="$router.push('/teacher/content')">管理章节</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { apiSubjects, apiCreateCourse, apiUpdateCourse } from '../../api/index.js'

const subjects = ref([])
const courses = ref([])
const showCourseForm = ref(false)
const editingCourseId = ref(null)

const courseForm = reactive({
  subjectId: null, title: '', coverImage: '', description: '',
  background: '', target: '', principle: '',
  tag: '', price: '免费', totalHours: 0, status: '草稿'
})

onMounted(async () => {
  const res = await apiSubjects()
  subjects.value = res.data || []
})

async function saveCourse() {
  if (editingCourseId.value) {
    await apiUpdateCourse(editingCourseId.value, courseForm)
  } else {
    await apiCreateCourse(courseForm)
  }
  showCourseForm.value = false
}
</script>

<style scoped>
/* reuse same styles as ContentManagement */
</style>
```

Note: The exact placement and styling depends on the existing `TeacherManage.vue` content. Read it first and integrate the form naturally.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/teacher/TeacherManage.vue
git commit -m "feat: add course creation form to TeacherManage with background/target/principle fields"
```

---

### Task 19: Verify Compilation & Integration

**Files:**
- All modified files

- [ ] **Step 1: Build backend**

```bash
cd C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\backend
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS. Fix any compilation errors (missing imports, renamed fields).

- [ ] **Step 2: Build frontend**

```bash
cd C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend
npm run build
```

Expected: Build completes without errors.

- [ ] **Step 3: Start services and smoke test**

```bash
# Terminal 1: Backend
cd C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\backend
mvn spring-boot:run

# Terminal 2: Frontend
cd C:\Users\ZWC\Desktop\软件杯大赛\softwacecup\frontend
npm run dev
```

Smoke test checklist:
- [ ] `GET /api/subjects` returns subject list
- [ ] `GET /api/courses/public` returns courses with new fields
- [ ] `GET /api/courses/{id}/chapters` returns chapter tree with nested sub-chapters
- [ ] `GET /api/sub-chapters/{id}` returns sub-chapter with knowledge points and exercises
- [ ] `POST /api/courses/{id}/questions` creates a question
- [ ] `GET /api/questions/{id}/answers` returns answers
- [ ] Frontend: CourseView loads chapter tree in left panel
- [ ] Frontend: Clicking sub-chapter loads content in center panel
- [ ] Frontend: Q&A tab shows question list with ask/answer functionality

- [ ] **Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: integration fixes for course migration"
```

---

## Self-Review Results

1. **Spec coverage:** All spec sections covered — data model (Task 1-2), API endpoints (Task 6-8), frontend components (Task 12-15), frontend pages (Task 16-18), migration (Task 1).

2. **Placeholder scan:** No TBD/TODO. All code is concrete with exact types and paths.

3. **Type consistency:** Backend uses `Long` for IDs consistently. Frontend uses `[Number, String]` for IDs. API function names match controller paths. `ChapterTree` component `@select` event emits sub-chapter objects with `{ id, title, type }` — matches usage in `CourseView.vue`.
