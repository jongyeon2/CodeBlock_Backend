package com.studyblock.domain.report.dto;

import com.studyblock.domain.report.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {

    @NotNull(message = "신고 대상 타입은 필수입니다.")
    private ReportTargetType targetType;  // POST, COMMENT, USER, VIDEO, COURSE

    @NotNull(message = "신고 대상 컨텐츠 ID는 필수입니다.")
    private Long contentId;  // 신고 대상 컨텐츠 ID (postId, commentId, userId, videoId, courseId)

    @NotNull(message = "신고당한 사용자 ID는 필수입니다.")
    private Long reportedUserId;  // 신고당한 사용자 ID

    private String reportReason;  // 신고 사유 (선택)
}

