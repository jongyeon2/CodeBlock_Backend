package com.studyblock.domain.course.enums;

/**
 * 강의 상태
 * - DRAFT: 작성 중 (비공개)
 * - PUBLISHED: 게시됨 (공개)
 * - PRIVATE: 비공개
 * - ACTIVE: 활성 (레거시, PUBLISHED와 동일)
 * - INACTIVE: 비활성 (레거시, PRIVATE와 유사)
 */
public enum LectureStatus {
    DRAFT,      // 작성 중
    PUBLISHED,  // 게시됨
    PRIVATE,    // 비공개
    ACTIVE,     // 활성 (레거시)
    INACTIVE    // 비활성 (레거시)
}
