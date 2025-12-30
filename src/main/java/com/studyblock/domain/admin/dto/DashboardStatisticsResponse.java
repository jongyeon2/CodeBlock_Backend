package com.studyblock.domain.admin.dto;

import com.studyblock.domain.activitylog.dto.ActivityLogResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStatisticsResponse {
    // 총 수입 (최근 5개월)
    private List<MonthlyRevenueDto> monthlyRevenue;
    
    // 총 사용자 수 (최근 5개월)
    private List<MonthlyUserDto> monthlyUsers;
    
    // 회원가입 경로 (전체)
    private SignupPathDto signupPath;
    
    // 수강자 수 TOP 5
    private List<TopCourseDto> topCourses;
    
    // 통계 카드 데이터
    private Integer activeCourses;  // 활성 강의 수
    private Double averageRating;   // 평균 평점
    
    // 최근 활동 (전체, 페이징 없음)
    private List<ActivityLogResponse> recentActivities;
}

