// RAG 命中率测试 — Chroma v2 + n-gram TF-IDF embedding
const http = require('http');
const CHROMA = 'http://localhost:8000';
const COLL_NAME = 'rag_test_' + Date.now();
let COLL_ID = null;

const DOCS = [
  { id:'doc1', title:'动态规划基础', course:'算法', content:'动态规划是一种通过把原问题分解为相对简单的子问题来求解复杂问题的方法。核心要素包括最优子结构和重叠子问题。最优子结构指问题的最优解包含子问题的最优解。重叠子问题指子问题被重复计算多次。经典应用：背包问题、最长公共子序列、编辑距离。' },
  { id:'doc2', title:'二叉树遍历', course:'数据结构', content:'二叉树的遍历方式主要有三种：先序遍历根左右、中序遍历左根右、后序遍历左右根。对于二叉搜索树BST，中序遍历结果是有序的。层序遍历使用队列实现，按层级从上到下从左到右访问节点。遍历时间复杂度均为On。' },
  { id:'doc3', title:'MySQL优化', course:'数据库', content:'MySQL查询优化包括：合理使用B+树索引和哈希索引、避免SELECT星号、使用EXPLAIN分析查询计划、优化JOIN顺序、使用连接池、配置查询缓存。慢查询日志是定位性能瓶颈的关键工具。索引覆盖可避免回表查询。' },
  { id:'doc4', title:'RESTful API设计', course:'Web开发', content:'RESTful API设计原则：使用HTTP方法表示操作GET查询POST创建PUT更新DELETE删除、资源URL使用名词复数、使用HTTP状态码表示结果、支持分页过滤、版本管理。无状态是REST的核心约束，每个请求包含所有必要信息。' },
  { id:'doc5', title:'梯度下降算法', course:'AI', content:'梯度下降是深度学习最基础的优化算法。变体：批量梯度下降BGD、随机梯度下降SGD、小批量梯度下降。改进算法：Momentum动量法、AdaGrad自适应学习率、RMSProp、Adam。学习率对收敛速度影响很大，过大会震荡过小会收敛慢。' },
  { id:'doc6', title:'HTTPS加密', course:'网络', content:'HTTPS通过TLS/SSL协议实现加密通信。机制：非对称加密RSA/ECDHE交换密钥、对称加密AES传输数据、数字证书CA签名验证身份。TLS握手：ClientHello、ServerHello、证书验证、密钥交换。443端口。' },
  { id:'doc7', title:'Java并发', course:'Java', content:'Java多线程并发核心技术：synchronized互斥同步、ReentrantLock可重入锁、volatile保证可见性、ThreadPoolExecutor线程池管理、CountDownLatch/CyclicBarrier协调工具、ConcurrentHashMap并发集合。避免死锁的方法：按序加锁、超时机制。' },
  { id:'doc8', title:'栈与队列', course:'数据结构', content:'栈Stack是后进先出LIFO的数据结构，操作在栈顶。应用：函数调用栈、括号匹配、撤销Ctrl+Z。队列Queue是先进先出FIFO，队尾入队队头出队。应用：BFS宽度优先搜索、任务调度、消息队列。双端队列Deque两端都可操作。' },
  { id:'doc9', title:'微服务架构', course:'架构', content:'微服务架构将单体应用拆分为多个独立小服务。优点：独立部署、技术栈灵活、故障隔离、可扩展。缺点：分布式复杂、数据一致性挑战、运维成本高。基础设施：服务注册发现、API网关、配置中心、链路追踪、熔断降级。' },
  { id:'doc10', title:'Python列表推导', course:'Python', content:'Python列表推导式是简洁的列表创建方式。语法：表达式for变量in可迭代对象if条件。比传统for循环更高效。支持嵌套推导、字典推导、集合推导。注意复杂推导会影响可读性，建议不超过两层嵌套。' },
  { id:'doc11', title:'排序算法', course:'算法', content:'常见排序算法及复杂度：冒泡On2、选择On2、插入On2、快速Onlogn、归并Onlogn、堆Onlogn。快排通常最快但不稳定，归并稳定但需额外On空间。计数排序On+k适用于整数范围小的情况。' },
  { id:'doc12', title:'神经网络', course:'AI', content:'人工神经网络模拟生物神经元。组件：输入层、隐藏层、输出层、激活函数ReLU/Sigmoid/Tanh、损失函数交叉熵/MSE、反向传播。CNN用于图像、RNN/LSTM用于序列、Transformer用于NLP。过拟合用Dropout和正则化。' },
];

