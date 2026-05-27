package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final DashboardService dashboardService;

    @GetMapping("/health")
    public ApiResponse<java.util.Map<String, Object>> health() {
        return ApiResponse.ok(java.util.Map.of("status", "UP", "service", "smartprep-backend"));
    }

    @GetMapping("/portal")
    public ApiResponse<java.util.Map<String, Object>> portal() {
        return ApiResponse.ok(java.util.Map.of(
                "heroStats", java.util.List.of(
                        java.util.Map.of("label", "智能体数量", "value", "6"),
                        java.util.Map.of("label", "资源类型", "value", "5+"),
                        java.util.Map.of("label", "画像维度", "value", "10"),
                        java.util.Map.of("label", "课程知识点", "value", "128")),
                "courses", java.util.List.of(
                        java.util.Map.of("tag", "AI", "category", "人工智能", "title", "人工智能导论", "desc", "覆盖概念、案例、题库与代码实操。", "price", "免费"),
                        java.util.Map.of("tag", "ML", "category", "机器学习", "title", "机器学习基础", "desc", "围绕分类、回归、评估指标组织多模态资源。", "price", "¥299"),
                        java.util.Map.of("tag", "DL", "category", "深度学习", "title", "深度学习实践", "desc", "适合竞赛冲刺和项目实训的高级课程。", "price", "¥399")),
                "timeline", java.util.List.of(
                        java.util.Map.of("step", "画像构建", "desc", "通过自然语言对话自动抽取学生特征。"),
                        java.util.Map.of("step", "多智能体生成", "desc", "不同智能体协同生成多模态学习资源。"),
                        java.util.Map.of("step", "路径规划", "desc", "结合学习进度和目标动态调整学习步骤。"),
                        java.util.Map.of("step", "评估优化", "desc", "根据行为与测评数据持续优化资源推送。"))));
    }

    @GetMapping("/datacenter")
    public ApiResponse<java.util.Map<String, Object>> datacenter() {
        Long uid = LoginUserHolder.get() == null ? null : LoginUserHolder.get().getUserId();
        java.util.Map<String, Object> studentView = uid == null ? java.util.Map.of(
                "studentName", "安安琪",
                "profile", java.util.Map.of(),
                "assessment", java.util.Map.of(),
                "honors", java.util.List.of()) : dashboardService.studentDataCenter(uid);
        return ApiResponse.ok(java.util.Map.of(
                "schoolView", java.util.Map.of(
                        "stats", dashboardService.datacenterScreen().get("schoolStats"),
                        "mapTrend", java.util.List.of(
                                java.util.Map.of("name", "2020", "value", 410),
                                java.util.Map.of("name", "2021", "value", 520),
                                java.util.Map.of("name", "2022", "value", 610),
                                java.util.Map.of("name", "2023", "value", 702),
                                java.util.Map.of("name", "2024", "value", 745))),
                "departmentView", java.util.Map.of(
                        "departmentName", "计算机与软件工程学院",
                        "classRisk", java.util.List.of(
                                java.util.Map.of("name", "一班", "value", 82),
                                java.util.Map.of("name", "二班", "value", 90),
                                java.util.Map.of("name", "三班", "value", 86)),
                        "tasks", java.util.List.of(
                                "任务1：问题进度存在风险", "任务2：作业进度存在风险", "任务3：考试平均分存在风险", "任务4：课程完成度存在不达标风险")),
                "studentView", studentView));
    }
}
