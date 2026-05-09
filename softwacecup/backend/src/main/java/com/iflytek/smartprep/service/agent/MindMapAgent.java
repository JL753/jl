package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MindMapAgent implements Agent {
    @Override
    public String name() {
        return "知识图谱智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String md = "```mermaid\ngraph TD\nA[" + request.getTopic() + "] --> B[概念定义]\nA --> C[核心方法]\nA --> D[工程应用]\nB --> E[关键词]\nC --> F[流程步骤]\nD --> G[案例实践]\nF --> H[易错点：" + (request.getWeakness() == null ? "知识迁移" : request.getWeakness()) + "]\n```\n\n"
                + "- 建议配合讲义同步使用\n"
                + "- 适合复习前快速回顾全貌\n";
        return AgentResult.builder()
                .type("mindmap")
                .title("知识点思维导图")
                .markdown(md)
                .links(List.of("https://mermaid.live"))
                .confidence(89)
                .agentName(name())
                .build();
    }
}
