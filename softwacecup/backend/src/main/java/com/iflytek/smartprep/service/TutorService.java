package com.iflytek.smartprep.service;

import com.iflytek.smartprep.dto.TutorAnswer;
import com.iflytek.smartprep.dto.TutorAskRequest;
import com.iflytek.smartprep.dto.TutorStreamEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface TutorService {
    TutorAnswer ask(Long userId, TutorAskRequest request);
    SseEmitter stream(Long userId, TutorAskRequest request);
    TutorAnswer buildAnswer(Long userId, TutorAskRequest request);
    void initializeUserData(Long userId, String displayName, String role);
    TutorStreamEvent authEvent(String token);
}
