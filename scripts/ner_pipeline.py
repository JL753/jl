#!/usr/bin/env python3
"""NER Pipeline:  抽取实体/属性/关系 → 写入 Neo4j（纯规则引擎，不依赖nltk）"""
import sys, json, os, argparse, re

# 尝试导入 nltk 增强识别，失败则用纯规则
try:
    import nltk
    for pkg in ['punkt', 'averaged_perceptron_tagger', 'maxent_ne_chunker', 'words']:
        try: nltk.data.find(f'tokenizers/{pkg}' if pkg == 'punkt' else f'taggers/{pkg}' if 'perceptron' in pkg else f'chunkers/{pkg}' if 'chunk' in pkg else f'corpora/{pkg}')
        except: pass
    HAS_NLTK = True
except:
    HAS_NLTK = False

try:
    from neo4j import GraphDatabase
    HAS_NEO4J = True
except:
    HAS_NEO4J = False

NEO4J_URI = os.environ.get('NEO4J_URI', 'bolt://localhost:7687')
NEO4J_USER = os.environ.get('NEO4J_USER', 'neo4j')
NEO4J_PASS = os.environ.get('NEO4J_PASS', 'password123')

KEYWORD_PATTERNS = {
    '编程语言': r'(C\+\+|C语言|Java|Python|JavaScript|Go|Rust|C#|PHP|Ruby|Swift|Kotlin|TypeScript)',
    '数据结构': r'(数组|链表|栈|队列|哈希表|树|二叉树|堆|图|散列表|字符串|矩阵|B树|红黑树|跳表)',
    '算法': r'(排序|冒泡|快排|归并|搜索|递归|动态规划|贪心|回溯|分治|DFS|BFS|二分|遍历|Dijkstra|最短路径)',
    '开发工具': r'(VS\s*Code|IntelliJ|Eclipse|Git|Docker|Maven|Gradle|npm|pip|Linux|Unix|gcc|g\+\+|clang|cmake|make)',
    '概念': r'(面向对象|函数式|并发|多线程|异步|REST|API|MVC|ORM|微服务|容器|指针|引用|内存管理|多态|继承|封装|模板|STL)',
    '数学基础': r'(线性代数|微积分|概率|统计|离散数学|图论|数论|组合数学)',
    '网络': r'(TCP|IP|HTTP|HTTPS|DNS|路由|交换机|协议栈|OSI|Socket|WebSocket)',
}

DIFFICULTY_KEYWORDS = [
    (r'(入门|基础|初级|零基础|新手|小白)', 1),
    (r'(中级|进阶|提高|深入)', 2),
    (r'(高级|精通|专家|核心|底层)', 3),
]

def extract_entities(text):
    """从文本中提取实体"""
    entities = []
    text = text if text else ''

    # 规则匹配
    for category, pattern in KEYWORD_PATTERNS.items():
        for match in re.finditer(pattern, text):
            name = match.group().strip()
            if len(name) >= 2:
                entities.append({'name': name, 'type': category})

    # nltk 增强（如果可用）
    if HAS_NLTK:
        try:
            sentences = nltk.sent_tokenize(text[:5000])
            for sent in sentences:
                words = nltk.word_tokenize(sent)
                tagged = nltk.pos_tag(words)
                chunks = nltk.ne_chunk(tagged)
                for chunk in chunks:
                    if hasattr(chunk, 'label'):
                        name = ' '.join(c[0] for c in chunk).strip()
                        if len(name) >= 2 and not any(name in e['name'] for e in entities):
                            entities.append({'name': name, 'type': chunk.label()})
        except:
            pass

    # 去重（按name）
    seen = set()
    unique = []
    for e in entities:
        key = e['name'].lower()
        if key not in seen:
            seen.add(key)
            unique.append(e)
    return unique

def extract_properties(text, title='', duration=None):
    """提取属性"""
    props = [{'key': 'source', 'value': 'B站导入'}]
    if title:
        props.insert(0, {'key': 'title', 'value': title[:100]})
    if duration and duration > 0:
        props.insert(1, {'key': 'duration_min', 'value': str(duration // 60)})

    for pattern, level in DIFFICULTY_KEYWORDS:
        if re.search(pattern, (text or '')[:500]):
            props.append({'key': 'difficulty', 'value': str(level)})
            break
    return props

def extract_relations(entities, lesson_id):
    """构建前后依赖关系"""
    relations = []
    for i in range(len(entities) - 1):
        relations.append({'from': entities[i]['name'], 'to': entities[i+1]['name'], 'type': 'PREREQUISITE'})
    return relations

def write_to_neo4j(subject_name, subject_id, entities, properties, relations, lesson_id):
    """写入 Neo4j"""
    if not HAS_NEO4J:
        return False

    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASS))
    with driver.session() as session:
        session.run("MERGE (s:Subject {id: $id}) SET s.name = $name", id=str(subject_id), name=subject_name)
        for entity in entities:
            session.run(
                "MERGE (e:Entity {name: $name}) SET e.type = $type, e.lessonId = $lid, e.subjectId = $sid",
                name=entity['name'], type=entity.get('type', 'Unknown'), lid=str(lesson_id), sid=str(subject_id))
            session.run(
                "MATCH (e:Entity {name: $ename}), (s:Subject {id: $sid}) MERGE (e)-[:BELONGS_TO]->(s)",
                ename=entity['name'], sid=str(subject_id))
        if entities:
            first = entities[0]['name']
            for prop in properties:
                session.run(
                    "MERGE (p:Property {key: $k}) SET p.value = $v WITH p MATCH (e:Entity {name: $ename}) MERGE (e)-[:HAS_PROPERTY]->(p)",
                    k=prop['key'], v=prop['value'], ename=first)
        for rel in relations:
            session.run(
                "MATCH (a:Entity {name: $from}), (b:Entity {name: $to}) MERGE (a)-[:PREREQUISITE]->(b)", **rel)
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

    neo4j_ok = False
    try:
        neo4j_ok = write_to_neo4j(args.subject_name, args.subject_id, entities, properties, relations, args.lesson_id)
    except Exception as e:
        neo4j_ok = False

    result = {
        'success': True,
        'entities': len(entities),
        'entityList': [{'name': e['name'], 'type': e['type']} for e in entities[:20]],
        'relations': len(relations),
        'properties': len(properties),
        'neo4jSynced': neo4j_ok,
    }
    if not HAS_NEO4J:
        result['neo4jWarning'] = 'neo4j-driver not installed'
    if not neo4j_ok and HAS_NEO4J:
        result['neo4jWarning'] = 'Neo4j connection failed'

    print(json.dumps(result, ensure_ascii=False, indent=2))
