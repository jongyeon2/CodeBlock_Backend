package com.studyblock.domain.course.enums;

/**
 * 퀴즈 위치
 * - SECTION_START: 섹션 시작 (첫 강의 전)
 * - AFTER_LECTURE: 특정 강의 뒤
 * - SECTION_END: 섹션 끝 (마지막 강의 뒤)
 */
public enum QuizPosition {
    SECTION_START,      // 섹션 시작 (sequence = 5)
    AFTER_LECTURE,      // 특정 강의 뒤 (sequence = 강의sequence + 5)
    SECTION_END         // 섹션 끝 (sequence = 마지막강의sequence + 10)
}