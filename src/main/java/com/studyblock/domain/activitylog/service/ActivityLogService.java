package com.studyblock.domain.activitylog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyblock.domain.activitylog.dto.ActivityLogResponse;
import com.studyblock.domain.activitylog.entity.ActivityLog;
import com.studyblock.domain.activitylog.enums.ActionType;
import com.studyblock.domain.activitylog.repository.ActivityLogRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 활동 로그 저장
    @Transactional
    public void createLog(Long userId, ActionType actionType, String targetType,
                          Long targetId, String description, String ipAddress,
                          Map<String, Object> metadataMap) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // metadata를 JSON 문자열로 변환
            String metadata = null;
            if (metadataMap != null && !metadataMap.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                metadata = objectMapper.writeValueAsString(metadataMap);
            }

            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .metadata(metadata)
                    .build();

            activityLogRepository.save(activityLog);
            log.info("활동 로그 저장 완료 = userId: {}, actionType: {}, description: {}", userId, actionType, description);
        } catch (Exception e) {
            log.error("활동 로그 저장 실패 - userId: {}, actionType: {}", userId, actionType, e);
        }
    }

    // 최근 활동 로그 조회
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRecentLogs(int limit) {
        try {
            int maxResults = limit > 0 ? limit : 50;
            TypedQuery<ActivityLog> query = entityManager.createQuery(
                    "SELECT al FROM ActivityLog al JOIN FETCH al.user ORDER BY al.createdAt DESC",
                    ActivityLog.class
            );
            query.setMaxResults(maxResults);
            List<ActivityLog> logs = query.getResultList();
            return logs.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("최근 활동 로그 조회 실패 - limit: {}", limit, e);
            throw new RuntimeException("활동 로그 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 특정 사용자의 로그 조회
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByUserId(Long userId, int limit) {
        List<ActivityLog> logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return logs.stream()
                .limit(limit > 0 ? limit : 50)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 특정 액션 타입별 로그 조회
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByActionType(ActionType actionType, int limit) {
        List<ActivityLog> logs = activityLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType);
        return logs.stream()
                .limit(limit > 0 ? limit : 50)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 특정 사용자와 특정 액션 타입 조회
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByUserIdAndActionType(Long userId, ActionType actionType, int limit) {
        List<ActivityLog> logs = activityLogRepository.findByUserIdAndActionTypeOrderByCreatedAtDesc(userId, actionType);
        return logs.stream()
                .limit(limit > 0 ? limit : 50)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ActivityLogResponse toResponse(ActivityLog activityLog) {
        if (activityLog == null) {
            log.warn("ActivityLog가 null입니다.");
            return null;
        }
        
        User user = activityLog.getUser();
        if (user == null) {
            log.warn("ActivityLog의 User가 null입니다. logId: {}", activityLog.getId());
            return ActivityLogResponse.builder()
                    .id(activityLog.getId())
                    .userId(null)
                    .userName(null)
                    .userNickname(null)
                    .actionType(activityLog.getActionType())
                    .targetType(activityLog.getTargetType())
                    .targetId(activityLog.getTargetId())
                    .description(activityLog.getDescription())
                    .ipAddress(activityLog.getIpAddress())
                    .metadata(activityLog.getMetadata())
                    .createdAt(activityLog.getCreatedAt())
                    .build();
        }
        
        return ActivityLogResponse.builder()
                .id(activityLog.getId())
                .userId(user.getId())
                .userName(user.getName())
                .userNickname(user.getNickname())
                .actionType(activityLog.getActionType())
                .targetType(activityLog.getTargetType())
                .targetId(activityLog.getTargetId())
                .description(activityLog.getDescription())
                .ipAddress(activityLog.getIpAddress())
                .metadata(activityLog.getMetadata())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }
}
