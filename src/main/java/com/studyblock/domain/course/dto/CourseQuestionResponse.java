package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CourseQuestion;
import com.studyblock.domain.course.enums.CourseQuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseQuestionResponse {

    private Long id;
    private String title;
    private String content;
    private String answer;
    private CourseQuestionStatus status;
    private String authorNickname;
    private String authorProfileImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CourseQuestionAnswerResponse> answers;

    public static CourseQuestionResponse from(CourseQuestion question, List<CourseQuestionAnswerResponse> answers) {
        String legacyAnswer = question.getAnswer();
        if ((legacyAnswer == null || legacyAnswer.isBlank()) && answers != null && !answers.isEmpty()) {
            legacyAnswer = answers.get(0).getContent();
        }

        return CourseQuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .answer(legacyAnswer)
                .status(question.getStatus())
                .authorNickname(question.getUser().getNickname())
                .authorProfileImage(question.getUser().getImg())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .answers(answers != null ? answers : Collections.emptyList())
                .build();
    }
}
