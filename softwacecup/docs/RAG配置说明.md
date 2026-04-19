# RAG 配置说明文档

## application.yml 配置项

请在 `backend/src/main/resources/application.yml` 中添加以下配置：

```yaml
smartprep:
  # 安全配置
  safety:
    blocked-words: "政治,暴力,色情"

  # 大模型配置
  llm:
    api-key: "your-api-key-here"
    base-url: "https://api.openai.com/v1"
    model: "gpt-3.5-turbo"
    temperature: 0.7
    max-tokens: 4096

  # RAG 配置
  rag:
    enabled: true                                    # 是否启用 RAG
    top-k: 3                                         # 检索返回的文档块数量

    # Chroma 向量数据库配置
    chroma:
      url: "http://localhost:8000"                   # Chroma 服务地址
      collection: "smartprep_knowledge"              # 集合名称

    # 文本向量化配置
    embedding:
      api-key: "your-embedding-api-key"              # 向量化 API Key
      base-url: "https://api.openai.com/v1"          # 向量化 API 地址
      model: "text-embedding-ada-002"                # 向量化模型

    # 文档分块配置
    chunking:
      chunk-size: 500                                # 每块字符数
      overlap-size: 100                              # 重叠字符数
```

## 完整配置示例

```yaml
server:
  port: 8080

spring:
  application:
    name: smartprep-backend

  datasource:
    url: jdbc:mysql://localhost:3306/smartprep?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB                            # 文档上传大小限制
      max-request-size: 50MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

smartprep:
  jwt:
    secret: "your-jwt-secret-key-at-least-256-bits-long"
    expiration: 86400000

  safety:
    blocked-words: "政治,暴力,色情,赌博"

  llm:
    api-key: "${LLM_API_KEY:}"
    base-url: "${LLM_BASE_URL:https://api.openai.com/v1}"
    model: "${LLM_MODEL:gpt-3.5-turbo}"
    temperature: 0.7
    max-tokens: 4096

  rag:
    enabled: true
    top-k: 3

    chroma:
      url: "${CHROMA_URL:http://localhost:8000}"
      collection: "smartprep_knowledge"

    embedding:
      api-key: "${EMBEDDING_API_KEY:}"
      base-url: "${EMBEDDING_BASE_URL:https://api.openai.com/v1}"
      model: "${EMBEDDING_MODEL:text-embedding-ada-002}"

    chunking:
      chunk-size: 500
      overlap-size: 100
```

## 环境变量配置（推荐）

为了安全起见，建议使用环境变量配置敏感信息：

```bash
# Linux/Mac
export LLM_API_KEY="your-api-key"
export LLM_BASE_URL="https://api.openai.com/v1"
export EMBEDDING_API_KEY="your-embedding-api-key"
export CHROMA_URL="http://localhost:8000"

# Windows
set LLM_API_KEY=your-api-key
set LLM_BASE_URL=https://api.openai.com/v1
set EMBEDDING_API_KEY=your-embedding-api-key
set CHROMA_URL=http://localhost:8000
```

## 使用国内大模型的配置示例

### 1. 智谱 GLM (推荐)

```yaml
smartprep:
  llm:
    api-key: "your-zhipu-api-key"
    base-url: "https://open.bigmodel.cn/api/paas/v4"
    model: "glm-4"
    temperature: 0.7
    max-tokens: 4096

  rag:
    embedding:
      api-key: "your-zhipu-api-key"
      base-url: "https://open.bigmodel.cn/api/paas/v4"
      model: "embedding-2"
```

### 2. 通义千问

```yaml
smartprep:
  llm:
    api-key: "your-dashscope-api-key"
    base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
    model: "qwen-turbo"
    temperature: 0.7
    max-tokens: 4096

  rag:
    embedding:
      api-key: "your-dashscope-api-key"
      base-url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
      model: "text-embedding-v1"
```

### 3. DeepSeek

```yaml
smartprep:
  llm:
    api-key: "your-deepseek-api-key"
    base-url: "https://api.deepseek.com/v1"
    model: "deepseek-chat"
    temperature: 0.7
    max-tokens: 4096

  rag:
    embedding:
      api-key: "your-deepseek-api-key"
      base-url: "https://api.deepseek.com/v1"
      model: "deepseek-embedding"
```

## Chroma 向量数据库部署

### 方式1：Docker 部署（推荐）

```bash
# 拉取镜像
docker pull chromadb/chroma

# 启动容器
docker run -d \
  --name chroma \
  -p 8000:8000 \
  -v chroma-data:/chroma/chroma \
  chromadb/chroma

# 验证
curl http://localhost:8000/api/v1/heartbeat
```

### 方式2：Python 部署

```bash
# 安装
pip install chromadb

# 启动服务
chroma run --host 0.0.0.0 --port 8000
```

### 方式3：使用内嵌模式（开发环境）

如果不想单独部署 Chroma，可以使用 FAISS 作为替代（需要修改代码）。

## 配置验证

启动后端后，访问以下接口验证配置：

```bash
# 检查 RAG 状态
curl http://localhost:8080/api/rag/status

# 预期响应
{
  "code": 200,
  "message": "success",
  "data": {
    "ragEnabled": true,
    "chromaUrl": "http://localhost:8000",
    "collectionName": "smartprep_knowledge",
    "embeddingModel": "text-embedding-ada-002",
    "topK": 3
  }
}
```

## 常见问题

### 1. Chroma 连接失败

**错误**: `Connection refused: localhost/127.0.0.1:8000`

**解决**:
- 检查 Chroma 是否启动: `docker ps | grep chroma`
- 检查端口是否被占用: `netstat -an | grep 8000`
- 修改配置中的 `chroma.url`

### 2. 向量化 API 调用失败

**错误**: `401 Unauthorized` 或 `Invalid API Key`

**解决**:
- 检查 `embedding.api-key` 是否正确
- 检查 API Key 是否有权限调用 embedding 接口
- 检查 `embedding.base-url` 是否正确

### 3. 文档上传失败

**错误**: `Maximum upload size exceeded`

**解决**:
- 增大 `spring.servlet.multipart.max-file-size`
- 检查文件格式是否支持（PDF/Word/TXT）

### 4. RAG 检索无结果

**原因**: 向量数据库中没有文档

**解决**:
```bash
# 上传测试文档
curl -X POST http://localhost:8080/api/rag/upload \
  -H "Authorization: Bearer your-token" \
  -F "file=@test.pdf" \
  -F "course=人工智能导论" \
  -F "tag=基础"
```

## 性能优化建议

1. **批量上传**: 一次上传多个文档，减少网络开销
2. **调整 chunk-size**: 根据文档类型调整分块大小
   - 技术文档: 500-800 字符
   - 教材: 800-1200 字符
   - 论文: 1000-1500 字符
3. **调整 top-k**: 根据问题复杂度调整检索数量
   - 简单问题: 1-2 个
   - 复杂问题: 3-5 个
4. **使用缓存**: 对常见问题启用 Redis 缓存

## 下一步

配置完成后，请参考 [RAG使用文档.md](./RAG使用文档.md) 了解如何使用 RAG 功能。
