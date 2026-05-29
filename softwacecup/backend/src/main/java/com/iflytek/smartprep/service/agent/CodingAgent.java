package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodingAgent implements Agent {
    @Override
    public String name() {
        return "实操项目智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String md = "```java\npublic class Demo {\n    public static void main(String[] args) {\n        System.out.println(\"主题：" + request.getTopic() + "\");\n        System.out.println(\"任务：完成一个最小可运行案例\");\n    }\n}\n```\n\n"
                + "## 实验步骤\n"
                + "1. 环境准备\n"
                + "2. 数据/输入定义\n"
                + "3. 核心逻辑编码\n"
                + "4. 运行测试与结果分析\n"
                + "5. 针对短板进行优化\n\n"
                + "## 评分点\n"
                + "- 正确性\n"
                + "- 可读性\n"
                + "- 迁移应用能力\n";
        return AgentResult.builder()
                .type("coding")
                .title("代码实操案例与实践项目")
                .markdown(md)
                .links(List.of("https://github.com", "https://gitee.com"))
                .confidence(91)
                .agentName(name())
                .build();
    }
}
