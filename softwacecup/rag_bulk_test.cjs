// RAG 批量命中率测试 — 10^4 规模
// n-gram TF-IDF 256d, Chroma v2 cosine
const http = require('http');
const CHROMA = 'http://localhost:8000';
const DIM = 256;
const DOC_COUNT = 10000;
const QUERY_COUNT = 100;

// ==================== 测试数据生成 ====================
const TOPICS = [
  { name:'动态规划', terms:['最优子结构','重叠子问题','状态转移方程','记忆化搜索','自顶向下','自底向上','背包问题','最长公共子序列','编辑距离','区间DP','树形DP','状压DP'] },
  { name:'二叉树', terms:['先序遍历','中序遍历','后序遍历','层序遍历','二叉搜索树','平衡二叉树','红黑树','AVL树','哈夫曼树','线索二叉树','完全二叉树','满二叉树'] },
  { name:'排序算法', terms:['冒泡排序','快速排序','归并排序','堆排序','插入排序','选择排序','希尔排序','计数排序','桶排序','基数排序','时间复杂度','空间复杂度'] },
  { name:'MySQL数据库', terms:['B+树索引','哈希索引','聚簇索引','覆盖索引','最左前缀','EXPLAIN','慢查询日志','连接池','事务隔离','MVCC','InnoDB','锁机制'] },
  { name:'RESTful API', terms:['HTTP方法','GET查询','POST创建','PUT更新','DELETE删除','状态码','无状态','资源URI','HATEOAS','版本管理','分页','JWT认证'] },
  { name:'深度学习', terms:['梯度下降','反向传播','激活函数','ReLU','损失函数','卷积层','池化层','全连接层','Dropout','BatchNorm','学习率','优化器'] },
  { name:'HTTPS', terms:['TLS握手','SSL证书','非对称加密','对称加密','RSA算法','AES算法','CA认证','数字签名','公钥','私钥','证书链','HTTPS'] },
  { name:'Java并发', terms:['synchronized','ReentrantLock','volatile','ThreadPool','CountDownLatch','CyclicBarrier','Semaphore','ConcurrentHashMap','CAS','AQS','死锁','线程安全'] },
  { name:'栈队列', terms:['LIFO','FIFO','栈顶','队头','队尾','双端队列','优先队列','循环队列','表达式求值','括号匹配','BFS','DFS'] },
  { name:'微服务', terms:['服务拆分','API网关','注册发现','配置中心','链路追踪','熔断降级','负载均衡','容器化','Docker','K8s','消息队列','分布式事务'] },
  { name:'Python', terms:['列表推导','装饰器','生成器','上下文管理','异步IO','多线程','GIL','虚拟环境','pip','NumPy','Pandas','Django'] },
  { name:'神经网络', terms:['CNN','RNN','LSTM','Transformer','Attention','BERT','GPT','残差网络','嵌入层','Softmax','Dropout','BatchNorm'] },
  { name:'操作系统', terms:['进程','线程','内存管理','虚拟内存','文件系统','死锁','调度算法','页表','中断','系统调用','内核态','用户态'] },
  { name:'计算机网络', terms:['TCP','UDP','IP','HTTP','DNS','路由','子网掩码','OSI模型','三次握手','四次挥手','拥塞控制','滑动窗口'] },
  { name:'Linux', terms:['Shell','文件权限','管道','进程管理','cron','systemd','iptables','SSH','Vim','Grep','Sed','Awk'] },
  { name:'机器学习', terms:['监督学习','无监督学习','决策树','SVM','KNN','KMeans','随机森林','XGBoost','过拟合','正则化','交叉验证','特征工程'] },
  { name:'前端开发', terms:['HTML','CSS','JavaScript','React','Vue','组件化','虚拟DOM','Webpack','Babel','TypeScript','响应式','Flexbox'] },
  { name:'Git', terms:['commit','branch','merge','rebase','pull','push','clone','stash','cherry-pick','tag','remote','冲突解决'] },
  { name:'Redis', terms:['缓存','持久化','RDB','AOF','哨兵','集群','主从复制','过期策略','LRU','管道','事务','发布订阅'] },
  { name:'软件工程', terms:['敏捷开发','Scrum','设计模式','SOLID','TDD','CI/CD','代码审查','重构','UML','需求分析','项目管理','DevOps'] },
];

