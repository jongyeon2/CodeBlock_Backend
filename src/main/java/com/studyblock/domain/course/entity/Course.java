package com.studyblock.domain.course.entity;

import com.studyblock.domain.category.entity.Category;
import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.course.enums.CourseLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private com.studyblock.domain.user.entity.InstructorProfile instructor;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    private CourseLevel level;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "discount_percentage", nullable = false)
    private Integer discountPercentage = 0;

    @Column(name = "enrollment_count", nullable = false)
    private Long enrollmentCount = 0L;

    @Column(name = "is_published", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isPublished = false;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseCategory> courseCategories = new ArrayList<>();

    @Builder
    // 코스 생성 시 사용하는 생성자
    public Course(com.studyblock.domain.user.entity.InstructorProfile instructor,
                  String title, String summary, CourseLevel level,
                  Integer durationMinutes, String thumbnailUrl, Long price, Integer discountPercentage,
                  List<Category> categories) {
        this.instructor = instructor;
        this.title = title;
        this.summary = summary;
        this.level = level;
        this.durationMinutes = durationMinutes;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
        this.enrollmentCount = 0L;
        this.isPublished = false;
        updateCategories(categories);
    }

    // 코스 발행 시 사용하는 메서드
    public void publish() {
        this.isPublished = true;
    }

    public void unpublish() {
        this.isPublished = false;
    }

    // 코스 수강 신청 시 사용하는 메서드
    public void increaseEnrollmentCount() {
        this.enrollmentCount++;
    }

    // 코스 수강 신청 취소 시 사용하는 메서드
    public void decreaseEnrollmentCount() {
        if (this.enrollmentCount > 0) {
            this.enrollmentCount--;
        }
    }

    public void updateInfo(String title, String summary, CourseLevel level,
                           String thumbnailUrl, Long price, Integer discountPercentage) {
        this.title = title;
        this.summary = summary;
        this.level = level;
        this.thumbnailUrl = thumbnailUrl;
        this.price = price;
        this.discountPercentage = discountPercentage != null ? discountPercentage : 0;
    }

    // ====== Partial update helpers (for PATCH) ======
    public void updateTitle(String title) { this.title = title; }
    public void updateSummary(String summary) { this.summary = summary; }
    public void updateLevel(CourseLevel level) { this.level = level; }
    public void updateDuration(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void updateThumbnail(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void updatePrice(Long price) { this.price = price; }
    public void updateDiscountPercentage(Integer discountPercentage) { this.discountPercentage = discountPercentage != null ? discountPercentage : 0; }

    public void addSection(Section section) {
        this.sections.add(section);
    }

    public void addLecture(Lecture lecture) {
        this.lectures.add(lecture);
    }

    public Long getDiscountedPrice() {
        if (price == null || discountPercentage == null || discountPercentage == 0) {
            return price;
        }
        return price - (price * discountPercentage / 100);
    }

    public List<Category> getCategories() {
        return courseCategories.stream()
                .map(CourseCategory::getCategory)
                .toList();
    }

    public Category getPrimaryCategory() {
        return getCategories().stream().findFirst().orElse(null);
    }

    public void updateCategories(List<Category> categories) {
        this.courseCategories.clear();

        if (categories == null || categories.isEmpty()) {
            return;
        }

        categories.stream()
                .filter(category -> category != null)
                .map(category -> CourseCategory.of(this, category))
                .forEach(courseCategories::add);
    }

    // 강사 설정/변경
    public void assignInstructor(com.studyblock.domain.user.entity.InstructorProfile instructor) {
        this.instructor = instructor;
    }

    // ✅ 강사 이름은 User 엔티티에서 가져오기
    public String getInstructorName() {
        return this.instructor != null && this.instructor.getUser() != null
                ? this.instructor.getUser().getName() : null;
    }

    // ✅ 강사 채널명은 InstructorProfile에서 가져오기
    public String getInstructorChannelName() {
        return this.instructor != null ? this.instructor.getChannelName() : null;
    }

    // ✅ 강사 ID는 InstructorProfile에서 가져오기
    public Long getInstructorId() {
        return this.instructor != null ? this.instructor.getId() : null;
    }

    //평점
    @Transient  // DB 컬럼 아님
    private Double averageRating;

    //리뷰 개수
    @Transient
    private Long reviewCount;

    //평점 정보 설정 메서드
    public void setReviewInfo(Double averageRating, Long reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

}
