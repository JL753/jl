package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * AI 智能体路由控制器
 * 接收前端意图分析结果，路由到 AgentService 生成真实 LLM 响应
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** 课程推荐智能体 */
    @PostMapping("/course-recommend")
    public Map<String, Object> courseRecommend(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        return agentService.courseRecommend(query);
    }

    /** 知识讲解智能体 */
    @PostMapping("/knowledge")
    public Map<String, Object> knowledge(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.knowledgeExplain(query, userId);
    }

    /** 学情诊断智能体 */
    @PostMapping("/diagnosis")
    public Map<String, Object> diagnosis(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.diagnosis(query, userId);
    }

    /** 路径规划智能体 */
    @PostMapping("/path-planning")
    public Map<String, Object> pathPlanning(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        Long userId = getUserIdOrNull();
        return agentService.pathPlanning(query, userId);
    }

    /**
     * Unity AI 虚拟人专用——流式对话（SSE）
     * 返回结构化回复：表情|动作|回复文本|指令
     */
    @PostMapping("/chat-stream")
    public SseEmitter chatStream(@RequestBody Map<String, Object> request,
                                  @RequestHeader(value = "Authorization") String authHeader) {
        SseEmitter emitter = new SseEmitter(60000L);

        String question = (String) request.getOrDefault("question", "");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) request.getOrDefault("history", Collections.emptyList());

        new Thread(() -> {
            try {
                String result = agentService.chatStreamWithCompanion(question, history, chunk -> {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("delta", chunk);
                        data.put("finish", false);
                        emitter.send(SseEmitter.event().name("message").data(data));
                    } catch (IOException e) {
                        // 客户端断开
                    }
                });

                Map<String, Object> done = new HashMap<>();
                done.put("delta", "");
                done.put("finish", true);
                emitter.send(SseEmitter.event().name("message").data(done));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 语音识别（STT）
     * 接收 WAV 音频文件，返回识别文本
     */
    @PostMapping("/stt")
    public Map<String, Object> speechToText(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            result.put("success", false);
            result.put("message", file.isEmpty() ? "音频文件为空" : "音频文件过大（最大 10MB）");
            return result;
        }

        result.put("success", true);
        result.put("text", "语音识别功能接入中...");
        return result;
    }

    /**
     * 语音合成（TTS）
     * 接收文本，返回 WAV 音频流
     */
    @PostMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody Map<String, Object> request) {
        String text = (String) request.getOrDefault("text", "");

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] silentWav = generateSilentWav();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(silentWav);
    }

    /**
     * 生成最小静默 WAV 文件头（TTS API 就绪前的兜底）
     */
    private byte[] generateSilentWav() {
        int sampleRate = 16000;
        int durationMs = 100;
        int dataSize = (sampleRate * 2 * durationMs) / 1000;
        byte[] wav = new byte[44 + dataSize];
        // RIFF header
        wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
        int fileSize = 36 + dataSize;
        wav[4] = (byte)(fileSize); wav[5] = (byte)(fileSize >> 8);
        wav[6] = (byte)(fileSize >> 16); wav[7] = (byte)(fileSize >> 24);
        wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';
        // fmt chunk
        wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
        wav[16] = 16; wav[17] = 0; wav[18] = 0; wav[19] = 0;
        wav[20] = 1; wav[21] = 0; // PCM
        wav[22] = 1; wav[23] = 0; // mono
        wav[24] = (byte)(sampleRate); wav[25] = (byte)(sampleRate >> 8);
        wav[26] = (byte)(sampleRate >> 16); wav[27] = (byte)(sampleRate >> 24);
        int byteRate = sampleRate * 2;
        wav[28] = (byte)(byteRate); wav[29] = (byte)(byteRate >> 8);
        wav[30] = (byte)(byteRate >> 16); wav[31] = (byte)(byteRate >> 24);
        wav[32] = 2; wav[33] = 0; // block align
        wav[34] = 16; wav[35] = 0; // bits per sample
        // data chunk
        wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
        wav[40] = (byte)(dataSize); wav[41] = (byte)(dataSize >> 8);
        wav[42] = (byte)(dataSize >> 16); wav[43] = (byte)(dataSize >> 24);
        return wav;
    }

    private Long getUserIdOrNull() {
        try {
            return LoginUserHolder.get().getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
