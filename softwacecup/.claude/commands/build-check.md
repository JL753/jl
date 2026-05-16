# 前端构建检查

在提交或演示前，检查前端工程的构建就绪状态，识别潜在的运行时问题。

## 检查步骤

### 1. 路由完整性
读取 `frontend/src/router/index.js`，对每个路由的 `component: () => import(...)` 路径，确认对应的 `.vue` 文件在 `frontend/src/views/` 下确实存在。标出死链路由。

### 2. API 调用一致性
读取 `frontend/src/api/index.js`，提取所有接口路径（如 `/profile/dialogue`），与后端 Controller 中的 `@RequestMapping` 路径做交叉比对，找出前端调用但后端未定义的接口。

### 3. 环境配置
检查 `frontend/vite.config.js`，确认：
- proxy 配置中后端地址是否为 `http://localhost:8080`
- 生产构建的 base 路径是否正确

### 4. 依赖审查
读取 `frontend/package.json`，检查是否有已知的安全漏洞版本或过时的主要依赖。

### 5. 构建产物检查
检查 `frontend/dist/` 目录是否存在，列出主要 chunk 文件大小，判断是否存在过大的包（单文件超过 500KB 建议拆包）。

## 输出
给出"构建就绪度报告"：✅ 通过 / ⚠️ 警告 / ❌ 需修复，并附具体修复建议。
