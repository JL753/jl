"""
Import knowledge points and dependencies from MySQL into Neo4j via HTTP API.
"""
import pymysql
import requests

MYSQL = dict(host='127.0.0.1', user='root', password='root', database='smartprep', charset='utf8mb4')
NEO4J_URL = 'http://localhost:7474'
NEO4J_AUTH = ('neo4j', 'password123')

COLOR_MAP = {
    '数据结构与算法': '#10b981',
    '机器学习基础': '#8b5cf6',
    'Web全栈开发': '#3b82f6',
    '操作系统': '#f59e0b',
    '计算机网络': '#ec4899',
    '数据库原理': '#06b6d4',
    '人工智能': '#8b5cf6',
    '数学基础': '#f97316',
}

def get_color(subject_name, lesson_name):
    for k, v in COLOR_MAP.items():
        if k in (subject_name or '') or k in (lesson_name or ''):
            return v
    return '#334155'

def run_cypher(stmt, params=None):
    """Execute a single Cypher statement with parameters."""
    payload = {"statements": [{"statement": stmt, "parameters": params or {}}]}
    resp = requests.post(
        f"{NEO4J_URL}/db/neo4j/tx/commit",
        auth=NEO4J_AUTH,
        headers={"Content-Type": "application/json"},
        json=payload
    )
    if resp.status_code != 200:
        print(f"Error ({resp.status_code}): {resp.text[:400]}")
        return None
    return resp.json()

def run_batch(stmts_with_params):
    """Execute multiple parameterized statements in one transaction."""
    statements = [{"statement": s, "parameters": p or {}} for s, p in stmts_with_params]
    resp = requests.post(
        f"{NEO4J_URL}/db/neo4j/tx/commit",
        auth=NEO4J_AUTH,
        headers={"Content-Type": "application/json"},
        json={"statements": statements}
    )
    if resp.status_code != 200:
        print(f"Batch error ({resp.status_code}): {resp.text[:400]}")
        return None
    return resp.json()

def main():
    conn = pymysql.connect(**MYSQL)
    cursor = conn.cursor()

    cursor.execute("""
        SELECT kp.id, kp.name, kp.lesson_id, COALESCE(s.name, '') as subj,
               COALESCE(l.name, '') as lesson, kp.difficulty_level, COALESCE(kp.tags, '') as tags
        FROM sp_knowledge_point kp
        LEFT JOIN sp_lesson l ON l.id = kp.lesson_id
        LEFT JOIN sp_unit u ON u.id = l.unit_id
        LEFT JOIN sp_subject s ON s.id = u.subject_id
        ORDER BY kp.id
    """)
    kps = cursor.fetchall()
    print(f"Knowledge points: {len(kps)}")

    cursor.execute("SELECT kp_id, depends_on_kp_id FROM sp_kp_dependency")
    deps = cursor.fetchall()
    print(f"Dependencies: {len(deps)}")

    cursor.execute("SELECT id, name FROM sp_subject")
    subjects = cursor.fetchall()
    print(f"Subjects: {len(subjects)}")

    cursor.close()
    conn.close()

    # --- Neo4j import ---
    print("Clearing existing data...")
    run_cypher("MATCH (n) DETACH DELETE n")

    # Subject nodes
    print(f"Creating {len(subjects)} Subject nodes...")
    for sid, sname in subjects:
        run_cypher(
            "CREATE (:Subject {id: $id, name: $name})",
            {"id": str(sid), "name": sname}
        )

    # Entity nodes (batch)
    print(f"Creating {len(kps)} Entity nodes...")
    stmts = []
    for kp_id, name, lesson_id, subj_name, lesson_name, diff, tags in kps:
        color = get_color(subj_name, lesson_name)
        stmts.append((
            "CREATE (:Entity {id: $id, name: $name, type: $type, mastery: $mastery, "
            "lessonId: $lessonId, subjectId: $subjectId, color: $color, "
            "difficultyLevel: $diff, tags: $tags})",
            {"id": str(kp_id), "name": name or '', "type": "Entity",
             "mastery": 0, "lessonId": str(lesson_id), "subjectId": subj_name or '',
             "color": color, "diff": diff or 1, "tags": tags or ''}
        ))
        if len(stmts) >= 30:
            run_batch(stmts)
            stmts = []
    if stmts:
        run_batch(stmts)

    # BELONGS_TO edges
    print("Creating BELONGS_TO relationships...")
    stmts = []
    for kp_id, name, lesson_id, subj_name, lesson_name, diff, tags in kps:
        if subj_name:
            stmts.append((
                "MATCH (e:Entity {id: $eid}) MATCH (s:Subject {name: $sname}) "
                "CREATE (e)-[:BELONGS_TO]->(s)",
                {"eid": str(kp_id), "sname": subj_name}
            ))
        if len(stmts) >= 30:
            run_batch(stmts)
            stmts = []
    if stmts:
        run_batch(stmts)

    # DEPENDS_ON edges
    print(f"Creating {len(deps)} DEPENDS_ON relationships...")
    stmts = []
    for kp_id, depends_on_kp_id in deps:
        stmts.append((
            "MATCH (a:Entity {id: $a}) MATCH (b:Entity {id: $b}) CREATE (a)-[:DEPENDS_ON]->(b)",
            {"a": str(kp_id), "b": str(depends_on_kp_id)}
        ))
        if len(stmts) >= 30:
            run_batch(stmts)
            stmts = []
    if stmts:
        run_batch(stmts)

    # Verify
    print("Verifying...")
    r1 = run_cypher("MATCH (n:Entity) RETURN count(n) as cnt")
    r2 = run_cypher("MATCH ()-[r]->() RETURN count(r) as cnt")
    r3 = run_cypher("MATCH (n:Subject) RETURN count(n) as cnt")
    if r1: print(f"  Entity nodes: {r1['results'][0]['data'][0]['row'][0]}")
    if r2: print(f"  Relationships: {r2['results'][0]['data'][0]['row'][0]}")
    if r3: print(f"  Subject nodes: {r3['results'][0]['data'][0]['row'][0]}")

    print("Done.")

if __name__ == '__main__':
    main()
