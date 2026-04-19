package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MediaAgent implements Agent {
    @Override
    public String name() {
        return "多模态内容智能体";
    }

    @Override
    public AgentResult run(StudentProfile profile, ResourceGenerateRequest request) {
        String md = "## 多模态教学视频/动画脚本\n"
                + "- 镜头1：用生活化案例引出‘" + request.getTopic() + "’\n"
                + "- 镜头2：图示展示关键流程与输入输出\n"
                + "- 镜头3：通过 1 道典型例题完成讲解\n"
                + "- 镜头4：总结易错点与复习建议\n\n"
                + "## 配图提示词\n"
                + "- 科技蓝教学场景\n"
                + "- 知识节点可视化\n"
                + "- 黑板/屏幕/代码片段元素\n\n"
                + "## 输出建议\n"
                + "- 适合生成 3~5 分钟微课\n"
                + "- 适合配字幕与语音播报\n";
        return AgentResult.builder()
                .type("media")
                .title("多模态视频/动画素材")
                .markdown(md)
                .links(List.of("https://www.xfyun.cn", "https://www.bilibili.com"))
                .confidence(86)
                .agentName(name())
                .build();
    }
}
