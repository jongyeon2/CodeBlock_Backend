package com.studyblock.domain.user.service;

import com.studyblock.domain.user.dto.InstructorProfileDTO;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.repository.InstructorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstructorPageService {
    private final InstructorRepository instructorRepository;

    public InstructorPageService(InstructorRepository instructorRepository) {
        this.instructorRepository = instructorRepository;
    }

    public InstructorProfileDTO getInstructorProfile(Long userId) {
        InstructorProfile instructorProfile = instructorRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 강사를 찾을 수 없습니다."));

        return InstructorProfileDTO.from(instructorProfile);
    }
}
