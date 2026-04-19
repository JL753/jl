package com.cnsoftbei.smartlearning.api;

public record AgentGenerateRequest(
        String major,
        String course,
        String topic,
        String weakness,
        String preference,
        String resourceType
) {
}