const QUERIES = [
  { q:'什么是动态规划', exp:'doc1' },
  { q:'二叉树的遍历方式', exp:'doc2' },
  { q:'如何优化MySQL查询', exp:'doc3' },
  { q:'RESTful API设计原则', exp:'doc4' },
  { q:'梯度下降算法详解', exp:'doc5' },
  { q:'HTTPS加密原理', exp:'doc6' },
  { q:'Java多线程并发编程', exp:'doc7' },
  { q:'栈和队列的区别', exp:'doc8' },
  { q:'微服务架构优缺点', exp:'doc9' },
  { q:'Python列表推导式', exp:'doc10' },
  { q:'常见排序算法对比', exp:'doc11' },
  { q:'神经网络基本结构', exp:'doc12' },
];

// ==================== HTTP ====================
function api(method, path, body) {
  return new Promise((resolve, reject) => {
    const d = body ? JSON.stringify(body) : null;
    const u = new URL(path, CHROMA);
    const opts = { hostname:u.hostname, port:u.port, path:u.pathname+u.search, method,
      headers:{'Content-Type':'application/json'} };
    if(d) opts.headers['Content-Length'] = Buffer.byteLength(d);
    const req = http.request(opts, res => { let c=''; res.on('data',x=>c+=x); res.on('end',()=>{ try{resolve(JSON.parse(c))}catch(e){resolve({raw:c})} }); });
    req.on('error', reject);
    if(d) req.write(d);
    req.end();
  });
}

// ==================== n-gram TF-IDF embedding ====================
const DIM = 256;
let vocab = null;

function buildVocab(texts) {
  const freq = new Map();
  for (const text of texts) {
    const chars = text.replace(/[^一-鿿]/g, '');
    for (let n = 1; n <= 3; n++) {
      for (let i = 0; i <= chars.length - n; i++) {
        const gram = chars.substring(i, i + n);
        freq.set(gram, (freq.get(gram) || 0) + 1);
      }
    }
  }
  return [...freq.entries()].sort((a,b)=>b[1]-a[1]).slice(0,DIM).map(([g])=>g);
}

function embed(text) {
  const chars = text.replace(/[^一-鿿]/g, '');
  const vec = new Array(vocab.length).fill(0);
  for (let i = 0; i < vocab.length; i++) {
    let count = 0, pos = 0;
    const gram = vocab[i];
    while ((pos = chars.indexOf(gram, pos)) !== -1) { count++; pos++; }
    if (count > 0) vec[i] = count / Math.max(1, chars.length);
  }
  const norm = Math.sqrt(vec.reduce((s,v)=>s+v*v,0)) || 1;
  return vec.map(v => parseFloat((v/norm).toFixed(6)));
}

