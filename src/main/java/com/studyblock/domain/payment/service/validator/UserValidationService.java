package com.studyblock.domain.payment.service.validator;

import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 사용자 검증 전담 서비스
// 단일 책임: 사용자 존재 여부 검증
@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final UserRepository userRepository;

    // 사용자 검증
    public void validateUser(Long userId) {
        // 사용자가 존재하는지 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId);
        }
    }
}

