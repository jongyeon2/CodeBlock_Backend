package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.QuizOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionResponse {

    private Long id;
    private String optionText;
    private Integer sequence;
    // isCorrect는 제출 후에만 반환

    public static QuizOptionResponse from(QuizOption option) {
        return QuizOptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .sequence(option.getSequence())
                .build();
    }
}