// ==================== 主流程 ====================
async function main() {
  console.log('═══════════════════════════════════════════');
  console.log('  RAG 命中率测试');
  console.log(`  配置: n-gram TF-IDF ${DIM}d + Chroma cosine`);
  console.log('  嵌入方式: 本地计算，无需 API Key');
  console.log('═══════════════════════════════════════════\n');

  // 1. Chroma heartbeat
  try { await api('GET','/api/v2/heartbeat'); console.log('✓ Chroma 运行中\n'); }
  catch(e) { console.log('✗ Chroma 未运行'); process.exit(1); }

  // 2. 创建新集合（唯一名称避免冲突）
  const colPath = '/api/v2/tenants/default_tenant/databases/default_database/collections';
  const coll = await api('POST', colPath, { name: COLL_NAME, metadata: { 'hnsw:space': 'cosine' } });
  if (!coll || !coll.id) { console.log('✗ 创建集合失败:', JSON.stringify(coll).substring(0,200)); process.exit(1); }
  COLL_ID = coll.id;
  const BASE = colPath + '/' + COLL_ID;
  console.log(`✓ 集合已创建: ${COLL_NAME}\n`);

  // 3. 生成 embedding 并上传
  const allTexts = DOCS.map(d => d.content).concat(QUERIES.map(q => q.q));
  vocab = buildVocab(allTexts);
  console.log(`  词汇表: ${vocab.length} n-grams`);
  const docEmbs = DOCS.map(d => embed(d.content));
  console.log(`  文档向量: ${DOCS.length} 个 (dim=${docEmbs[0].length})`);

  const addRes = await api('POST', BASE + '/add', {
    ids: DOCS.map(d => d.id),
    embeddings: docEmbs,
    documents: DOCS.map(d => d.content),
    metadatas: DOCS.map(d => ({ title: d.title, course: d.course }))
  });
  if (addRes.error) { console.log(`✗ 上传失败: ${addRes.error} ${addRes.message}`); process.exit(1); }
  console.log(`✓ 已上传 ${DOCS.length} 篇文档\n`);

  // 4. 查询测试
  console.log('─── 向量检索结果 ───\n');
  const queryEmbs = QUERIES.map(q => embed(q.q));
  let hits = 0, dists = [], details = [];

  for (let i = 0; i < QUERIES.length; i++) {
    const t = QUERIES[i];
    process.stdout.write(`  "${t.q}" → `);
    try {
      const resp = await api('POST', BASE + '/query', {
        query_embeddings: [queryEmbs[i]],
        n_results: 3
      });
      if (resp.error) { console.log(`✗ API错误: ${resp.message}`); continue; }
      const ids = resp.ids?.[0] || [];
      const d = resp.distances?.[0] || [];
      const hit = ids[0] === t.exp;
      if (hit) { hits++; console.log(`✓ 命中 (top1:${ids[0]}, d=${(d[0]||0).toFixed(4)})`); }
      else { console.log(`✗ (got:[${ids.slice(0,3).join(',')}], exp:${t.exp})`); }
      details.push({ q: t.q, hit, top1: ids[0], top3: ids.slice(0,3), dist: d[0] });
      if (d[0] != null) dists.push(d[0]);
    } catch(e) { console.log(`✗ ${e.message}`); }
  }

  const rate = (hits / QUERIES.length * 100).toFixed(1);
  const avgDist = dists.length ? (dists.reduce((a,b)=>a+b,0)/dists.length).toFixed(4) : 'N/A';

  // 5. 也测试 FTS（全文搜索）对比
  console.log(`\n─── FTS 全文搜索对比 ───\n`);
  let ftsHits = 0;
  for (const t of QUERIES) {
    process.stdout.write(`  "${t.q}" → `);
    try {
      const resp = await api('POST', BASE + '/query', {
        query_texts: [t.q],
        n_results: 3
      });
      if (resp.error) { console.log(`✗ ${resp.message}`); continue; }
      const ids = resp.ids?.[0] || [];
      const hit = ids[0] === t.exp;
      if (hit) { ftsHits++; console.log(`✓ 命中 (top1:${ids[0]})`); }
      else { console.log(`✗ (got:[${ids.slice(0,3).join(',')}], exp:${t.exp})`); }
    } catch(e) { console.log(`✗ ${e.message}`); }
  }
  const ftsRate = (ftsHits / QUERIES.length * 100).toFixed(1);

  console.log(`\n═══════════════════════════════════════════`);
  console.log(`  向量检索 (TF-IDF): ${hits}/${QUERIES.length} = ${rate}%  |  平均距离: ${avgDist}`);
  console.log(`  全文搜索 (FTS):    ${ftsHits}/${QUERIES.length} = ${ftsRate}%`);
  console.log(`═══════════════════════════════════════════\n`);

  console.log('>>> RESULT_JSON <<<');
  console.log(JSON.stringify({
    date: new Date().toISOString().slice(0,10),
    embedMethod: 'n-gram TF-IDF 256d',
    total: QUERIES.length,
    vectorHits: hits, vectorRate: parseFloat(rate), avgCosineDist: avgDist === 'N/A' ? null : parseFloat(avgDist),
    ftsHits, ftsRate: parseFloat(ftsRate),
    details
  }, null, 2));
}
main().catch(e => { console.error(e); process.exit(1); });
