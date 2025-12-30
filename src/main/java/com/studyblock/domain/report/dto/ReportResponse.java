package com.studyblock.domain.report.dto;

import com.studyblock.domain.report.entity.Report;
import com.studyblock.domain.report.enums.ReportStatus;
import com.studyblock.domain.report.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long userId;  // 신고한 사용자 ID
    private String userName;  // 신고한 사용자 이름
    private String userNickname;  // 신고한 사용자 닉네임
    private Long reportedUserId;  // 신고당한 사용자 ID
    private String reportedUserName;  // 신고당한 사용자 이름
    private String reportedUserNickname;  // 신고당한 사용자 닉네임
    private ReportTargetType targetType;  // 신고 대상 타입
    private Long contentId;  // 신고 대상 컨텐츠 ID
    private String contentTitle;  // 신고 대상 컨텐츠 제목/내용
    private String reportReason;  // 신고 사유
    private ReportStatus status;  // 신고 상태
    private LocalDateTime reportedAt;  // 신고 접수 일시
    private LocalDateTime reportedActedAt;  // 신고 처리 일시

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .userName(report.getUser().getName())
                .userNickname(report.getUser().getNickname())
                .reportedUserId(report.getReportedUser().getId())
                .reportedUserName(report.getReportedUser().getName())
                .reportedUserNickname(report.getReportedUser().getNickname())
                .targetType(report.getTargetType())
                .contentId(report.getContentId())
                .contentTitle(null)  // ReportService에서 설정
                .reportReason(report.getReportReason())
                .status(report.getStatus())
                .reportedAt(report.getReportedAt())
                .reportedActedAt(report.getReportedActedAt())
                .build();
    }
}
