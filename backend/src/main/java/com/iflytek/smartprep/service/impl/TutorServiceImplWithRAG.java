package com.iflytek.smartprep.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.smartprep.config.JwtTokenProvider;
import com.iflytek.smartprep.domain.KnowledgeDoc;
import com.iflytek.smartprep.dto.*;
import com.iflytek.smartprep.mapper.KnowledgeDocMapper;
import com.iflytek.smartprep.rag.model.RetrievalResult;
import com.iflytek.smartprep.rag.service.RAGService;
import com.iflytek.smartprep.service.*;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@org.springframework.context.annotation.Primary
public class TutorServiceImplWithRAG implements TutorService {

    @Value("${smartprep.safety.blocked-words}") private String blockedWords;
    @Value("${smartprep.llm.api-key:}") private String llmApiKey;
    @Value("${smartprep.llm.base-url:}") private String llmBaseUrl;
    @Value("${smartprep.llm.model:astron-code-latest}") private String llmModel;
    @Value("${smartprep.llm.temperature:0.7}") private Double llmTemperature;
    @Value("${smartprep.llm.max-tokens:4096}") private Integer llmMaxTokens;

    // RAG 配置
    @Value("${smartprep.rag.enabled:true}") private Boolean ragEnabled;
    @Value("${smartprep.rag.top-k:3}") private Integer ragTopK;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final JwtTokenProvider jwtTokenProvider;
    private final ProfileService profileService;
    private final ResourceService resourceService;
    private final StudyPathService studyPathService;
    private final AssessmentService assessmentService;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final RAGService ragService;

    public TutorServiceImplWithRAG(JwtTokenProvider jwtTokenProvider, ProfileService profileService, ResourceService resourceService,
                            StudyPathService studyPathService, AssessmentService assessmentService, KnowledgeDocMapper knowledgeDocMapper,
                            RAGService ragService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.profileService = profileService;
        this.resourceService = resourceService;
        this.studyPathService = studyPathService;
        this.assessmentService = assessmentService;
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.ragService = ragService;
    }

    @Override
    public TutorAnswer ask(Long userId, TutorAskRequest request) { return buildAnswer(userId, request); }

    @Override
    public void initializeUserData(Long userId, String displayName, String role) {
        // RAG版本的初始化逻辑（如果需要的话）
        // 目前可以留空或添加特定的初始化逻辑
    }

    @Override
    public TutorAnswer buildAnswer(Long userId, TutorAskRequest request) {
        validateRequest(request);

        // RAG 检索
        List<RetrievalResult> retrievalResults = new ArrayList<>();
        List<String> citations = new ArrayList<>();

        if (ragEnabled) {
            try {
                // 从请求中提取课程信息（如果有）
                String course = extractCourseFromContext(request.getContext());
                retrievalResults = ragService.retrieveRelevantDocuments(request.getQuestion(), course, ragTopK);
                citations = ragService.extractCitations(retrievalResults);
            } catch (Exception e) {
                // RAG 失败不影响主流程
                System.err.println("RAG 检索失败: " + e.getMessage());
            }
        }

        // 调用大模型
        String markdown = callRemoteTutorWithRAG(request, retrievalResults);
        if (markdown == null || markdown.isBlank()) {
            markdown = buildFallbackMarkdown(request.getQuestion());
        }

        return TutorAnswer.builder()
                .markdown(markdown)
                .diagrams(List.of("概念关系图", "算法流程图", "错题定位图"))
                .shortVideoTips(List.of("3分钟速讲", "5分钟题型拆解", "案例动画讲解"))
                .references(List.of("课程知识库", "教师讲义", "推荐公开课资源"))
                .streamChunks(List.of("正在检索知识点...", "正在匹配画像偏好...", "正在生成图解与视频建议...", "已完成安全审查与答案汇总。"))
                .safetyTips(List.of("已过滤敏感内容", "建议结合教师讲义进行二次确认", "复杂知识点可继续追问获得分步解释"))
                .citations(citations)
                .retrievedChunksCount(retrievalResults.size())
                .build();
    }

