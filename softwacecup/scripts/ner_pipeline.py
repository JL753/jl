#!/usr/bin/env python3
"""NER Pipeline: nltk 抽取实体/属性/关系 → 写入 Neo4j"""
import sys, json, os, argparse, re
import nltk
from neo4j import GraphDatabase

# 下载 nltk 数据（首次运行）
for pkg in ['punkt', 'averaged_perceptron_tagger', 'maxent_ne_chunker', 'words', 'stopwords']:
    try: nltk.data.find(f'tokenizers/{pkg}' if pkg == 'punkt' else f'taggers/{pkg}' if 'perceptron' in pkg else f'chunkers/{pkg}' if 'chunk' in pkg else f'corpora/{pkg}')
    except: nltk.download(pkg, quiet=True)

NEO4J_URI = os.environ.get('NEO4J_URI', 'bolt://localhost:7687')
NEO4J_USER = os.environ.get('NEO4J_USER', 'neo4j')
NEO4J_PASS = os.environ.get('NEO4J_PASS', 'password123')

KEYWORD_PATTERNS = {
    '编程语言': r'(C\+\+|Java|Python|JavaScript|Go|Rust|C#|PHP|Ruby|Swift|Kotlin|TypeScript|C语言)',
    '数据结构': r'(数组|链表|栈|队列|哈希表|树|二叉树|堆|图|散列表|字符串|矩阵)',
    '算法': r'(排序|搜索|递归|动态规划|贪心|回溯|分治|DFS|BFS|二分|遍历|Dijkstra)',
    '开发工具': r'(VS ?Code|IntelliJ|Eclipse|Git|Docker|Maven|Gradle|npm|pip|Linux|Unix)',
    '概念': r'(面向对象|函数式|并发|多线程|异步|REST|API|MVC|ORM|微服务|容器)',
}

DIFFICULTY_MAP = {'入门': 1, '基础': 1, '初级': 1, '中级': 2, '进阶': 2, '高级': 3, '精通': 3}

def extract_entities(text):
    """从文本中提取命名实体"""
    entities = []
    sentences = nltk.sent_tokenize(text[:5000] if text else '')
    for sent in sentences:
        words = nltk.word_tokenize(sent)
        tagged = nltk.pos_tag(words)
        chunks = nltk.ne_chunk(tagged)
        for chunk in chunks:
            if hasattr(chunk, 'label'):
                name = ' '.join(c[0] for c in chunk)
                entities.append({'name': name, 'type': chunk.label()})

    # 规则匹配补充
    for category, pattern in KEYWORD_PATTERNS.items():
        for match in re.finditer(pattern, text if text else ''):
            entities.append({'name': match.group(), 'type': category})

    # 去重
    seen = set()
    unique = []
    for e in entities:
        if e['name'] not in seen:
            seen.add(e['name'])
            unique.append(e)
    return unique

def extract_properties(text, title='', duration=None):
    """提取属性：难度、时长、来源"""
    props = []
    if title:
        props.append({'key': 'title', 'value': title[:100]})
    if duration:
        props.append({'key': 'duration_min', 'value': str(duration // 60)})

    # 难度检测
    for keyword, level in DIFFICULTY_MAP.items():
        if keyword in (text or '')[:500]:
            props.append({'key': 'difficulty', 'value': str(level)})
            break

    # 来源
    props.append({'key': 'source', 'value': 'B站导入'})
    return props

def extract_relations(entities, lesson_id):
    """根据实体顺序构建 PREREQUISITE 关系"""
    relations = []
    for i in range(len(entities) - 1):
        relations.append({
            'from': entities[i]['name'],
            'to': entities[i + 1]['name'],
            'type': 'PREREQUISITE'
        })
    return relations

def write_to_neo4j(subject_name, subject_id, entities, properties, relations, lesson_id):
    """写入 Neo4j"""
    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASS))
    with driver.session() as session:
        # 确保 Subject 节点存在
        session.run(
            "MERGE (s:Subject {id: $id}) SET s.name = $name",
            id=str(subject_id), name=subject_name
        )
        # 写入实体节点
        for entity in entities:
            session.run(
                "MERGE (e:Entity {name: $name}) "
                "SET e.type = $type, e.lessonId = $lessonId, e.subjectId = $subjectId",
                name=entity['name'], type=entity.get('type', 'Unknown'),
                lessonId=str(lesson_id), subjectId=str(subject_id)
            )
            # BELONGS_TO 关系
            session.run(
                "MATCH (e:Entity {name: $ename}), (s:Subject {id: $sid}) "
                "MERGE (e)-[:BELONGS_TO]->(s)",
                ename=entity['name'], sid=str(subject_id)
            )
        # 属性节点
        for prop in properties:
            session.run(
                "MERGE (p:Property {key: $key}) SET p.value = $value "
                "WITH p MATCH (e:Entity {name: $ename}) "
                "MERGE (e)-[:HAS_PROPERTY]->(p)",
                key=prop['key'], value=prop['value'],
                ename=entities[0]['name'] if entities else ''
            )
        # PREREQUISITE 关系
        for rel in relations:
            session.run(
                "MATCH (a:Entity {name: $from}), (b:Entity {name: $to}) "
                "MERGE (a)-[:PREREQUISITE]->(b)",
                **rel
            )
    driver.close()
    return True

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--lesson-id', required=True)
    parser.add_argument('--title', default='')
    parser.add_argument('--content', default='')
    parser.add_argument('--subject-name', default='')
    parser.add_argument('--subject-id', default='')
    parser.add_argument('--duration', type=int, default=0)
    args = parser.parse_args()

    text = f"{args.title}\n{args.content}"
    entities = extract_entities(text)
    properties = extract_properties(args.content, args.title, args.duration)
    relations = extract_relations(entities, args.lesson_id)

    try:
        write_to_neo4j(args.subject_name, args.subject_id, entities, properties, relations, args.lesson_id)
        result = {'success': True, 'entities': len(entities), 'relations': len(relations), 'properties': len(properties)}
    except Exception as e:
        result = {'success': False, 'error': str(e)}

    print(json.dumps(result, ensure_ascii=False))
