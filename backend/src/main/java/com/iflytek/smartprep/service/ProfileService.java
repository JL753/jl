package com.iflytek.smartprep.service;

import com.iflytek.smartprep.domain.StudentProfile;
import com.iflytek.smartprep.dto.DialogueProfileRequest;

import java.util.Map;

public interface ProfileService {
    StudentProfile buildByDialogue(Long userId, DialogueProfileRequest request);
    StudentProfile getByUserId(Long userId);
    StudentProfile saveOrUpdateProfile(Long userId, StudentProfile profile);
    Map<String, Object> summary(Long userId);
}
