# 数据库初始化检查

检查数据库脚本的完整性，确保 `sql/init.sql` 与代码中的实体类、Mapper 保持一致。

## 检查步骤

### 1. 读取初始化脚本
读取 `sql/init.sql`，列出其中定义的所有表名。

### 2. 扫描实体类
读取 `backend/src/main/java/com/iflytek/smartprep/domain/` 下所有 `*.java`，提取每个类的 `@TableName` 注解（或类名转下划线）得到期望的表名列表。

### 3. 扫描 Mapper
读取 `backend/src/main/java/com/iflytek/smartprep/mapper/` 下所有 `*Mapper.java`，确认每个 Mapper 对应的实体都有对应的表。

### 4. 交叉比对
- 列出在代码中存在但 SQL 脚本中缺失的表（会导致启动报错）
- 列出在 SQL 中存在但代码中无对应实体的表（冗余表）

### 5. 检查默认数据
确认 `sql/init.sql` 中是否包含：
- 默认用户（admin/teacher/student）
- 示例课程数据
- 知识库文档数据

## 输出
给出一份"表对齐报告"，标注缺失项和建议补充的 SQL 语句。
