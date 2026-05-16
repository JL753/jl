# 生成 API 接口文档摘要

扫描后端所有 Controller，生成一份结构化的接口清单，便于比赛答辩时展示和前后端联调时查阅。

## 扫描范围

读取 `backend/src/main/java/com/iflytek/smartprep/controller/` 下所有 `*Controller.java` 文件。

## 输出格式

按模块分组，每个接口一行，格式为：

```
[HTTP方法] /api路径  →  功能说明  [需要角色: xxx]
```

示例：
```
### 认证模块 (AuthController)
POST   /auth/login          →  用户登录，返回 JWT Token        [公开]
POST   /auth/register       →  注册新用户                      [公开]
GET    /auth/me             →  获取当前登录用户信息             [已登录]
PUT    /auth/me             →  更新个人信息                     [已登录]

### 学习画像 (ProfileController)
POST   /profile/dialogue    →  对话式构建/更新学习画像         [student]
GET    /profile/mine        →  获取当前学生的画像数据           [student]
```

## 额外统计

在文档末尾输出：
- 接口总数
- 各模块接口数量分布
- 标注了角色权限的接口比例（用于评估鉴权覆盖率）
