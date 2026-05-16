# 启动开发环境

检查并启动本项目的后端和前端开发服务器。

## 执行步骤

1. 检查 `backend/src/main/resources/application.yml` 确认 MySQL 和 Redis 连接配置
2. 提示用户确认 MySQL（127.0.0.1:3306）和 Redis（127.0.0.1:6379）是否已启动
3. 给出启动后端的命令：
   ```
   cd backend && mvn spring-boot:run
   ```
4. 给出启动前端的命令（新终端）：
   ```
   cd frontend && npm install && npm run dev
   ```
5. 告知访问地址：
   - 前端门户：http://localhost:5173/portal
   - 后端 API：http://localhost:8080
   - 默认账号：admin/teacher/student，密码均为 123456

如果用户想用 Docker 一键启动，给出：`docker compose up -d`
