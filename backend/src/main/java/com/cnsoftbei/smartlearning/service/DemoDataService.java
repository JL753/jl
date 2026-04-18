package com.cnsoftbei.smartlearning.service;

import com.cnsoftbei.smartlearning.api.AgentGenerateRequest;
import com.cnsoftbei.smartlearning.api.LoginResult;
import com.cnsoftbei.smartlearning.api.ResourceCard;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoDataService {

    public LoginResult login(String username, String role) {
        String r = role == null || role.isBlank() ? "teacher" : role;
        return new LoginResult(
                "demo-token-" + r,
                r,
                "teacher".equals(r) ? "王老师" : "student".equals(r) ? "邢同学" : "系统管理员",
                "https://api.dicebear.com/9.x/notionists/svg?seed=" + r,
                menus(r),
                Map.of("major", "计算机科学与技术", "course", "人工智能导论", "stage", "2023级本科", "goal", "强化机器学习基础并完成课程项目")
        );
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "teacherStats", List.of(stat("运行课程", "12", "+2"), stat("今日活跃学生", "286", "+18%"), stat("资源生成次数", "1248", "+36%"), stat("辅导满意度", "96.5%", "+3.2%")),
                "studentStats", List.of(stat("本周学习时长", "18.5h", "+4.2h"), stat("知识掌握度", "78%", "+6%"), stat("推荐资源完成数", "23", "+8"), stat("能力成长指数", "89", "+12")),
                "chart", List.of(
                        Map.of("name", "机器学习", "mastery", 68, "interest", 88, "practice", 56),
                        Map.of("name", "深度学习", "mastery", 52, "interest", 93, "practice", 44),
                        Map.of("name", "计算机视觉", "mastery", 61, "interest", 82, "practice", 59),
                        Map.of("name", "自然语言处理", "mastery", 72, "interest", 90, "practice", 63),
                        Map.of("name", "强化学习", "mastery", 35, "interest", 70, "practice", 22)
                ),
                "warnings", List.of("3名学生在反向传播推导上连续答错", "2个班级实验报告提交延迟率超过20%", "第4章资源打开率偏低，建议优化封面与摘要")
        );
    }

    public Map<String, Object> buildProfile(String conversation) {
        return new LinkedHashMap<>(Map.of(
                "major", "计算机科学与技术",
                "target", "期末90+并完成AI课程项目",
                "knowledgeBase", "Python和数据结构基础良好，概率论偏弱",
                "cognitiveStyle", "偏好图解化、案例驱动、分步骤讲解",
                "learningHabit", "晚间学习效率更高，每次专注40分钟",
                "weakness", "反向传播、损失函数选择、模型调参",
                "resourcePreference", "视频 + 图文总结 + 代码案例",
                "mistakePattern", "链式求导与矩阵维度变化易错",
                "motivation", "对生成式AI和智能体应用兴趣高",
                "evaluation", "综合学习画像评分 78/100"
        ));
    }

    public Map<String, Object> generateResources(AgentGenerateRequest request) {
        String topic = txt(request.topic(), "大模型辅助学习");
        List<Map<String, Object>> agents = List.of(
                agent("学习画像分析智能体", "提取专业、目标、错因、偏好、节奏线索"),
                agent("课程拆解智能体", "重组知识点依赖关系，输出讲解目录"),
                agent("练习设计智能体", "生成基础题、情境题、编程题"),
                agent("多模态内容智能体", "生成微课脚本、图解卡片、思维导图"),
                agent("学习规划智能体", "形成7天路径、推送策略与评估节点")
        );
        List<ResourceCard> resources = List.of(
                new ResourceCard("讲解文档", topic + "·精讲手册", "结构化讲解", "概念解析、公式推导、例题拆解与误区提示。", "中等", "20分钟"),
                new ResourceCard("思维导图", topic + "·知识图谱", "一张图串联核心概念", "覆盖前置知识、核心模块、典型应用。", "基础", "8分钟"),
                new ResourceCard("练习题库", topic + "·分层练习", "基础题 / 提高题 / 综合题", "15题并附答案解析与错因提示。", "分层", "25分钟"),
                new ResourceCard("视频脚本", topic + "·微课脚本", "2-3分钟短视频方案", "含旁白文案、镜头建议、字幕关键词。", "中等", "3分钟"),
                new ResourceCard("代码实操", topic + "·实验案例", "Notebook与工程任务", "提供步骤拆解、关键代码与结果分析。", "提高", "45分钟"),
                new ResourceCard("拓展阅读", topic + "·延伸资源", "论文、博客、课程链接推荐", "按入门 / 进阶 / 前沿三级推荐。", "弹性", "15分钟")
        );
        return Map.of(
                "agents", agents,
                "resources", resources,
                "resourceBoards", List.of(
                        Map.of("label", "讲解文档", "count", 3, "tag", "可导出 Word/PDF"),
                        Map.of("label", "图解卡片", "count", 5, "tag", "适合碎片化学习"),
                        Map.of("label", "题目集合", "count", 15, "tag", "覆盖单选/简答/编程"),
                        Map.of("label", "多模态脚本", "count", 2, "tag", "支持视频/动画生成"),
                        Map.of("label", "案例任务", "count", 1, "tag", "含评价标准")
                ),
                "progress", List.of("需求解析", "画像匹配", "资源编排", "多模态生成", "质量审查", "打包完成"),
                "citations", List.of("人工智能导论课程知识库", "高校课程实验说明书", "开源教学资源索引（已标记协议）"),
                "safety", Map.of(
                        "antiHallucination", List.of("知识库命中优先", "生成后事实核验提示", "输出标注适用范围"),
                        "contentFilter", List.of("敏感词拦截", "违规请求拒答", "资源链接白名单校验"),
                        "generationTrace", List.of("已记录提示词版本", "已记录资源来源", "已记录生成时间")
                )
        );
    }

    public Map<String, Object> pathPlan() {
        return Map.of(
                "weeks", List.of(
                        day("Day1", "前置概念梳理", "阅读精讲手册", "观看5分钟概念视频", "完成5道基础题"),
                        day("Day2", "关键公式理解", "学习图解卡片", "完成推导练习", "向智能辅导提问"),
                        day("Day3", "代码实操入门", "运行示例项目", "修改关键参数", "提交实验结论"),
                        day("Day4", "综合应用训练", "完成提升题", "观看案例视频", "输出知识总结"),
                        day("Day5", "错题归因复盘", "查看错题画像", "再做针对训练", "对比掌握度变化"),
                        day("Day6", "项目化迁移", "完成小项目任务", "生成阶段汇报PPT", "接受智能评估"),
                        day("Day7", "阶段检测与调整", "在线测验", "获取学习诊断", "动态调整下周路径")
                ),
                "recommendations", List.of("建议先学损失函数与梯度下降后再进入反向传播", "资源推送以视频+代码案例为主", "保持晚间20:00-22:00为高效学习窗口"),
                "pushCards", List.of(
                        Map.of("type", "视频", "title", "3分钟理解梯度下降", "reason", "匹配你的视觉化偏好"),
                        Map.of("type", "文档", "title", "反向传播推导图解", "reason", "针对链式求导薄弱点"),
                        Map.of("type", "实操", "title", "手写二分类实验", "reason", "巩固模型训练闭环")
                )
        );
    }

    public Map<String, Object> tutor(String question) {
        return Map.of(
                "answer", "人工智能是研究如何让机器模拟人类智能行为的学科。建议先理解‘数据—模型—训练—评估’四步闭环，再进入具体算法。",
                "markdown", "## 分步骤理解\n1. 明确任务目标\n2. 准备训练数据\n3. 选择模型并训练\n4. 用指标评估效果\n\n> 你当前更适合先看图解，再做一个最小代码实验。",
                "visualGuide", List.of(
                        Map.of("title", "学习流程图", "content", "任务目标 → 数据准备 → 模型训练 → 评估反馈"),
                        Map.of("title", "当前短板定位", "content", "模型训练阶段：链式求导与梯度更新"),
                        Map.of("title", "建议资源形式", "content", "图解卡 + 3分钟视频 + 最小代码实验")
                ),
                "videoScript", Map.of("scene1", "解释AI定义与学科范围", "scene2", "展示训练流程图", "scene3", "用垃圾邮件分类举例"),
                "question", txt(question, "什么是人工智能？"),
                "responseTime", "1.2s（流式输出首包）"
        );
    }

    public Map<String, Object> assessment() {
        return Map.of(
                "radar", Map.of("知识理解", 82, "实操能力", 74, "自主规划", 88, "反思能力", 69, "学习韧性", 85, "创新迁移", 71),
                "behaviors", List.of(v("资源浏览完成率", "91%"), v("练习正确率", "76%"), v("实操任务完成度", "68%"), v("问答互动频次", "32次/周")),
                "suggestions", List.of("增加反向传播专题练习题 8 道", "连续 3 天推送代码调参案例", "将长文档拆分为 3 份微课形式"),
                "timeline", List.of(Map.of("stage", "学习前", "score", 62), Map.of("stage", "第1周", "score", 71), Map.of("stage", "第2周", "score", 78), Map.of("stage", "第3周", "score", 84))
        );
    }

    public Map<String, Object> teacherWorkbench() {
        return Map.of(
                "courses", List.of(
                        Map.of("name", "人工智能导论", "students", 120, "resources", 86, "completion", "84%"),
                        Map.of("name", "机器学习基础", "students", 96, "resources", 64, "completion", "79%"),
                        Map.of("name", "Python数据分析", "students", 132, "resources", 102, "completion", "88%")
                ),
                "questionBank", List.of(
                        Map.of("title", "感知机原理", "type", "单选", "difficulty", "基础", "score", 5),
                        Map.of("title", "反向传播推导", "type", "简答", "difficulty", "提高", "score", 15),
                        Map.of("title", "线性回归实验", "type", "编程", "difficulty", "综合", "score", 20)
                ),
                "todoList", List.of("审核今日新增资源 42 条", "查看班级练习题错误分布", "发布下周复习路径任务")
        );
    }

    public Map<String, Object> examCenter() {
        return Map.of(
                "summary", List.of(stat("试卷总数", "16", "+3"), stat("今日开考", "4", "+1"), stat("客观题自动批改", "91%", "+7%"), stat("监考异常提醒", "2", "-1")),
                "papers", List.of(
                        Map.of("name", "人工智能导论期中测试", "duration", "60分钟", "questionCount", 25, "status", "进行中"),
                        Map.of("name", "机器学习基础周测", "duration", "45分钟", "questionCount", 18, "status", "待发布"),
                        Map.of("name", "神经网络专题训练", "duration", "30分钟", "questionCount", 12, "status", "已结束")
                ),
                "questionTypes", List.of(Map.of("label", "单选题", "count", 46), Map.of("label", "判断题", "count", 18), Map.of("label", "简答题", "count", 12), Map.of("label", "编程题", "count", 8))
        );
    }

    public Map<String, Object> resourceCenter() {
        return Map.of(
                "collections", List.of(
                        Map.of("title", "人工智能导论初始知识库", "type", "文档集", "size", "28篇", "source", "自建课程知识库"),
                        Map.of("title", "机器学习代码实验集", "type", "实操案例", "size", "12套", "source", "实验指导书"),
                        Map.of("title", "生成式AI延伸阅读", "type", "拓展资源", "size", "18篇", "source", "公开资料索引")
                ),
                "agreements", List.of("Element Plus（MIT）", "ECharts（Apache-2.0）", "Vue 3（MIT）", "Spring Boot（Apache-2.0）")
        );
    }

    public Map<String, Object> bigScreen() {
        return Map.of(
                "mapStats", List.of(Map.of("name", "华东", "value", 860), Map.of("name", "华北", "value", 720), Map.of("name", "华南", "value", 690), Map.of("name", "西南", "value", 510)),
                "collegeStats", List.of(Map.of("name", "计算机学院", "rate", 92), Map.of("name", "电子信息学院", "rate", 87), Map.of("name", "自动化学院", "rate", 84), Map.of("name", "软件学院", "rate", 95)),
                "riskStats", List.of(Map.of("name", "课程掉队风险", "value", 11), Map.of("name", "实验拖延风险", "value", 8), Map.of("name", "考试焦虑风险", "value", 5)),
                "summary", Map.of("students", 15240, "generatedResources", 2268, "pathCompletion", 21.31, "weeklyVideos", 426)
        );
    }

    public Map<String, Object> systemSummary() {
        return Map.of(
                "title", "A3-基于大模型的个性化资源生成与学习多智能体系统",
                "course", "人工智能导论",
                "knowledgeBaseScale", "1门完整课程 + 28篇知识文档 + 12套实验案例",
                "competitionCoverage", List.of("对话式学习画像构建（10维）", "多智能体协同生成6类资源", "学习路径规划与精准推送", "智能辅导与Markdown答疑", "学习效果评估与策略调整"),
                "nonFunctional", List.of("卡片化多模态展示与生成进度追踪", "防幻觉提醒与内容安全过滤", "开源协议依赖清单", "首包流式输出体验"),
                "updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private Map<String, Object> stat(String label, String value, String trend) { return Map.of("label", label, "value", value, "trend", trend); }
    private Map<String, Object> v(String label, String value) { return Map.of("label", label, "value", value); }
    private Map<String, Object> agent(String name, String result) { return Map.of("name", name, "result", result, "status", "completed"); }
    private Map<String, Object> day(String d, String t, String a, String b, String c) { return Map.of("day", d, "title", t, "tasks", List.of(a, b, c)); }
    private String txt(String text, String fallback) { return text == null || text.isBlank() ? fallback : text; }

    private List<Map<String, Object>> menus(String role) {
        if ("teacher".equals(role)) return List.of(Map.of("name", "教师首页", "path", "/teacher"), Map.of("name", "智能体工作台", "path", "/teacher/agent"), Map.of("name", "题库与考试", "path", "/teacher/exam"), Map.of("name", "数据中台", "path", "/bigscreen"));
        if ("admin".equals(role)) return List.of(Map.of("name", "平台总览", "path", "/teacher"), Map.of("name", "数据中台", "path", "/bigscreen"));
        return List.of(Map.of("name", "学生首页", "path", "/student"), Map.of("name", "学习助手", "path", "/student/assistant"), Map.of("name", "学习路径", "path", "/student/path"), Map.of("name", "能力评估", "path", "/student/assessment"));
    }
}
