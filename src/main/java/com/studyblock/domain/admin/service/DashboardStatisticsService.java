package com.studyblock.domain.admin.service;

import com.studyblock.domain.activitylog.dto.ActivityLogResponse;
import com.studyblock.domain.activitylog.service.ActivityLogService;
import com.studyblock.domain.admin.dto.*;
import com.studyblock.domain.course.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardStatisticsService {
    
    private final CourseRepository courseRepository;
    private final ActivityLogService activityLogService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional(readOnly = true, noRollbackFor = {Exception.class})
    public DashboardStatisticsResponse getStatistics() {
        log.info("대시보드 통계 조회 시작");
        
        // 1. 최근 5개월 수입 계산 (환불 제외)
        List<MonthlyRevenueDto> monthlyRevenue = calculateMonthlyRevenue();
        
        // 2. 최근 5개월 신규 가입자 수
        List<MonthlyUserDto> monthlyUsers = calculateMonthlyUsers();
        
        // 3. 회원가입 경로 분포
        SignupPathDto signupPath = calculateSignupPath();
        
        // 4. 수강자 수 TOP 5
        List<TopCourseDto> topCourses = getTopCoursesByEnrollment();
        
        // 5. 활성 강의 수
        Integer activeCourses = countActiveCourses();
        
        // 6. 평균 평점
        Double averageRating = calculateAverageRating();
        
        // 7. 최근 활동 로그 전체 (최근 50개)
        List<ActivityLogResponse> recentActivities = getRecentActivities();
        
        log.info("대시보드 통계 조회 완료 - 활성 강의: {}, 평균 평점: {}", activeCourses, averageRating);
        
        return DashboardStatisticsResponse.builder()
                .monthlyRevenue(monthlyRevenue)
                .monthlyUsers(monthlyUsers)
                .signupPath(signupPath)
                .topCourses(topCourses)
                .activeCourses(activeCourses)
                .averageRating(averageRating)
                .recentActivities(recentActivities)
                .build();
    }
    
    /**
     * 최근 5개월 수입 계산 (환불 제외)
     * Query: ORDER 테이블에서 
     *        - status = 'PAID' (결제 완료)
     *        - Refund 테이블과 LEFT JOIN하여 환불되지 않은 주문만 포함 (Refund.status != 'PROCESSED')
     *        - 현재 월 포함 최근 5개월 모두 반환 (데이터 없는 달은 0)
     *        - 월별로 GROUP BY
     */
    private List<MonthlyRevenueDto> calculateMonthlyRevenue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        // 현재 월 포함 최근 5개월 목록 생성
        List<String> monthList = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            LocalDateTime month = currentMonthStart.minusMonths(i);
            monthList.add(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        String query = """
            SELECT 
                FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') as month,
                COALESCE(SUM(o.totalAmount), 0) as totalAmount
            FROM com.studyblock.domain.payment.entity.Order o
            LEFT JOIN com.studyblock.domain.refund.entity.Refund r ON r.order.id = o.id AND r.status = 'PROCESSED'
            WHERE o.status = 'PAID'
              AND r.id IS NULL
              AND o.createdAt >= :startDate
            GROUP BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m')
            ORDER BY month ASC
        """;
        
        try {
            LocalDateTime fiveMonthsAgo = currentMonthStart.minusMonths(4);
            List<Object[]> results = entityManager.createQuery(query, Object[].class)
                .setParameter("startDate", fiveMonthsAgo)
                .getResultList();
            
            // 결과를 Map으로 변환
            Map<String, Long> resultMap = results.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> ((Number) row[1]).longValue()
                ));
            
            // 모든 월에 대해 데이터 생성 (없는 달은 0)
            return monthList.stream()
                .map(month -> new MonthlyRevenueDto(
                    formatMonth(month),  // "2024-11" -> "11월"
                    resultMap.getOrDefault(month, 0L)
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("월별 수입 계산 중 오류 발생", e);
            // 에러 발생 시에도 5개월 모두 0으로 반환
            return monthList.stream()
                .map(month -> new MonthlyRevenueDto(formatMonth(month), 0L))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 최근 5개월 신규 가입자 수
     * Query: USER 테이블에서 created_at 기준 월별 COUNT
     * 현재 월 포함 최근 5개월 모두 반환 (데이터 없는 달은 0)
     */
    private List<MonthlyUserDto> calculateMonthlyUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        // 현재 월 포함 최근 5개월 목록 생성
        List<String> monthList = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            LocalDateTime month = currentMonthStart.minusMonths(i);
            monthList.add(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        String query = """
            SELECT 
                FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m') as month,
                COUNT(u.id) as userCount
            FROM com.studyblock.domain.user.entity.User u
            WHERE u.createdAt >= :startDate
            GROUP BY FUNCTION('DATE_FORMAT', u.createdAt, '%Y-%m')
            ORDER BY month ASC
        """;
        
        try {
            LocalDateTime fiveMonthsAgo = currentMonthStart.minusMonths(4);
            List<Object[]> results = entityManager.createQuery(query, Object[].class)
                .setParameter("startDate", fiveMonthsAgo)
                .getResultList();
            
            // 결과를 Map으로 변환
            Map<String, Integer> resultMap = results.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> ((Number) row[1]).intValue()
                ));
            
            // 모든 월에 대해 데이터 생성 (없는 달은 0)
            List<MonthlyUserDto> result = monthList.stream()
                .map(month -> new MonthlyUserDto(
                    formatMonth(month),
                    resultMap.getOrDefault(month, 0)
                ))
                .collect(Collectors.toList());
            
            log.info("월별 사용자 데이터 생성 완료 - 총 {}개월: {}", result.size(), 
                result.stream().map(dto -> dto.getMonth() + "(" + dto.getCount() + ")").collect(Collectors.joining(", ")));
            
            return result;
        } catch (Exception e) {
            log.error("월별 사용자 계산 중 오류 발생", e);
            // 에러 발생 시에도 5개월 모두 0으로 반환
            return monthList.stream()
                .map(month -> new MonthlyUserDto(formatMonth(month), 0))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 회원가입 경로 분포
     * Query: USER 테이블에서 jointype별 COUNT
     *        jointype: 0=LOCAL, 1=KAKAO, 2=NAVER, 3=GOOGLE
     */
    private SignupPathDto calculateSignupPath() {
        String query = """
            SELECT u.jointype, COUNT(u.id)
            FROM com.studyblock.domain.user.entity.User u
            GROUP BY u.jointype
        """;
        
        try {
            List<Object[]> results = entityManager.createQuery(query, Object[].class)
                .getResultList();
            
            Map<Integer, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                    row -> (Integer) row[0],
                    row -> ((Number) row[1]).longValue()
                ));
            
            return new SignupPathDto(
                countMap.getOrDefault(0, 0L).intValue(),  // LOCAL
                countMap.getOrDefault(1, 0L).intValue(),  // KAKAO
                countMap.getOrDefault(2, 0L).intValue(),  // NAVER
                countMap.getOrDefault(3, 0L).intValue()   // GOOGLE
            );
        } catch (Exception e) {
            log.error("가입 경로 계산 중 오류 발생", e);
            return new SignupPathDto(0, 0, 0, 0);
        }
    }
    
    /**
     * 수강자 수 TOP 5
     * Query: COURSE 테이블에서 enrollment_count 상위 5개
     */
    private List<TopCourseDto> getTopCoursesByEnrollment() {
        String query = """
            SELECT c.title, c.enrollmentCount
            FROM com.studyblock.domain.course.entity.Course c
            WHERE c.isPublished = true
            ORDER BY c.enrollmentCount DESC
        """;
        
        try {
            List<Object[]> results = entityManager.createQuery(query, Object[].class)
                .setMaxResults(5)
                .getResultList();
            
            return results.stream()
                .map(row -> new TopCourseDto(
                    (String) row[0],
                    ((Number) row[1]).intValue()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("TOP 강의 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 활성 강의 수
     * Query: COURSE 테이블에서 is_published = true COUNT
     */
    private Integer countActiveCourses() {
        try {
            Integer count = courseRepository.countByIsPublished(true);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("활성 강의 수 조회 중 오류 발생", e);
            return 0;
        }
    }
    
    /**
     * 평균 평점 계산
     * Query: COURSE_REVIEW 테이블에서 rating 평균
     */
    private Double calculateAverageRating() {
        String query = "SELECT AVG(r.rating) FROM com.studyblock.domain.course.entity.CourseReview r";
        
        try {
            Double avg = (Double) entityManager.createQuery(query)
                .getSingleResult();
            
            return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;  // 소수점 1자리
        } catch (Exception e) {
            log.error("평균 평점 계산 중 오류 발생", e);
            return 0.0;
        }
    }
    
    /**
     * 최근 활동 로그 (전체)
     * Query: ACTIVITY_LOG 테이블에서 최근 50개
     */
    private List<ActivityLogResponse> getRecentActivities() {
        try {
            return activityLogService.getRecentLogs(50);
        } catch (Exception e) {
            log.error("최근 활동 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 월 포맷 변환
     * @param yearMonth "2024-11"
     * @return "11월"
     */
    private String formatMonth(String yearMonth) {
        String[] parts = yearMonth.split("-");
        return Integer.parseInt(parts[1]) + "월";
    }
}

