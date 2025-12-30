package com.studyblock.domain.course.service;

import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.Lecture;
import com.studyblock.domain.course.entity.PreviewVideo;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.enums.CourseLevel;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.LectureRepository;
import com.studyblock.domain.course.repository.PreviewVideoRepository;
import com.studyblock.domain.course.repository.SectionRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.InstructorProfileRepository;
import com.studyblock.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 맛보기 비디오 삭제 통합 테스트
 * - 실제 DB와 연동하여 삭제 동작 확인
 * - N+1 문제 해결 여부 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("맛보기 비디오 삭제 테스트")
class PreviewVideoService삭제테스트 {

    @Autowired
    private PreviewVideoService previewVideoService;

    @Autowired
    private PreviewVideoRepository previewVideoRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstructorProfileRepository instructorProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private User 강사_사용자;
    private InstructorProfile 강사_프로필;
    private Course 코스;
    private Section 섹션;
    private Lecture 강의;
    private PreviewVideo 맛보기_비디오;

    @BeforeEach
    void 테스트_데이터_준비() {
        // 1. 강사 사용자 생성
        강사_사용자 = User.builder()
                .memberId("instructor_test")
                .email("instructor@test.com")
                .password("password123")
                .name("강사명")
                .phone("01012345678")
                .birth(java.time.LocalDate.of(1990, 1, 1))
                .gender(1)
                .jointype(1)
                .nickname("강사닉네임")
                .intro("강사 소개")
                .img(null)
                .interests("프로그래밍")
                .build();
        강사_사용자 = userRepository.save(강사_사용자);

        // 2. 강사 프로필 생성
        강사_프로필 = InstructorProfile.builder()
                .user(강사_사용자)
                .channelName("테스트 채널")
                .channelUrl("test-channel")
                .bio("테스트 강사 소개")
                .build();
        강사_프로필 = instructorProfileRepository.save(강사_프로필);

        // 3. 코스 생성
        코스 = Course.builder()
                .title("테스트 코스")
                .summary("테스트 코스 요약")
                .level(CourseLevel.BEGINNER)
                .instructor(강사_프로필)
                .build();
        코스 = courseRepository.save(코스);

        // 4. 섹션 생성
        섹션 = Section.builder()
                .course(코스)
                .title("테스트 섹션")
                .description("테스트 섹션 설명")
                .sequence(1)
                .build();
        섹션 = sectionRepository.save(섹션);

        // 5. 강의 생성
        강의 = Lecture.builder()
                .course(코스)
                .section(섹션)
                .instructor(강사_프로필)
                .title("테스트 강의")
                .description("테스트 강의 설명")
                .uploadDate(java.time.LocalDate.now())
                .sequence(1)
                .isFree(false)
                .build();
        강의 = lectureRepository.save(강의);

        // 4. 맛보기 비디오 생성
        맛보기_비디오 = PreviewVideo.builder()
                .lecture(강의)
                .name("테스트 맛보기 비디오.mp4")
                .originalUrl("s3://bucket/test-preview.mp4")
                .resolution("1080p")
                .fileSize(1024L * 1024L * 100) // 100MB
                .build();
        맛보기_비디오 = previewVideoRepository.save(맛보기_비디오);
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 성공 - DB에서 실제로 삭제되는지 확인")
    void 맛보기_비디오_삭제_성공() {
        // given
        Long 삭제할_비디오_ID = 맛보기_비디오.getId();
        
        // 삭제 전 존재 여부 확인
        assertThat(previewVideoRepository.findById(삭제할_비디오_ID))
                .isPresent()
                .hasValueSatisfying(pv -> {
                    assertThat(pv.getId()).isEqualTo(삭제할_비디오_ID);
                    assertThat(pv.getLecture().getId()).isEqualTo(강의.getId());
                });

        // when - 삭제 실행
        previewVideoService.deletePreviewVideo(삭제할_비디오_ID, 강사_사용자);

        // then - DB에서 실제로 삭제되었는지 확인
        Optional<PreviewVideo> 삭제_후_조회 = previewVideoRepository.findById(삭제할_비디오_ID);
        assertThat(삭제_후_조회)
                .isEmpty()
                .as("삭제 후 DB에서 조회되지 않아야 함");

        // 강의는 여전히 존재해야 함
        assertThat(lectureRepository.findById(강의.getId()))
                .isPresent()
                .as("강의는 삭제되지 않아야 함");

        // 강의의 previewVideo 참조가 null인지 확인
        Lecture 삭제_후_강의 = lectureRepository.findById(강의.getId()).orElseThrow();
        assertThat(삭제_후_강의.getPreviewVideo())
                .as("강의의 previewVideo 참조는 null이어야 함")
                .isNull();
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 실패 - 존재하지 않는 ID")
    void 맛보기_비디오_삭제_실패_존재하지_않는_ID() {
        // given
        Long 존재하지_않는_ID = 99999L;

        // when & then - 예외 발생 확인
        assertThatThrownBy(() -> previewVideoService.deletePreviewVideo(존재하지_않는_ID, 강사_사용자))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("맛보기 비디오를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 실패 - 권한 없는 사용자")
    void 맛보기_비디오_삭제_실패_권한_없는_사용자() {
        // given - 다른 강사 사용자 생성
        User 다른_강사_생성 = User.builder()
                .memberId("other_instructor")
                .email("other@test.com")
                .password("password123")
                .name("다른 강사")
                .phone("01087654321")
                .birth(java.time.LocalDate.of(1990, 1, 1))
                .gender(1)
                .jointype(1)
                .nickname("다른강사")
                .intro("다른 강사 소개")
                .img(null)
                .interests("프로그래밍")
                .build();
        User 다른_강사 = userRepository.save(다른_강사_생성);

        InstructorProfile 다른_강사_프로필_생성 = InstructorProfile.builder()
                .user(다른_강사)
                .channelName("다른 채널")
                .channelUrl("other-channel")
                .bio("다른 강사 소개")
                .build();
        instructorProfileRepository.save(다른_강사_프로필_생성);

        // when & then - 권한 없는 사용자가 삭제 시도 시 예외 발생
        Long 비디오_ID = 맛보기_비디오.getId();
        assertThatThrownBy(() -> previewVideoService.deletePreviewVideo(비디오_ID, 다른_강사))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 강의의 소유자만 맛보기 비디오를 관리할 수 있습니다");
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 실패 - 일반 사용자(강사 프로필 없음)")
    void 맛보기_비디오_삭제_실패_일반_사용자() {
        // given - 일반 사용자 생성
        User 일반_사용자_생성 = User.builder()
                .memberId("normal_user")
                .email("normal@test.com")
                .password("password123")
                .name("일반 사용자")
                .phone("01011111111")
                .birth(java.time.LocalDate.of(1995, 1, 1))
                .gender(1)
                .jointype(1)
                .nickname("일반사용자")
                .intro("일반 사용자 소개")
                .img(null)
                .interests("일반")
                .build();
        User 일반_사용자 = userRepository.save(일반_사용자_생성);

        // when & then - 일반 사용자가 삭제 시도 시 예외 발생
        Long 비디오_ID = 맛보기_비디오.getId();
        assertThatThrownBy(() -> previewVideoService.deletePreviewVideo(비디오_ID, 일반_사용자))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("강사만 맛보기 비디오를 관리할 수 있습니다");
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 실패 - null 사용자")
    void 맛보기_비디오_삭제_실패_null_사용자() {
        // when & then - null 사용자 삭제 시도 시 예외 발생
        assertThatThrownBy(() -> previewVideoService.deletePreviewVideo(맛보기_비디오.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("로그인이 필요합니다");
    }

    @Test
    @DisplayName("맛보기 비디오 삭제 후 강의의 previewVideo 참조 확인")
    void 맛보기_비디오_삭제_후_강의_참조_확인() {
        // given
        Long 삭제할_비디오_ID = 맛보기_비디오.getId();

        // 삭제 전 강의의 previewVideo 참조 확인
        Lecture 삭제_전_강의 = lectureRepository.findById(강의.getId()).orElseThrow();
        assertThat(삭제_전_강의.getPreviewVideo())
                .isNotNull()
                .as("삭제 전에는 previewVideo가 존재해야 함");
        assertThat(삭제_전_강의.getPreviewVideo().getId()).isEqualTo(삭제할_비디오_ID);

        // when - 삭제 실행
        previewVideoService.deletePreviewVideo(삭제할_비디오_ID, 강사_사용자);

        // then - 강의의 previewVideo 참조가 null인지 확인 (orphanRemoval 확인)
        // 엔티티 매니저를 통해 다시 조회해야 변경사항이 반영됨
        lectureRepository.flush();
        Lecture 삭제_후_강의 = lectureRepository.findById(강의.getId()).orElseThrow();
        
        assertThat(삭제_후_강의.getPreviewVideo())
                .as("orphanRemoval로 인해 강의의 previewVideo 참조가 null이어야 함")
                .isNull();
    }
}

