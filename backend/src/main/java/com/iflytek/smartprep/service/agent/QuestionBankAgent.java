package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionBankAgent implements Agent {
    @Override
    public String name() {
        return "题库生成智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String weakness = request.getWeakness() == null || request.getWeakness().isBlank() ? "综合应用" : request.getWeakness();
        String md = "## 单选题\n"
                + "1. 关于" + request.getTopic() + "，下列说法正确的是？\n"
                + "- A. 仅适用于单一场景\n"
                + "- B. 需要结合任务目标进行建模\n"
                + "- C. 与数据无关\n\n"
                + "## 判断题\n"
                + "1. 该知识点只需要记忆结论，不需要理解过程。（ ）\n\n"
                + "## 简答题\n"
                + "1. 请结合‘" + weakness + "’说明该知识点在学习中常见的困难。\n\n"
                + "## 编程/应用题\n"
                + "1. 设计一个最小案例说明" + request.getTopic() + "的应用流程。\n";
        return AgentResult.builder()
                .type("question")
                .title("分层练习题与测评题")
                .markdown(md)
                .links(List.of())
                .confidence(90)
                .agentName(name())
                .build();
    }
}
