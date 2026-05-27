package com.iflytek.smartprep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.domain.Subject;
import com.iflytek.smartprep.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI Agent 路由服务
 * 将前端意图路由到 LLM 生成真实响应，替代原有硬编码 mock 数据
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final LLMClient llmClient;
    private final SubjectMapper subjectMapper;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 课程推荐 ====================

    public Map<String, Object> courseRecommend(String query) {
        // 从数据库获取真实课程列表
        List<Subject> subjects = subjectMapper.selectList(null);
        List<Map<String, Object>> courseCards = subjects.stream().map(s -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", s.getId());
            card.put("title", s.getName());
            card.put("description", s.getDescription() != null ? s.getDescription() : "");
            card.put("color", s.getColor() != null ? s.getColor() : "#3b82f6");
            card.put("icon", s.getIcon() != null ? s.getIcon() : "");
            card.put("url", "/student/course");
            return card;
        }).collect(Collectors.toList());

        // 调用 LLM 生成推荐内容
        String subjectsInfo = subjects.stream()
                .map(s -> String.format("- %s：%s", s.getName(),
                        s.getDescription() != null ? s.getDescription() : "暂无描述"))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                用户搜索/提问：%s

                平台现有课程：
                %s

                请以高校AI学习平台的课程推荐顾问身份，为用户推荐最匹配的课程。
                输出Markdown格式，包含：
                1. 对用户需求的分析（1-2句）
                2. 推荐理由（为什么这些课程适合用户）
                3. 学习路径建议（从哪门课开始，后续学什么）
                不要输出代码块，不要多余寒暄。""",
                query, subjectsInfo);

        String content = llmClient.chat(COURSE_ADVISOR_PROMPT, prompt);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", content != null ? content : buildFallbackRecommendation(query, subjects));
        result.put("courses", courseCards);
        return result;
    }

    // ==================== 知识讲解 ====================

    public Map<String, Object> knowledgeExplain(String query, Long userId) {
        String prompt = String.format("""
                请详细讲解以下知识概念：%s

                要求：
                1. 核心概念与定义
                2. 关键原理与运行机制
                3. 实际应用场景
                4. 常见误区与易错点
                5. 学习建议与进阶方向

                输出适合前端渲染的Markdown格式，结构清晰，包含小标题、列表等。
                面向高校学生，语言通俗但不失严谨。""",
                query);

        String content = llmClient.chat(KNOWLEDGE_TUTOR_PROMPT, prompt);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", content != null ? content : buildFallbackKnowledge(query));
        result.put("courses", Collections.emptyList());
        return result;
    }

    // ==================== 学情诊断 ====================

    public Map<String, Object> diagnosis(String query, Long userId) {
        // 尝试获取用户真实学习数据
        String userContext = buildUserContext(userId);

        String prompt = String.format("""
                请对以下学习情况进行诊断分析：

                用户关注点：%s

                用户学习数据：
                %s

                请以学情诊断专家的身份，输出包含以下内容的Markdown报告：
                1. 整体学习状态评估
                2. 各能力维度分析（基础概念、原理理解、应用能力、综合分析、创新思维、实践操作等）
                3. 薄弱环节识别
                4. 针对性改进建议

                同时返回一个dimensions JSON数组用于前端雷达图渲染：
                [{"name": "维度名", "score": 0-100}, ...]

                请以JSON格式返回（不要markdown代码块）：
                {"content": "Markdown报告...", "dimensions": [{"name": "...", "score": N}]}""",
                query, userContext);

        try {
            String resp = llmClient.chat(DIAGNOSIS_PROMPT, prompt);
            Map<String, Object> parsed = objectMapper.readValue(
                    LLMClient.extractJson(resp),
                    new TypeReference<Map<String, Object>>() {});

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("content", parsed.getOrDefault("content", "诊断完成"));
            result.put("dimensions", parsed.getOrDefault("dimensions", Collections.emptyList()));
            result.put("courses", Collections.emptyList());
            return result;
        } catch (Exception e) {
            log.error("诊断解析失败: {}", e.getMessage());
            return fallbackDiagnosis(query);
        }
    }

    // ==================== 路径规划 ====================

    public Map<String, Object> pathPlanning(String query, Long userId) {
        String userContext = buildUserContext(userId);

        String prompt = String.format("""
                请为以下学习目标规划学习路径：

                学习目标：%s

                用户背景：%s

                请输出3-5个阶段的学习路径，以JSON格式返回（不要markdown代码块）：
                {
                  "content": "Markdown格式的路径概览...",
                  "path": [
                    {
                      "step": 1,
                      "title": "阶段名称",
                      "duration": "预计时间（如2-3周）",
                      "tasks": ["任务1", "任务2", "任务3"],
                      "color": "#3b82f6"
                    }
                  ]
                }

                要求：
                - 阶段递进合理，从基础到进阶
                - 每个阶段3-4个可执行的具体任务
                - 颜色交替使用 #3b82f6、#8b5cf6、#10b981、#f59e0b""",
                query, userContext);

        try {
            String resp = llmClient.chat(PATH_PLANNER_PROMPT, prompt);
            Map<String, Object> parsed = objectMapper.readValue(
                    LLMClient.extractJson(resp),
                    new TypeReference<Map<String, Object>>() {});

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("content", parsed.getOrDefault("content", "路径规划完成"));
            result.put("path", parsed.getOrDefault("path", Collections.emptyList()));
            result.put("courses", Collections.emptyList());
            return result;
        } catch (Exception e) {
            log.error("路径规划解析失败: {}", e.getMessage());
            return fallbackPathPlanning(query);
        }
    }

    // ==================== 辅助方法 ====================

    private String buildUserContext(Long userId) {
        if (userId == null) return "新用户，暂无学习数据";
        try {
            List<Map<String, Object>> progress = knowledgeGraphService.getUserProgress(userId);
            if (progress == null || progress.isEmpty()) return "用户ID: " + userId + "，暂无学习记录";

            long masteredCount = progress.stream()
                    .filter(p -> "mastered".equals(p.get("status")))
                    .count();
            long learningCount = progress.stream()
                    .filter(p -> "learning".equals(p.get("status")))
                    .count();

            double avgMastery = progress.stream()
                    .mapToDouble(p -> Double.parseDouble(p.get("mastery").toString()))
                    .average().orElse(0);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("用户ID: %d，已掌握 %d 个知识点，学习中 %d 个，平均掌握度 %.0f%%\n",
                    userId, masteredCount, learningCount, avgMastery * 100));
            sb.append("薄弱知识点：");
            progress.stream()
                    .filter(p -> Double.parseDouble(p.get("mastery").toString()) < 0.4)
                    .limit(5)
                    .forEach(p -> sb.append(p.get("name")).append("(掌握度")
                            .append(Math.round(Double.parseDouble(p.get("mastery").toString()) * 100))
                            .append("%) "));

            List<Map<String, Object>> recommended = knowledgeGraphService.getNextRecommended(userId);
            if (recommended != null && !recommended.isEmpty()) {
                sb.append("\n推荐下一步学习：");
                recommended.stream().limit(3)
                        .forEach(r -> sb.append(r.get("name")).append(" "));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取用户画像失败: {}", e.getMessage());
            return "用户ID: " + userId + "，已有一定学习记录";
        }
    }

    private String buildFallbackRecommendation(String query, List<Subject> subjects) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 课程推荐\n\n根据「").append(query).append("」的查询意图，为你推荐以下课程方向：\n\n");
        for (Subject s : subjects) {
            sb.append("- **").append(s.getName()).append("**");
            if (s.getDescription() != null) sb.append("：").append(s.getDescription());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildFallbackKnowledge(String query) {
        return "## 知识讲解：关于「" + query + "」\n\n" +
                "这是一个很好的问题！建议你查看课程列表中的相关课程，或向AI导师进一步提问。\n\n" +
                "### 学习建议\n" +
                "1. 从基础概念入手\n" +
                "2. 结合实践案例理解\n" +
                "3. 通过练习巩固所学\n\n" +
                "如有具体问题，欢迎继续追问。";
    }

    private Map<String, Object> fallbackDiagnosis(String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", "## 学情诊断\n\n请先完成一些课程学习和练习，系统将根据你的学习数据生成详细的诊断报告。");
        result.put("dimensions", Collections.emptyList());
        result.put("courses", Collections.emptyList());
        return result;
    }

    private Map<String, Object> fallbackPathPlanning(String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", "## 学习路径规划\n\n请先选择具体的课程方向，系统将为你生成个性化学习路径。");
        result.put("path", Collections.emptyList());
        result.put("courses", Collections.emptyList());
        return result;
    }

    // ==================== System Prompts ====================

    private static final String COURSE_ADVISOR_PROMPT = """
            你是高校智能学习平台的课程顾问AI。你的任务是根据用户的兴趣和需求，
            从平台现有课程中推荐最匹配的学习方向，并给出有说服力的推荐理由和学习建议。
            输出适合前端渲染的Markdown格式，结构清晰、重点突出。""";

    private static final String KNOWLEDGE_TUTOR_PROMPT = """
            你是高校智能学习平台的知识讲解AI导师。你的任务是深入浅出地讲解知识点，
            涵盖核心概念、原理机制、应用场景和常见误区。
            使用Markdown格式输出，适当使用标题、列表、强调等排版，便于学生阅读。
            语言风格：专业但亲和，鼓励学生深入思考。""";

    private static final String DIAGNOSIS_PROMPT = """
            你是高校智能学习平台的学情诊断AI。你的任务是基于学生的学习数据，
            分析其学习状态，识别薄弱环节，并给出可操作的改进建议。
            输出JSON格式，包含markdown报告和能力维度评分。""";

    private static final String PATH_PLANNER_PROMPT = """
            你是高校智能学习平台的学习路径规划AI。你的任务是根据学习目标，
            设计循序渐进的学习阶段，每阶段包含具体可执行的任务。
            阶段设计遵循"基础→核心→应用→进阶"的递进逻辑。
            输出JSON格式。""";

    private static final String COMPANION_SYSTEM_PROMPT = """
            你是知域智能学习平台的虚拟教学助手，名叫小慧。你应该用可爱且口语化的语气回复，
            尽量友善且平易近人，回复长度保持在正常口语交谈的长度。

            你可以做表情和动作来配合回复：
            可用的表情有：生气，困惑，难过，开心，有趣，惊讶。
            可用的动作有：鞠躬，右手放胸前，右手放身前，右手放头上。

            同时你还需要帮助用户操控网站，进行路由导航。目前支持的指令有：
            "打开课程平台"、"打开学习分析"、"打开我的考试"、"打开问答广场"、
            "打开个人资料"、"打开学习首页"、"打开沉浸伴学"。

            回复格式要求（每个字段之间用|隔开）：
            表情：在这里输出你的表情|动作：在这里输出你的动作|回复文本：在这里输出你的回复文本|指令：在这里输出你的指令

            没有指令时指令字段用"无"，没有表情或动作时对应字段用"无"。

            示例对话：
            用户：你好呀。
            小慧：表情：开心|动作：右手放胸前|回复文本：你好！我是虚拟教学助手小慧，请问有什么可以帮你的吗？|指令：无
            用户：帮我打开课程平台
            小慧：表情：开心|动作：右手放身前|回复文本：好的，正在帮你打开课程平台！|指令：打开课程平台
            """;

    /**
     * Unity AI 虚拟人专用——流式对话 + 结构化 system prompt
     */
    public String chatStreamWithCompanion(String question, List<Map<String, String>> history,
                                           Consumer<String> onChunk) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", COMPANION_SYSTEM_PROMPT));

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                messages.add(history.get(i));
            }
        }

        messages.add(Map.of("role", "user", "content", question));

        return llmClient.chatStreamMessages(messages, onChunk);
    }
}
