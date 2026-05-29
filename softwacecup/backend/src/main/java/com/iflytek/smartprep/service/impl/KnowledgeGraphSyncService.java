package com.iflytek.smartprep.service.impl;

import com.iflytek.smartprep.domain.*;
import com.iflytek.smartprep.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphSyncService {

    private final Driver neo4jDriver;
    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final SubChapterMapper subChapterMapper;
    private final KnowledgePointMapper kpMapper;
    private final KpDependencyMapper depMapper;

    @PostConstruct
    public void syncOnStartup() {
        try {
            syncAll();
            log.info("[Neo4j Sync] Knowledge graph synced successfully");
        } catch (Exception e) {
            log.warn("[Neo4j Sync] Sync failed on startup: {}", e.getMessage());
        }
    }

    public void syncAll() {
        try (Session session = neo4jDriver.session()) {
            // Clear existing data
            session.run("MATCH (n) DETACH DELETE n");

            // Sync Subjects
            List<Subject> subjects = subjectMapper.selectList(null);
            for (Subject s : subjects) {
                session.run(
                    "CREATE (:Entity {id: $id, name: $name, type: 'subject', subjectId: $id})",
                    Map.of("id", String.valueOf(s.getId()), "name", s.getName() != null ? s.getName() : ""));
            }

            // Sync Courses
            List<Course> courses = courseMapper.selectList(null);
            for (Course c : courses) {
                session.run(
                    "CREATE (:Entity {id: $id, name: $name, type: 'course', courseId: $id, subjectId: $subjectId})",
                    Map.of("id", String.valueOf(c.getId()), "name", c.getTitle() != null ? c.getTitle() : "",
                           "subjectId", c.getSubjectId() != null ? String.valueOf(c.getSubjectId()) : ""));
            }

            // Sync Chapters
            List<Chapter> chapters = chapterMapper.selectList(null);
            for (Chapter ch : chapters) {
                session.run(
                    "CREATE (:Entity {id: $id, name: $name, type: 'chapter', chapterId: $id, courseId: $courseId})",
                    Map.of("id", String.valueOf(ch.getId()), "name", ch.getTitle() != null ? ch.getTitle() : "",
                           "courseId", ch.getCourseId() != null ? String.valueOf(ch.getCourseId()) : ""));
            }

            // Sync SubChapters
            List<SubChapter> subChapters = subChapterMapper.selectList(null);
            for (SubChapter sc : subChapters) {
                session.run(
                    "CREATE (:Entity {id: $id, name: $name, type: 'sub_chapter', subChapterId: $id, chapterId: $chapterId})",
                    Map.of("id", String.valueOf(sc.getId()), "name", sc.getTitle() != null ? sc.getTitle() : "",
                           "chapterId", sc.getChapterId() != null ? String.valueOf(sc.getChapterId()) : ""));
            }

            // Sync KnowledgePoints
            List<KnowledgePoint> kps = kpMapper.selectList(null);
            for (KnowledgePoint kp : kps) {
                session.run(
                    "CREATE (:Entity {id: $id, name: $name, type: 'knowledge_point', kpId: $id, subChapterId: $subChapterId, difficultyLevel: $difficulty})",
                    Map.of("id", String.valueOf(kp.getId()), "name", kp.getName() != null ? kp.getName() : "",
                           "subChapterId", kp.getLessonId() != null ? String.valueOf(kp.getLessonId()) : "",
                           "difficulty", kp.getDifficultyLevel() != null ? kp.getDifficultyLevel() : 0));
            }

            // Propagate courseId to SubChapter nodes via Chapter chain
            session.run(
                "MATCH (sc:Entity {type: 'sub_chapter'})-[:CONTAINS]->(ch:Entity {type: 'chapter'})-[:CONTAINS]->(c:Entity {type: 'course'}) " +
                "SET sc.courseId = c.courseId");

            // Propagate courseId to KnowledgePoint nodes via SubChapter→Chapter chain
            session.run(
                "MATCH (kp:Entity {type: 'knowledge_point'})-[:BELONGS_TO]->(sc:Entity {type: 'sub_chapter'}) " +
                "SET kp.courseId = sc.courseId, kp.subChapterId = sc.subChapterId");

            // Create BELONGS_TO edges: Course → Subject
            for (Course c : courses) {
                if (c.getSubjectId() != null) {
                    session.run(
                        "MATCH (a:Entity {id: $courseId}), (b:Entity {id: $subjectId}) " +
                        "CREATE (a)-[:BELONGS_TO]->(b)",
                        Map.of("courseId", String.valueOf(c.getId()),
                               "subjectId", String.valueOf(c.getSubjectId())));
                }
            }

            // Create CONTAINS edges: Chapter → Course
            for (Chapter ch : chapters) {
                if (ch.getCourseId() != null) {
                    session.run(
                        "MATCH (a:Entity {id: $chapterId}), (b:Entity {id: $courseId}) " +
                        "CREATE (a)-[:CONTAINS]->(b)",
                        Map.of("chapterId", String.valueOf(ch.getId()),
                               "courseId", String.valueOf(ch.getCourseId())));
                }
            }

            // Create CONTAINS edges: SubChapter → Chapter
            for (SubChapter sc : subChapters) {
                if (sc.getChapterId() != null) {
                    session.run(
                        "MATCH (a:Entity {id: $scId}), (b:Entity {id: $chapterId}) " +
                        "CREATE (a)-[:CONTAINS]->(b)",
                        Map.of("scId", String.valueOf(sc.getId()),
                               "chapterId", String.valueOf(sc.getChapterId())));
                }
            }

            // Create BELONGS_TO edges: KnowledgePoint → SubChapter
            for (KnowledgePoint kp : kps) {
                if (kp.getLessonId() != null) {
                    session.run(
                        "MATCH (a:Entity {id: $kpId}), (b:Entity {id: $scId}) " +
                        "CREATE (a)-[:BELONGS_TO]->(b)",
                        Map.of("kpId", String.valueOf(kp.getId()),
                               "scId", String.valueOf(kp.getLessonId())));
                }
            }

            // Create DEPENDS_ON edges: KnowledgePoint → KnowledgePoint
            for (KpDependency dep : depMapper.selectList(null)) {
                session.run(
                    "MATCH (a:Entity {id: $fromId}), (b:Entity {id: $toId}) " +
                    "CREATE (a)-[:DEPENDS_ON]->(b)",
                    Map.of("fromId", String.valueOf(dep.getDependsOnKpId()),
                           "toId", String.valueOf(dep.getKpId())));
            }
        }
    }
}
