package com.studyblock.domain.admin.service;

import com.studyblock.domain.admin.repository.UserListRepository;
import com.studyblock.domain.user.dto.UserProfileResponse;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserListService {

    private final UserListRepository userRepository;

    //유저 정보 불러오기 (관리자 제외)
    public List<UserProfileResponse> getUserList() {
        // 관리자를 제외한 사용자 목록 조회
        List<User> userList = userRepository.findAllExceptAdmin();
        return userList.stream()
                .map(user -> UserProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .nickname(user.getNickname())
                        .gender(user.getGenderEnum())
                        .birth(user.getBirth())
                        .jointype(user.getJoinTypeEnum())
                        .status(user.getStatusEnum())
                        .userType(user.getInstructorProfile() != null ? "강사" : "회원") // 강사 프로필이 있으면 "강사", 없으면 "회원"
                        .created_at(user.getCreatedAt())
                        .build()
                ).collect(Collectors.toList());
    }

    // 사용자 상태 변경
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID=" + userId));

        try {
            UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
            user.updateStatus(userStatus);
            log.info("사용자 상태 변경 성공: userId={}, status={}", userId, status);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 사용자 상태: {}", status);
            throw new IllegalArgumentException("유효하지 않은 사용자 상태입니다: " + status);
        }
    }

    //활성화 상태인 전체 회원 수 조회(관리자 제외)
    public long getActiveUserCount() {
        return userRepository.countActiveUsersExcludingAdmin(UserStatus.ACTIVE.getValue());
    }

    // 차단된 사용자 조회
    public List<UserProfileResponse> getBlockedUsers() {
        List<User> blockedUsers = userRepository.findByStatusEnum(UserStatus.SUSPENDED.getValue());
        return blockedUsers.stream()
                .map(user -> UserProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .nickname(user.getNickname())
                        .gender(user.getGenderEnum())
                        .birth(user.getBirth())
                        .jointype(user.getJoinTypeEnum())
                        .status(user.getStatusEnum())
                        .userType(user.getInstructorProfile() != null ? "강사" : "회원")
                        .created_at(user.getCreatedAt())
                        .build()
                ).collect(Collectors.toList());
    }

    // 차단 사용자 통계 조회
    public BlockedUserStatistics getBlockedUserStatistics() {
        // 총 차단 사용자 수
        Long totalBlocked = userRepository.countByStatusEnum(UserStatus.SUSPENDED.getValue());
        
        // 이번 달 차단/해제 수
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        // 이번 달에 차단된 사용자 수 (status = SUSPENDED이고 이번 달에 updatedAt이 변경된 경우)
        Long thisMonthBlocked = userRepository.countByStatusAndUpdatedAtMonth(UserStatus.SUSPENDED.getValue(), currentYear, currentMonth);
        
        // 이번 달에 해제된 사용자 수는 ActivityLog를 사용하거나, 간단하게는 0으로 설정
        // 정확한 추적을 위해서는 ActivityLog를 사용하는 것이 좋지만, 일단 간단하게 구현
        Long thisMonthUnblocked = 0L; // TODO: ActivityLog를 사용하여 정확한 해제 수 추적
        
        return BlockedUserStatistics.builder()
                .totalBlocked(totalBlocked != null ? totalBlocked.intValue() : 0)
                .thisMonthBlocked(thisMonthBlocked != null ? thisMonthBlocked.intValue() : 0)
                .thisMonthUnblocked(thisMonthUnblocked.intValue())
                .build();
    }

    // 차단 사용자 통계 DTO
    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BlockedUserStatistics {
        private Integer totalBlocked;
        private Integer thisMonthBlocked;
        private Integer thisMonthUnblocked;
    }
}