// 生成随机中文段落
function genDoc(topic, docIdx) {
  const terms = topic.terms;
  const shuffled = [...terms].sort(() => Math.random() - 0.5);
  const sentences = [];
  sentences.push(`${topic.name}是计算机科学中的重要概念。`);
  for (let i = 0; i < 3; i++) {
    const t = shuffled[i % shuffled.length];
    sentences.push(`${t}是${topic.name}的核心组成部分，在实际应用中广泛使用。`);
  }
  sentences.push(`深入理解${topic.name}需要掌握${shuffled.slice(0,3).join('、')}等关键知识点。`);
  sentences.push(`${topic.name}的典型应用场景包括${shuffled.slice(4,7).join('、')}等方面。`);
  sentences.push(`学习${topic.name}时需要注意${shuffled[7]}和${shuffled[8]}的区别与联系。`);
  return sentences.join('');
}

function genQuery(topic, qIdx) {
  const terms = topic.terms;
  const patterns = [
    `什么是${topic.name}的${terms[qIdx % terms.length]}`,
    `${topic.name}中${terms[(qIdx+1) % terms.length]}和${terms[(qIdx+2) % terms.length]}的区别`,
    `如何理解${topic.name}的核心原理`,
    `请解释${topic.name}的应用场景`,
    `${topic.name}的学习方法有哪些`,
  ];
  return patterns[qIdx % patterns.length];
}

// ==================== HTTP ====================
function api(method, path, body) {
  return new Promise((resolve, reject) => {
    const d = body ? JSON.stringify(body) : null;
    const u = new URL(path, CHROMA);
    const opts = { hostname:u.hostname, port:u.port, path:u.pathname+u.search, method,
      headers:{'Content-Type':'application/json'}, timeout:30000 };
    if(d) opts.headers['Content-Length'] = Buffer.byteLength(d);
    const req = http.request(opts, res => { let c=''; res.on('data',x=>c+=x); res.on('end',()=>{ try{resolve(JSON.parse(c))}catch(e){resolve({raw:c})} }); });
    req.on('error', reject);
    req.on('timeout', ()=>{req.destroy();reject(new Error('timeout'));});
    if(d) req.write(d);
    req.end();
  });
}

// ==================== n-gram TF-IDF ====================
function buildVocab(texts, dim) {
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
  return [...freq.entries()].sort((a,b)=>b[1]-a[1]).slice(0,dim).map(([g])=>g);
}