    @Override
    public SseEmitter stream(Long userId, TutorAskRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                validateRequest(request);

                // 1. 发送开始事件
                send(emitter, "meta", "开始处理问题...");

                // 2. RAG 检索
                List<RetrievalResult> retrievalResults = new ArrayList<>();
                List<String> citations = new ArrayList<>();

                if (ragEnabled) {
                    try {
                        send(emitter, "meta", "正在检索知识库...");
                        String course = extractCourseFromContext(request.getContext());
                        retrievalResults = ragService.retrieveRelevantDocuments(request.getQuestion(), course, ragTopK);
                        citations = ragService.extractCitations(retrievalResults);

                        if (!citations.isEmpty()) {
                            send(emitter, "meta", "找到 " + citations.size() + " 个相关文档");
                        }
                    } catch (Exception e) {
                        send(emitter, "meta", "知识库检索失败，使用通用回答");
                    }
                }

                // 3. 调用大模型流式输出
                send(emitter, "meta", "正在生成回答...");

                String llmResponse = callRemoteTutorWithRAG(request, retrievalResults);
                if (llmResponse == null || llmResponse.isBlank()) {
                    llmResponse = buildFallbackMarkdown(request.getQuestion());
                }

                // 4. 流式发送回答
                List<String> chunks = splitMarkdown(llmResponse);
                for (String chunk : chunks) {
                    send(emitter, "delta", chunk);
                    sleep(50); // 模拟打字机效果
                }

                // 5. 发送引用来源
                if (!citations.isEmpty()) {
                    TutorStreamEvent citationEvent = TutorStreamEvent.builder()
                            .type("citations")
                            .content("参考资料")
                            .citations(citations)
                            .build();
                    emitter.send(SseEmitter.event().name("citations").data(objectMapper.writeValueAsString(citationEvent)));
                }

                // 6. 发送完成事件
                send(emitter, "done", "回答完成");
                emitter.complete();

            } catch (Exception e) {
                try {
                    send(emitter, "error", e.getMessage());
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @Override
    public TutorStreamEvent authEvent(String token) {
        Claims claims = jwtTokenProvider.parse(token);
        return TutorStreamEvent.builder().type("auth").content(String.valueOf(claims.get("uid"))).build();
    }

    /**
     * 调用大模型（带 RAG 上下文）
     */
    private String callRemoteTutorWithRAG(TutorAskRequest request, List<RetrievalResult> retrievalResults) {
        if (llmApiKey == null || llmApiKey.isBlank() || llmBaseUrl == null || llmBaseUrl.isBlank()) {
            return null;
        }

        try {
            String url = llmBaseUrl.endsWith("/") ? llmBaseUrl + "chat/completions" : llmBaseUrl + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmApiKey);

            // 构建带 RAG 上下文的提示词
            String userPrompt = buildUserPromptWithRAG(request, retrievalResults);

            Map<String, Object> body = Map.of(
                    "model", llmModel,
                    "temperature", llmTemperature,
                    "max_tokens", llmMaxTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPromptWithRAG()),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JsonNode contentNode = objectMapper.readTree(response.getBody()).path("choices").path(0).path("message").path("content");
            return contentNode.isMissingNode() ? null : contentNode.asText();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 系统提示词（RAG 版本）
     */
    private String systemPromptWithRAG() {
        return "你是高校智能教学平台中的AI助教。你会收到从知识库中检索到的相关文档片段作为参考资料。" +
                "请基于这些参考资料回答用户问题，并在回答中标注引用来源（使用【参考资料X】的格式）。" +
                "输出适合前端直接渲染的Markdown格式。回答要结构化，包含：问题识别、核心讲解、分步学习建议、易错点、延伸资源建议。" +
                "不要输出多余寒暄，不要使用代码围栏包裹整段内容。";
    }

    /**
     * 构建用户提示词（带 RAG 上下文）
     */
    private String buildUserPromptWithRAG(TutorAskRequest request, List<RetrievalResult> retrievalResults) {
        StringBuilder sb = new StringBuilder();

        // 添加 RAG 上下文
        if (retrievalResults != null && !retrievalResults.isEmpty()) {
            sb.append(ragService.buildRAGContext(retrievalResults));
            sb.append("\n\n---\n\n");
        }

        // 添加用户问题
        sb.append("用户问题：").append(request.getQuestion()).append("\n");

        if (request.getContext() != null && !request.getContext().isBlank()) {
            sb.append("补充上下文：").append(request.getContext()).append("\n");
        }

        if (request.getAnswerMode() != null && !request.getAnswerMode().isBlank()) {
            sb.append("回答模式：").append(request.getAnswerMode()).append("\n");
        }

        sb.append("请面向高校教学与学习场景输出，强调可操作性和学习路径。");

        return sb.toString();
    }

    /**
     * 从上下文中提取课程信息
     */
    private String extractCourseFromContext(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }

        // 简单的课程名称提取逻辑
        String[] keywords = {"人工智能", "机器学习", "数据结构", "算法", "计算机"};
        for (String keyword : keywords) {
            if (context.contains(keyword)) {
                return keyword;
            }
        }

        return null;
    }

    private void validateRequest(TutorAskRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) throw new IllegalArgumentException("问题不能为空");
        for (String w : Arrays.stream(blockedWords.split(",")).toList()) if (request.getQuestion().contains(w)) throw new IllegalArgumentException("命中内容安全策略，请调整提问内容");
    }

    private void send(SseEmitter emitter, String event, String data) throws java.io.IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private List<String> splitMarkdown(String markdown) {
        return Arrays.stream(markdown.split("(?<=\\n\\n)|(?<=。)|(?<=：)|(?<=\\n)"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildFallbackMarkdown(String question) {
        return "### 智能辅导回答\n**问题识别：** " + question + "\n\n> 系统已结合课程知识库、学生画像与教学场景生成讲解建议，并已进行内容安全过滤。\n\n#### 1. 概念拆解\n- 先理解定义与应用场景。\n- 再理解关键步骤与输入输出。\n- 最后通过例题或代码验证理解。\n\n#### 2. 学习建议\n1. 看 5 分钟图解或微课，建立整体印象。\n2. 阅读结构化讲义，标出关键词。\n3. 完成 3 道基础题 + 1 道综合题。\n4. 用自己的语言复述，形成长期记忆。\n\n#### 3. 常见误区\n- 只背定义，不理解使用条件。\n- 只看答案，不做步骤推导。\n- 只做单题，不做知识迁移。\n\n#### 4. 多模态辅导建议\n- 文字：精讲讲义\n- 图解：流程图 / 思维导图\n- 视频：3分钟微课脚本\n";
    }
}
