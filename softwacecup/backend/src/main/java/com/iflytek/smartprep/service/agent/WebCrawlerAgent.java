package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebCrawlerAgent implements Agent {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebCrawlerAgent.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String WIKI_API = "https://zh.wikipedia.org/api/rest_v1/page/summary/";

    @Override
    public String name() {
        return "互联网知识爬取智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String topic = request.getTopic();
        List<String> sources = new ArrayList<>();
        StringBuilder md = new StringBuilder();

        md.append("# ").append(topic).append(" - 互联网知识汇编\n\n");
        md.append("> 本内容由互联网知识爬取智能体自动采集并整理，来源包括 Wikipedia、知乎专题等公开教育资源。\n\n");

        // 1. 爬取 Wikipedia 中文摘要
        WikiResult wiki = fetchWikipedia(topic);
        if (wiki != null) {
            md.append("## 📖 百科定义（Wikipedia）\n\n");
            md.append(wiki.extract).append("\n\n");
            if (wiki.url != null) sources.add(wiki.url);
        }

        // 2. 构建学科延伸资源链接
        List<WikiResult> related = fetchRelatedTopics(topic, request.getCourse());
        if (!related.isEmpty()) {
            md.append("## 🔗 相关知识点\n\n");
            for (WikiResult r : related) {
                md.append("- **").append(r.title).append("**");
                if (r.extract != null && !r.extract.isBlank()) {
                    md.append("：").append(r.extract.length() > 80 ? r.extract.substring(0, 80) + "..." : r.extract);
                }
                md.append("\n");
                if (r.url != null) sources.add(r.url);
            }
            md.append("\n");
        }

        // 3. 推荐公开学习资源
        md.append("## 🎯 推荐学习资源\n\n");
        String encoded = URLEncoder.encode(topic, StandardCharsets.UTF_8);
        String zhihuUrl = "https://www.zhihu.com/search?type=content&q=" + encoded;
        String bilibiliUrl = "https://search.bilibili.com/all?keyword=" + encoded;
        String csdnUrl = "https://so.csdn.net/so/search?q=" + encoded;
        md.append("| 来源 | 链接 | 类型 |\n");
        md.append("|------|------|------|\n");
        md.append("| 知乎 | [查看").append(topic).append("相关讨论](").append(zhihuUrl).append(") | 问答社区 |\n");
        md.append("| Bilibili | [查看").append(topic).append("教学视频](").append(bilibiliUrl).append(") | 视频教程 |\n");
        md.append("| CSDN | [查看").append(topic).append("技术博客](").append(csdnUrl).append(") | 技术文章 |\n");
        md.append("| 百度学术 | [查看").append(topic).append("学术论文](https://xueshu.baidu.com/s?wd=").append(encoded).append(") | 学术资料 |\n\n");

        sources.add(zhihuUrl);
        sources.add(bilibiliUrl);
        sources.add(csdnUrl);

        // 4. 学习建议
        md.append("## 💡 爬取智能体学习建议\n\n");
        md.append("基于互联网公开资源分析，建议按以下顺序学习 **").append(topic).append("**：\n\n");
        md.append("1. 先通过百科定义建立基本概念\n");
        md.append("2. 在知乎查看高质量问答，了解实际应用场景\n");
        md.append("3. 在 Bilibili 观看视频教程，强化直觉理解\n");
        md.append("4. 在 CSDN 阅读实战代码，完成动手实践\n");
        md.append("5. 通过百度学术查阅论文，深入研究原理\n\n");
        md.append("> ⚠️ 注意：互联网资源质量参差不齐，建议以课程教材为主要学习依据，互联网资源作为补充参考。\n");

        return AgentResult.builder()
                .type("web-crawl")
                .title(topic + " 互联网知识汇编")
                .markdown(md.toString())
                .links(sources)
                .confidence(78)
                .agentName(name())
                .build();
    }

    private WikiResult fetchWikipedia(String topic) {
        try {
            String encoded = URLEncoder.encode(topic, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WIKI_API + encoded))
                    .header("User-Agent", "SmartPrepBot/1.0 (educational-platform)")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            String body = resp.body();
            String extract = extractJsonField(body, "extract");
            String title = extractJsonField(body, "title");

            if (extract == null || extract.isBlank()) return null;

            // 限制摘要长度
            if (extract.length() > 500) extract = extract.substring(0, 500) + "...";

            WikiResult r = new WikiResult();
            r.title = title != null ? title : topic;
            r.extract = extract;
            r.url = "https://zh.wikipedia.org/wiki/" + URLEncoder.encode(topic, StandardCharsets.UTF_8);
            return r;
        } catch (Exception e) {
            log.debug("Wikipedia 爬取失败 [{}]: {}", topic, e.getMessage());
            return null;
        }
    }

    private List<WikiResult> fetchRelatedTopics(String topic, String course) {
        List<WikiResult> results = new ArrayList<>();
        // 根据课程和主题构建相关词
        List<String> related = buildRelatedKeywords(topic, course);
        for (String kw : related) {
            WikiResult r = fetchWikipedia(kw);
            if (r != null) {
                results.add(r);
                if (results.size() >= 3) break;
            }
        }
        return results;
    }

    private List<String> buildRelatedKeywords(String topic, String course) {
        List<String> keywords = new ArrayList<>();
        // 为常见 AI/CS 课题生成相关词
        if (topic.contains("机器学习") || (course != null && course.contains("人工智能"))) {
            keywords.add("深度学习");
            keywords.add("神经网络");
            keywords.add("监督学习");
        } else if (topic.contains("算法")) {
            keywords.add("时间复杂度");
            keywords.add("数据结构");
            keywords.add("动态规划");
        } else if (topic.contains("神经网络")) {
            keywords.add("反向传播");
            keywords.add("激活函数");
            keywords.add("卷积神经网络");
        } else {
            // 通用：尝试添加"原理"和"应用"变体
            keywords.add(topic + "原理");
            keywords.add(topic + "应用");
        }
        return keywords;
    }

    // 简单 JSON 字段提取（避免引入额外依赖）
    private String extractJsonField(String json, String field) {
        try {
            Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1).replace("\\n", "\n").replace("\\\"", "\"");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static class WikiResult {
        String title;
        String extract;
        String url;
    }
}
