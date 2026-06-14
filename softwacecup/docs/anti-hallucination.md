# AI 大模型防幻觉设计文档

> 项目：知域智备优教 (SmartPrep) | 日期：2026-06-15 | 版本：V2.2

---

## 一、概述

大语言模型存在"幻觉"问题——即生成看似合理但事实错误或完全虚构的内容。在教育场景中，幻觉可能导致学生学到错误知识，风险极高。本文档系统性地阐述本项目防幻觉的多层防护体系。

---

## 二、多层防护架构

```
                    用户提问
                       │
    ┌──────────────────┼──────────────────┐
    │ 第1层：输入安全过滤                  │
    │ 敏感词检测（作弊/违法/恐怖/色情/暴力）  │
    │ 命中 → 直接拒绝，不调用LLM            │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第2层：RAG 事实基础注入               │
    │ 文档解析(PDF/Word/TXT) → 切片(500字)  │
    │ → 向量化(Embedding) → Chroma语义检索  │
    │ → 检索结果注入 Prompt                 │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第3层：Prompt 工程约束                │
    │ · 角色限定（高校教学场景）             │
    │ · 引用标注要求（【参考资料X】格式）     │
    │ · 不确定性声明（"不确定就说不知道"）    │
    │ · 结构化输出约束（JSON/Markdown）      │
    │ · 温度控制（0.4，降低随机性）          │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第4层：LLM 调用                       │
    │ DeepSeek API (OpenAI 兼容)            │
    │ Temperature: 0.4 | Max Tokens: 4096  │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第5层：输出安全过滤（本次新增）         │
    │ 敏感词检测（与输入相同的 blockedWords） │
    │ 命中 → 替换为安全提示                 │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第6层：RAG 接地验证（本次新增）         │
    │ 检索到资料但LLM未标注引用 → 添加       │
    │ "⚠️ 建议核实关键信息" 安全提示         │
    └──────────────────┼──────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │ 第7层：LLM 不可用降级                  │
    │ · 返回静态模板回答                     │
    │ · 前端兜底提示                        │
    └──────────────────┼──────────────────┘
                       │
                    用户看到安全、有据可查的回答
```

---

## 三、各层详细设计

### 第 1 层：输入安全过滤

**文件**：[TutorServiceImplWithRAG.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/impl/TutorServiceImplWithRAG.java)

```java
@Value("${smartprep.safety.blocked-words}")
private String blockedWords;  // 作弊,违法,恐怖,色情,暴力

private void validateRequest(TutorAskRequest request) {
    for (String w : Arrays.stream(blockedWords.split(",")).toList())
        if (request.getQuestion().contains(w))
            throw new IllegalArgumentException("命中内容安全策略，请调整提问内容");
}
```

**覆盖范围**：仅 `TutorController`（AI 导师）。Agent 模块目前未加入输入过滤（Agent 的课程推荐/知识讲解等场景风险较低）。

---

### 第 2 层：RAG 事实基础注入

**文件**：[RAGService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/rag/service/RAGService.java) | [VectorStoreService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/rag/service/VectorStoreService.java) | [DocumentChunker.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/rag/util/DocumentChunker.java)

**文档处理流程：**

```
上传文档 (PDF/Word/TXT)
  → Apache PDFBox / POI / Tika 解析
  → 500 字切片 + 100 字重叠
  → 章节感知分割（识别"第X章"等标题）
  → 句边界分割（优先在。！？处切分）
  → Embedding API 向量化 (text-embedding-ada-002)
  → 存入 Chroma VectorDB
```

**检索与注入：**

```java
// 语义检索
List<RetrievalResult> results = ragService.retrieveRelevantDocuments(question, course, topK=3);

// 构建 RAG 上下文块，注入 Prompt
String ragContext = ragService.buildRAGContext(results);
// 输出格式：
// "以下是从知识库中检索到的相关内容："
// "【参考资料 1】[文档标题 - 章节 第N页]"
// "文档内容..."
// "---"
// "请基于以上参考资料回答用户问题，并在回答中标注引用来源。"
```

**覆盖范围（本次扩展后）**：

| 模块 | 修改前 | 修改后 |
|------|--------|--------|
| AI 导师 (Tutor) | ✅ RAG | ✅ RAG |
| 知识讲解 (Agent/knowledge) | ❌ 裸调 LLM | ✅ RAG |
| 学情诊断 (Agent/diagnosis) | ❌ 裸调 LLM | ✅ RAG |
| 路径规划 (Agent/path-planning) | ❌ 裸调 LLM | ✅ RAG |
| 课程推荐 (Agent/course-recommend) | ❌ 裸调 LLM | ❌（基于数据库课程列表，不需要RAG） |
| 虚拟人对话 (Agent/chat-stream) | ❌ 裸调 LLM | ❌（对话场景，RAG 上下文会干扰自然交流） |

---

### 第 3 层：Prompt 工程约束

#### 3.1 不确定性声明（本次新增）

**文件**：[TutorServiceImplWithRAG.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/impl/TutorServiceImplWithRAG.java) | [AgentService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/AgentService.java)

所有 System Prompt 末尾统一追加：

```
如果你不确定答案，或者参考资料中没有足够信息，请明确说"我不确定"
或"我目前没有足够的信息回答这个问题"，不要猜测或编造内容。
```

#### 3.2 引用标注要求

Tutor 的 System Prompt 明确要求：

```
请基于这些参考资料回答用户问题，并在回答中标注引用来源（使用【参考资料X】的格式）。
```

