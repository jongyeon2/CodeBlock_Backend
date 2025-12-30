package com.studyblock.domain.user.service;

import com.studyblock.domain.user.dto.InstructorProfilePatchRequest;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.InstructorChannelStatus;
import com.studyblock.domain.user.enums.InstructorPayStatus;
import com.studyblock.domain.user.repository.InstructorProfileRepository;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.global.aop.annotation.InstructorProfileCreated;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorProfileService {

    private final InstructorProfileRepository instructorProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InstructorProfile getMyProfile(Long userId) {
        log.debug("강사 프로필 조회 - userId: {}", userId);
        return instructorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("강사 프로필을 찾을 수 없습니다."));
    }

    @Transactional
    @InstructorProfileCreated
    public InstructorProfile upsertMyProfile(Long userId, InstructorProfilePatchRequest req) {
        log.debug("강사 프로필 Upsert - userId: {}", userId);

        InstructorProfile profile = instructorProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        // 부분 업데이트: 전달된 필드만 적용
        if (req.getCareer() != null) {
            profile.updateCareer(req.getCareer());
        }
        if (req.getSkills() != null) {
            profile.updateSkills(req.getSkills());
        }
        
        // channel 관련 필드는 함께 업데이트 (기존 메서드 사용)
        if (req.getChannelName() != null || req.getChannelUrl() != null || req.getBio() != null) {
            String channelName = req.getChannelName() != null ? req.getChannelName() : profile.getChannelName();
            String channelUrl = req.getChannelUrl() != null ? req.getChannelUrl() : profile.getChannelUrl();
            String bio = req.getBio() != null ? req.getBio() : profile.getBio();
            profile.updateChannelInfo(channelName, channelUrl, bio);
        }
        
        if (req.getContactEmail() != null) {
            profile.updateContactEmail(req.getContactEmail());
        }
        if (req.getPayStatus() != null) {
            profile.setPayStatus(req.getPayStatus());
        }
        if (req.getChannelStatus() != null) {
            profile.setChannelStatus(req.getChannelStatus());
        }
        if (req.getIsActive() != null) {
            if (Boolean.TRUE.equals(req.getIsActive())) {
                profile.activate();
            } else {
                profile.deactivate();
            }
        }

        InstructorProfile saved = instructorProfileRepository.save(profile);
        log.debug("강사 프로필 저장 완료 - profileId: {}", saved.getId());
        return saved;
    }

    private InstructorProfile createDefaultProfile(Long userId) {
        log.debug("기본 강사 프로필 생성 - userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. userId: " + userId));

        InstructorProfile created = InstructorProfile.builder()
                .user(user)
                .career(null)
                .skills(null)
                .channelName("")
                .channelUrl("http://example.com")
                .bio(null)
                .contactEmail(null)
                .build();

        // 기본 상태 설정 (엔티티 기본값과 동일하지만 명시적으로 설정)
        created.setPayStatus(InstructorPayStatus.READY);
        created.setChannelStatus(InstructorChannelStatus.INACTIVE);
        created.activate();
        
        log.debug("기본 강사 프로필 생성 완료 - userId: {}", userId);
        return created;
    }
}
