package com.studyblock.domain.admin.service;

import com.studyblock.domain.admin.dto.ReviewListResponse;
import com.studyblock.domain.admin.repository.ReviewListRepository;
import com.studyblock.domain.course.entity.CourseReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewListService {

    private final ReviewListRepository reviewListRepository;

    public List<ReviewListResponse> getReviewList() {
        List<CourseReview> reviewList = reviewListRepository.findAll();

        return reviewList.stream()
                .map(review -> {
                    var course = review.getCourse();
                    var primaryCategory = course.getPrimaryCategory();
                    
                    return ReviewListResponse.builder()
                            .id(review.getId())
                            .content(review.getContent())
                            .name(review.getUser().getName())
                            .title(course.getTitle())
                            .rating(review.getRating())
                            .update_at(review.getUpdatedAt())
                            .categoryId(primaryCategory != null ? primaryCategory.getId() : null)
                            .categoryName(primaryCategory != null ? primaryCategory.getName() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
