package com.studyblock.domain.course.dto;

import com.studyblock.domain.course.entity.CourseQuestionAnswer;
import com.studyblock.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CourseQuestionAnswerResponse {

    private Long id;
    private Long authorId;
    private String authorNickname;
    private String avatarUrl;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOwner;

    public static CourseQuestionAnswerResponse from(CourseQuestionAnswer answer, Long currentUserId) {
        User author = answer.getAuthor();

        return CourseQuestionAnswerResponse.builder()
                .id(answer.getId())
                .authorId(author.getId())
                .authorNickname(author.getNickname())
                .avatarUrl(author.getImg())
                .role(author.getInstructorProfile() != null ? "INSTRUCTOR" : "STUDENT")
                .content(answer.getContent())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .isOwner(currentUserId != null && author.getId().equals(currentUserId))
                .build();
    }
}

