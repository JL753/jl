package com.iflytek.smartprep.controller;

import com.iflytek.smartprep.config.LoginUserHolder;
import com.iflytek.smartprep.domain.StudyPath;
import com.iflytek.smartprep.dto.ApiResponse;
import com.iflytek.smartprep.dto.StudyPathRequest;
import com.iflytek.smartprep.service.StudyPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/path")
@RequiredArgsConstructor
public class StudyPathController {

    private final StudyPathService studyPathService;

    @PostMapping("/generate")
    public ApiResponse<StudyPath> generate(@RequestBody StudyPathRequest request) {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(studyPathService.generate(uid, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<StudyPath>> mine() {
        Long uid = LoginUserHolder.get().getUserId();
        return ApiResponse.ok(studyPathService.list(uid));
    }
}
