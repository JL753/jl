package com.iflytek.smartprep.service.agent;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.AgentResult;
import com.iflytek.smartprep.dto.ResourceGenerateRequest;

public interface Agent {
    String name();
    AgentResult run(StudentProfile profile, ResourceGenerateRequest request);
}
