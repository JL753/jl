# 六维能力真实数据方案

**目标**：补齐练习题作答+知识点掌握度+学习时长数据采集，六维图用精准公式计算

---

## 新增表
- `sp_exercise_attempt` — 练习题作答记录
- `sp_study_duration` — 每日学习时长

## 新增端点
- `POST /api/exercise/submit` — 提交答案
- `POST /api/study/heartbeat` — 学习时长心跳

## 前端采集
- CourseView 30秒心跳 → study/heartbeat
- 练一练提交 → exercise/submit
- 我已掌握 → completeLesson + 结束学习时段

## 六维公式
1. 知识广度 = 覆盖学科/总学科 × 覆盖单元/学科单元
2. 知识深度 = avg(UserKpMastery.mastery)
3. 解题能力 = sum(correct×difficulty)/sum(difficulty)
4. 活跃度 = streak×10 + min(日均分钟/5, 50)
5. 知识迁移 = 跨lesson练习题正确率
6. 韧性 = 错题复习率×50 + 重试正确率×50
