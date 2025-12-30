package com.studyblock.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatisticsResponse {

    // 전체 통계
    private Long totalCount;  // 전체 신고 건수
    
    // 상태별 건수
    private Long pendingCount;  // 대기 중인 신고 건수
    private Long reviewingCount;  // 검토 중인 신고 건수
    private Long resolvedCount;  // 처리 완료된 신고 건수
    private Long rejectedCount;  // 거절된 신고 건수
    
    // 타입별 건수
    private Long postCount;  // 게시글 신고 건수
    private Long commentCount;  // 댓글 신고 건수
    private Long userCount;  // 유저 신고 건수
    private Long videoCount;  // 동영상 신고 건수
    private Long courseCount;  // 강의 신고 건수
}

