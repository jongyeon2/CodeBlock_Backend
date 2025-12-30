package com.studyblock.domain.course.enums;

/**
 * 퀴즈 문제 유형
 * - MULTIPLE_CHOICE: 객관식 (여러 선택지 중 하나 선택)
 * - SHORT_ANSWER: 단답형 (짧은 텍스트 답변, 자동 채점 가능)
 * - SUBJECTIVE: 주관식 (긴 서술형 답변, 자동 채점 가능 - 정답 텍스트와 비교)
 */
public enum QuestionType {
    MULTIPLE_CHOICE,    // 객관식
    SHORT_ANSWER,       // 단답형
    SUBJECTIVE          // 주관식 (자동 채점 가능)
}
