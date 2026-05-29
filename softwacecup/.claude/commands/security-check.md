# 安全检查

对项目进行安全扫描，识别硬编码密钥、配置漏洞和权限缺陷。

## 检查项目

### 1. 敏感信息硬编码
扫描以下文件中的硬编码密钥：
- `backend/src/main/resources/application.yml` — 检查 `llm.api-key`、`jwt.secret`、数据库密码是否为默认值
- 所有 `.java` 文件 — 搜索 `@Value` 注入的密钥是否有明文兜底值
- `docker-compose.yml` — 检查环境变量是否暴露密钥

### 2. 前端路由守卫
读取 `frontend/src/router/index.js`，检查角色鉴权代码块是否为空（已知问题：teacher/admin 路由守卫是空注释）

### 3. 后端接口权限
检查 `backend/src/main/java/com/iflytek/smartprep/controller/` 下各 Controller：
- 是否标注了 `@RequireRole` 注解
- 敏感接口（用户管理、日志）是否对非 admin 角色做了拦截

### 4. CORS 配置
读取 `WebConfig.java`，检查是否允许了 `*` 通配来源

### 5. JWT 配置
确认 `smartprep.jwt.secret` 不是默认值 `smartprep-super-secret-key-change-in-prod-smartprep`

## 输出格式
按【高/中/低】风险分级列出问题，并给出每个问题的修复建议。