#### 3.3 温度控制（本次调整）

**文件**：[application.yml](softwacecup/backend/src/main/resources/application.yml)

```yaml
# 修改前
temperature: 0.7   # 较高，输出随机性强，增加幻觉概率

# 修改后
temperature: 0.4   # 较低，输出更确定性，减少幻觉概率
```

温度 0.4 在教育场景中平衡了准确性和表达多样性。知识图谱提取任务使用更低的 0.3。

#### 3.4 领域限定

所有 System Prompt 将 AI 角色限定为"高校智能教学平台"场景，通过角色约束防止模型输出与教学无关的编造内容。

#### 3.5 结构化输出约束

- 学情诊断 → JSON `{content, dimensions}`
- 路径规划 → JSON `{content, path: [{step, title, duration, tasks}]}`
- 自适应出题 → JSON 题目格式
- Tutor → Markdown 结构化（问题识别/核心讲解/分步学习建议/易错点/延伸资源）

---

### 第 4 层：LLM 调用

**文件**：[LLMClient.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/LLMClient.java)

- API：DeepSeek（OpenAI 兼容协议）
- 请求格式：`POST {baseUrl}/chat/completions`
- 参数：temperature=0.4, max_tokens=4096, stream=true/false
- 超时：连接 30s，读取 180s

---

### 第 5 层：输出安全过滤（本次新增）

**文件**：[TutorServiceImplWithRAG.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/impl/TutorServiceImplWithRAG.java) | [AgentService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/AgentService.java)

```java
private String filterOutput(String content, String question) {
    if (content == null || content.isBlank()) return content;
    for (String w : Arrays.stream(blockedWords.split(",")).toList()) {
        if (content.contains(w)) {
            log.warn("LLM 输出命中敏感词: {}", w);
            return "### 安全提示\n\n您的提问已收到，但系统检测到AI生成的回答中"
                   + "包含不适当内容，已被自动拦截。\n\n请换个方式提问。";
        }
    }
    return content;
}
```

**调用位置**：

| 方法 | 时机 |
|------|------|
| `TutorServiceImplWithRAG.buildAnswer()` | LLM 返回 markdown 后、构建 TutorAnswer 前 |
| `TutorServiceImplWithRAG.stream()` | LLM 返回 llmResponse 后、分块发送前 |
| `AgentService.knowledgeExplain()` | LLM 返回 content 后 |
| `AgentService.courseRecommend()` | LLM 返回 content 后 |
| `AgentService.diagnosis()` | LLM 返回 resp 后 |
| `AgentService.pathPlanning()` | LLM 返回 resp 后 |
| `AgentService.chatStreamWithCompanion()` | LLM 返回 result 后 |

---

### 第 6 层：RAG 接地验证（本次新增）

**文件**：[TutorServiceImplWithRAG.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/impl/TutorServiceImplWithRAG.java)

```java
// 检索到了资料但 LLM 未标注引用 → 标记"仅供参考"
if (retrievalResults != null && !retrievalResults.isEmpty()
    && !markdown.contains("参考资料")) {
    safetyTips.add("⚠️ AI 回答未明确标注引用来源，建议核实关键信息");
}
```

此机制不阻止回答，但会向前端返回额外安全提示，提醒学生核实信息。

---

### 第 7 层：LLM 不可用降级

**三层回退策略**：

| 层级 | 条件 | 行为 | 文件 |
|------|------|------|------|
| L1 | API 未配置或调用失败 | `LLMClient` 返回 `null` | LLMClient.java |
| L2 | L1 返回 null | Tutor 使用 `buildFallbackMarkdown()` 生成结构化模板回答；Agent 使用各模块专用 fallback | TutorServiceImplWithRAG.java, AgentService.java |
| L3 | 网络完全不通 | 前端显示"网络异常"/"服务繁忙" | AIFloatingBall.vue, AIDialogBar.vue |

---

## 四、修改文件清单

| 文件 | 修改内容 | 优先级 |
|------|----------|--------|
| [TutorServiceImplWithRAG.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/impl/TutorServiceImplWithRAG.java) | +输出过滤 `filterOutput()`；+RAG 接地验证；+不确定性声明 | 1, 3, 5 |
| [AgentService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/AgentService.java) | +输出过滤 `filterOutput()`；+RAG 注入（3个方法）；+不确定性声明；+`@Value blockedWords` | 1, 3, 4 |
| [application.yml](softwacecup/backend/src/main/resources/application.yml) | temperature 0.7 → 0.4 | 2 |
| [AdaptiveQuizService.java](softwacecup/backend/src/main/java/com/iflytek/smartprep/service/AdaptiveQuizService.java) | 已有温度 0.3，无需修改 | — |

---

## 五、文档位置建议

在你的项目设计说明文档目录中，建议在 **"三、系统关键技术"** 之后新增一个小节：

```
三、系统关键技术
（一）Spring Boot 3 后端框架
（二）Vue 3 前端开发框架
（三）大语言模型 API 集成技术
（四）检索增强生成技术（RAG）
（五）多智能体协同调度技术
（六）知识图谱构建技术
（七）AI 生成内容防幻觉技术    ← 新增，引用本文档
```

该位置的原因：
- 防幻觉技术是**系统关键技术**而非功能实现
- 与 RAG 小节紧密关联（RAG 是防幻觉的核心手段之一）
- 与 LLM API 集成技术呼应（输入/输出过滤是对 API 调用的安全包裹）
