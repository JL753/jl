package com.iflytek.smartprep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.QaHistory;
import com.iflytek.smartprep.mapper.QaHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QaHistoryService {

    private final QaHistoryMapper qaHistoryMapper;

    public List<QaHistory> getRecentByUserId(Long userId, int limit) {
        return qaHistoryMapper.selectList(
            new LambdaQueryWrapper<QaHistory>()
                .eq(QaHistory::getUserId, userId)
                .orderByDesc(QaHistory::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    public QaHistory save(QaHistory qaHistory) {
        if (qaHistory.getCreatedAt() == null) {
            qaHistory.setCreatedAt(LocalDateTime.now());
        }
        qaHistoryMapper.insert(qaHistory);
        return qaHistory;
    }

    public List<QaHistory> getBySessionId(String sessionId) {
        return qaHistoryMapper.selectList(
            new LambdaQueryWrapper<QaHistory>()
                .eq(QaHistory::getSessionId, sessionId)
                .orderByAsc(QaHistory::getCreatedAt)
        );
    }
}
