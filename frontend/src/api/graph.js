import http from './http'

// ==================== 知识图谱 ====================
export const apiSubmitAnswer = (payload) => http.post('/knowledge-graph/submit-answer', payload)
export const apiKnowledgeGraphFull = () => http.get('/knowledge-graph/full')
export const apiKnowledgeGraphProgress = () => http.get('/knowledge-graph/my-progress')
export const apiKnowledgeGraphNextRecommended = () => http.get('/knowledge-graph/next-recommended')
export const apiKpExercises = (kpId) => http.get(`/knowledge-graph/exercises/${kpId}`)
export const apiPrerequisiteChain = (kpId) => http.get(`/knowledge-graph/prerequisite-chain/${kpId}`)

// ==================== Neo4j ====================
export const apiGraphNeo4j = () => http.get('/graph/neo4j')
export const apiGraphSearchResource = (nodeName, subjectName = '') => http.post('/graph/search-resource', { nodeName, subjectName })
