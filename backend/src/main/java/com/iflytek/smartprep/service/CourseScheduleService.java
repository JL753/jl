package com.iflytek.smartprep.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.smartprep.domain.CourseSchedule;
import com.iflytek.smartprep.mapper.CourseScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseScheduleService {

    private final CourseScheduleMapper courseScheduleMapper;

    public List<CourseSchedule> getByUserId(Long userId) {
        return courseScheduleMapper.selectList(
            new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getUserId, userId)
                .orderByAsc(CourseSchedule::getDayOfWeek, CourseSchedule::getStartTime)
        );
    }

    public List<CourseSchedule> getByUserIdAndDay(Long userId, Integer dayOfWeek) {
        return courseScheduleMapper.selectList(
            new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getUserId, userId)
                .eq(CourseSchedule::getDayOfWeek, dayOfWeek)
                .orderByAsc(CourseSchedule::getStartTime)
        );
    }

    public CourseSchedule save(CourseSchedule schedule) {
        if (schedule.getCreatedAt() == null) {
            schedule.setCreatedAt(LocalDateTime.now());
        }
        schedule.setUpdatedAt(LocalDateTime.now());
        courseScheduleMapper.insert(schedule);
        return schedule;
    }

    public void update(CourseSchedule schedule) {
        schedule.setUpdatedAt(LocalDateTime.now());
        courseScheduleMapper.updateById(schedule);
    }

    public void delete(Long id) {
        courseScheduleMapper.deleteById(id);
    }

    public void deleteByUserId(Long userId) {
        courseScheduleMapper.delete(
            new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getUserId, userId)
        );
    }
}
