package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CourseFaq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseFaqResponse {

    private Long id;
    private String question;
    private String answer;
    private String tag;
    private Integer order;

    public static CourseFaqResponse from(CourseFaq faq) {
        return CourseFaqResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .tag(faq.getTag())
                .order(faq.getDisplayOrder())
                .build();
    }
}
