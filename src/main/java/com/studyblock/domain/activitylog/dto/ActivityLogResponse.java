package com.studyblock.domain.activitylog.dto;

import com.studyblock.domain.activitylog.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityLogResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userNickname;
    private ActionType actionType;
    private String targetType;
    private Long targetId;
    private String description;
    private String ipAddress;
    private String metadata;
    private LocalDateTime createdAt;
}
