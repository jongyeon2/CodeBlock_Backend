package com.studyblock.domain.user.controller;

import com.studyblock.domain.user.dto.InstructorProfileDTO;
import com.studyblock.domain.user.service.InstructorPageService;
import com.studyblock.global.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class InstructorPageController {
    // 강사 마이페이지 컨트롤러

    private final InstructorPageService instructorService;

    @GetMapping("/instructor/{id}")
    public ResponseEntity<CommonResponse<InstructorProfileDTO>> getInstructorMyPage(@PathVariable Long id) {
        InstructorProfileDTO response = instructorService.getInstructorProfile(id);

        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