function embed(text, vocab) {
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

// ==================== 指标计算 ====================
function calcMetrics(results) {
  const N = results.length;
  let top1Hits = 0, top3Hits = 0, mrrSum = 0;
  const dists = [];
  for (const r of results) {
    if (r.top1Hit) top1Hits++;
    if (r.top3Hit) top3Hits++;
    if (r.rank > 0) mrrSum += 1.0 / r.rank;
    if (r.dist != null) dists.push(r.dist);
  }
  const avgDist = dists.length ? dists.reduce((a,b)=>a+b,0)/dists.length : 0;
  return {
    top1Rate: (top1Hits / N * 100).toFixed(2),
    top3Rate: (top3Hits / N * 100).toFixed(2),
    mrr: (mrrSum / N).toFixed(4),
    avgDist: avgDist.toFixed(4),
    top1Hits, top3Hits, total: N
  };
}

// ==================== 主流程 ====================
async function main() {
  console.log('══════════════════════════════════════════════');
  console.log(`  RAG 批量命中率测试 — 10^4 规模`);
  console.log(`  文档数: ${DOC_COUNT} | 查询数: ${QUERY_COUNT}`);
  console.log(`  Embedding: n-gram TF-IDF ${DIM}d + Chroma cosine`);
  console.log('══════════════════════════════════════════════\n');

  // 1. Chroma
  try { await api('GET','/api/v2/heartbeat'); console.log('✓ Chroma 运行中\n'); }
  catch(e) { console.log('✗ Chroma 未运行'); process.exit(1); }

  // 2. 生成数据
  console.log(`  生成 ${DOC_COUNT} 篇文档...`);
  const docs = [];
  const queries = [];
  const queryTopics = [];
  for (let i = 0; i < DOC_COUNT; i++) {
    const topic = TOPICS[i % TOPICS.length];
    docs.push({ id: `d${i}`, topic: topic.name, content: genDoc(topic, i), topicIdx: i % TOPICS.length });
  }
  for (let i = 0; i < QUERY_COUNT; i++) {
    const topic = TOPICS[i % TOPICS.length];
    queries.push({ q: genQuery(topic, i), topicIdx: i % TOPICS.length });
    queryTopics.push(topic.name);
  }

  // 3. 构建词表
  const allTexts = docs.map(d => d.content).concat(queries.map(q => q.q));
  const vocab = buildVocab(allTexts, DIM);
  console.log(`  词汇表: ${vocab.length} n-grams`);

  // 4. 生成文档 embedding 并分批上传
  console.log(`  生成文档 embedding...`);
  const t0 = Date.now();
  const BATCH = 500;
  const colPath = '/api/v2/tenants/default_tenant/databases/default_database/collections';
  const coll = await api('POST', colPath, { name: 'rag_bulk_' + Date.now(), metadata: { 'hnsw:space': 'cosine' } });
  const BASE = colPath + '/' + coll.id;
  console.log(`  集合: ${coll.name}\n`);

  for (let i = 0; i < DOC_COUNT; i += BATCH) {
    const batch = docs.slice(i, i + BATCH);
    const embs = batch.map(d => embed(d.content, vocab));
    await api('POST', BASE + '/add', {
      ids: batch.map(d => d.id),
      embeddings: embs,
      metadatas: batch.map(d => ({ topic: d.topic }))
    });
    if ((i / BATCH) % 4 === 0) process.stdout.write(`  上传进度: ${i}/${DOC_COUNT}\r`);
  }
  console.log(`  上传完成: ${DOC_COUNT} 篇 (${Date.now()-t0}ms)\n`);

  // 5. 查询测试
  console.log(`  执行 ${QUERY_COUNT} 次查询...`);
  const queryEmbs = queries.map(q => embed(q.q, vocab));
  const results = [];
  for (let i = 0; i < QUERY_COUNT; i++) {
    const resp = await api('POST', BASE + '/query', {
      query_embeddings: [queryEmbs[i]],
      n_results: 5
    });
    const ids = resp.ids?.[0] || [];
    const distances = resp.distances?.[0] || [];
    const expTopicIdx = queries[i].topicIdx;
    let rank = -1, top1Hit = false, top3Hit = false;
    for (let j = 0; j < ids.length; j++) {
      const docIdx = parseInt(ids[j].substring(1));
      if (docIdx % TOPICS.length === expTopicIdx) {
        if (rank < 0) rank = j + 1;
        if (j === 0) top1Hit = true;
        if (j < 3) top3Hit = true;
      }
    }
    results.push({ top1Hit, top3Hit, rank, dist: distances[0] });
    if (i % 20 === 19) process.stdout.write(`  查询进度: ${i+1}/${QUERY_COUNT}\r`);
  }
  console.log(`  查询完成: ${QUERY_COUNT} 次\n`);

  // 6. 指标
  const m = calcMetrics(results);

  // 7. 不同维度对比
  console.log('─── 不同维度对比 ───');
  for (const dim of [128, 256, 512]) {
    const v = buildVocab(allTexts, dim);
    let h = 0;
    for (let i = 0; i < Math.min(50, QUERY_COUNT); i++) {
      const resp = await api('POST', BASE + '/query', {
        query_embeddings: [embed(queries[i].q, v)],
        n_results: 1
      });
      const ids = resp.ids?.[0] || [];
      const docIdx = parseInt(ids[0]?.substring(1) || '-1');
      if (docIdx >= 0 && docIdx % TOPICS.length === queries[i].topicIdx) h++;
    }
    console.log(`  ${dim}d → Top-1: ${(h/50*100).toFixed(1)}% (50 queries)`);
  }

  // 8. 输出
  console.log(`\n══════════════════════════════════════════════`);
  console.log(`  规模: ${DOC_COUNT} docs × ${QUERY_COUNT} queries`);
  console.log(`  Top-1 命中率: ${m.top1Rate}%`);
  console.log(`  Top-3 命中率: ${m.top3Rate}%`);
  console.log(`  MRR: ${m.mrr}`);
  console.log(`  平均余弦距离: ${m.avgDist}`);
  console.log(`  总耗时: ${((Date.now()-t0)/1000).toFixed(1)}s`);
  console.log(`══════════════════════════════════════════════\n`);

  console.log('>>> RESULT_JSON <<<');
  console.log(JSON.stringify({
    date: new Date().toISOString().slice(0,10),
    method: `n-gram TF-IDF ${DIM}d`,
    docCount: DOC_COUNT, queryCount: QUERY_COUNT, topics: TOPICS.length,
    top1Rate: parseFloat(m.top1Rate), top3Rate: parseFloat(m.top3Rate),
    mrr: parseFloat(m.mrr), avgCosineDist: parseFloat(m.avgDist),
    dimComparison: { '128d': null, '256d': parseFloat(m.top1Rate), '512d': null }
  }, null, 2));
}
main().catch(e => { console.error(e); process.exit(1); });
