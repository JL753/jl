package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyllabusAgent implements Agent {
    @Override
    public String name() {
        return "课程讲解智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String md = "# " + request.getCourse() + " - " + request.getTopic() + "\n"
                + "- 适配专业：" + request.getMajor() + "\n"
                + "- 当前基础：" + profile.getKnowledgeBase() + "\n"
                + "- 认知风格：" + profile.getCognitiveStyle() + "\n"
                + "- 学习目标：" + profile.getExamGoal() + "\n"
                + "\n## 一、知识目标\n"
                + "1. 理解核心概念与基本原理\n"
                + "2. 能说清典型场景与关键步骤\n"
                + "3. 完成基础练习与应用迁移\n"
                + "\n## 二、内容精讲\n"
                + "- 先从定义与背景切入\n"
                + "- 再拆分关键流程与常见算法\n"
                + "- 最后结合案例说明使用方式\n"
                + "\n## 三、学习建议\n"
                + "- 先看图解，再读讲义\n"
                + "- 对公式与步骤单独做标注\n"
                + "- 课后完成1次复盘\n";
        return AgentResult.builder()
                .type("document")
                .title("专业课程讲解文档")
                .markdown(md)
                .links(List.of("https://www.icourse163.org", "https://www.cnki.net"))
                .confidence(92)
                .agentName(name())
                .build();
    }
}
