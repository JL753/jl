package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.TutorAnswer;
import com.iflytek.smartprep.dto.TutorAskRequest;
import com.iflytek.smartprep.dto.TutorStreamEvent;
import com.iflytek.smartprep.service.TutorService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @PostMapping("/ask")
    public ApiResponse<TutorAnswer> ask(@RequestBody TutorAskRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(tutorService.ask(uid, request));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String question,
                             @RequestParam(required = false) String context,
                             @RequestParam(required = false) String answerMode,
                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            SseEmitter err = new SseEmitter();
            try { err.completeWithError(new IllegalArgumentException("Authorization header required")); } catch (Exception ignored) {}
            return err;
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        TutorStreamEvent authResp = tutorService.authEvent(token);
        if (authResp == null || authResp.getContent() == null) {
            SseEmitter err = new SseEmitter();
            try { err.completeWithError(new IllegalArgumentException("Invalid token")); } catch (Exception ignored) {}
            return err;
        }
        Long uid = Long.valueOf(authResp.getContent());
        TutorAskRequest request = new TutorAskRequest();
        request.setQuestion(question);
        request.setContext(context);
        request.setAnswerMode(answerMode);
        return tutorService.stream(uid, request);
    }
}
