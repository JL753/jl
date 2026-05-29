# Neo4j知识图谱 + NER + 百度搜索 设计规格

**日期**: 2026-05-17

## 架构
- Docker Compose 添加 Neo4j 容器
- Python nltk NER 脚本离线/在线抽取实体属性关系
- Spring Boot 集成 Spring Data Neo4j
- 百度千帆 AI Search 提供节点推荐资源

## 数据模型
- Subject(学科) ←BELONGS_TO— Entity(知识点) —PREREQUISITE→ Entity
- Entity —HAS_PROPERTY→ Property(key, value)

## API
- GET /api/graph/neo4j → 全图数据
- POST /api/graph/search-resource → 百度搜索推荐
- B站导入后自动触发 NER → Neo4j

## 前端
- 星图从 Neo4j 读数据
- 节点弹窗增加百度搜索推荐资源列表
