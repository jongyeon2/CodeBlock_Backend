package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.*;
import com.studyblock.domain.course.entity.Quiz;
import com.studyblock.domain.course.entity.QuizQuestion;
import com.studyblock.domain.course.entity.QuizOption;
import com.studyblock.domain.course.enums.QuestionType;
import com.studyblock.domain.course.repository.QuizRepository;
import com.studyblock.domain.course.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final com.studyblock.domain.course.repository.SectionRepository sectionRepository;
    private final com.studyblock.domain.course.repository.LectureRepository lectureRepository;
    private final com.studyblock.domain.course.repository.CourseRepository courseRepository;

    /**
     * 강의 ID로 퀴즈 목록 조회
     */
    public List<QuizSummaryResponse> getQuizzesByLectureId(Long lectureId) {
        log.info("강의 ID {}의 퀴즈 목록 조회", lectureId);

        List<Quiz> quizzes = quizRepository.findByLecture_IdOrderBySequenceAsc(lectureId);

        return quizzes.stream()
                .map(QuizSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 섹션 ID로 퀴즈 목록 조회
     */
    public List<QuizSummaryResponse> getQuizzesBySectionId(Long sectionId) {
        log.info("섹션 ID {}의 퀴즈 목록 조회", sectionId);

        List<Quiz> quizzes = quizRepository.findBySection_IdOrderBySequenceAsc(sectionId);

        return quizzes.stream()
                .map(QuizSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 퀴즈 상세 정보 조회 (문제 포함)
     */
    public QuizDetailResponse getQuizDetail(Long quizId) {
        log.info("퀴즈 ID {} 상세 조회", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId));

        // 문제와 옵션을 함께 조회
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdWithOptions(quizId);
        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(questions);

        return QuizDetailResponse.from(quiz);
    }

    /**
     * 퀴즈 제출 및 채점
     */
    @Transactional
    public QuizResultResponse submitQuiz(Long quizId, QuizSubmitRequest request) {
        log.info("퀴즈 ID {} 제출", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdWithOptions(quizId);
        Map<Long, Object> answers = request.getAnswers();

        // 채점
        int correctCount = 0;
        int totalPoints = 0;
        int earnedPoints = 0;

        for (QuizQuestion question : questions) {
            totalPoints += question.getPoints();

            Object answer = answers.get(question.getId());
            if (answer == null) {
                continue;
            }

            boolean isCorrect = false;

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                // 객관식: answerId와 정답 비교
                Long answerId = ((Number) answer).longValue();
                QuizOption correctOption = question.getCorrectOption();

                if (correctOption != null && correctOption.getId().equals(answerId)) {
                    isCorrect = true;
                }
            } else if (question.getQuestionType() == QuestionType.SHORT_ANSWER) {
                // 주관식: 정답 텍스트와 비교 (대소문자 무시, 공백 제거)
                String answerText = answer.toString().trim().toLowerCase();
                QuizOption correctOption = question.getCorrectOption();

                if (correctOption != null) {
                    String correctText = correctOption.getOptionText().trim().toLowerCase();
                    if (answerText.equals(correctText)) {
                        isCorrect = true;
                    }
                }
            }

            if (isCorrect) {
                correctCount++;
                earnedPoints += question.getPoints();
            }
        }

        // 점수 계산 (100점 만점으로 환산)
        int score = totalPoints > 0 ? (earnedPoints * 100) / totalPoints : 0;
        boolean passed = score >= quiz.getPassingScore();

        log.info("퀴즈 채점 결과: 점수={}, 정답={}/{}, 합격={}", score, correctCount, questions.size(), passed);

        return QuizResultResponse.builder()
                .quizId(quizId)
                .score(score)
                .correctCount(correctCount)
                .totalQuestions(questions.size())
                .passed(passed)
                .attemptNumber(1) // TODO: 실제 시도 횟수 tracking 구현 필요
                .build();
    }

    /**
     * 퀴즈 시도 이력 조회
     * TODO: 실제 QuizAttempt 엔티티 구현 필요
     */
    public List<Object> getQuizAttempts(Long quizId) {
        log.info("퀴즈 ID {}의 시도 이력 조회", quizId);

        // 현재는 빈 리스트 반환
        // 추후 QuizAttempt 엔티티와 Repository 구현 후 실제 데이터 반환
        return List.of();
    }

    // ===== 강사용 퀴즈 관리 API =====

    /**
     * 퀴즈 생성 (강사용)
     * - sequence 자동 계산
     */
    @Transactional
    public QuizSummaryResponse createQuiz(QuizCreateRequest request) {
        log.info("퀴즈 생성: sectionId={}, position={}", request.getSectionId(), request.getPosition());

        // 섹션 조회
        com.studyblock.domain.course.entity.Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + request.getSectionId()));

        // 코스 조회
        com.studyblock.domain.course.entity.Course course = section.getCourse();
        if (course == null) {
            throw new IllegalArgumentException("섹션에 연결된 코스가 없습니다.");
        }

        // Sequence 자동 계산
        Integer sequence = calculateSequence(request, section);

        // 타겟 강의 조회 (AFTER_LECTURE인 경우)
        com.studyblock.domain.course.entity.Lecture targetLecture = null;
        if (request.getTargetLectureId() != null) {
            targetLecture = lectureRepository.findById(request.getTargetLectureId())
                    .orElseThrow(() -> new IllegalArgumentException("대상 강의를 찾을 수 없습니다. ID=" + request.getTargetLectureId()));
        }

        // Quiz 엔티티 생성
        Quiz quiz = Quiz.builder()
                .section(section)
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .position(request.getPosition())
                .targetLecture(targetLecture)
                .passingScore(request.getPassingScore())
                .maxAttempts(request.getMaxAttempts())
                .sequence(sequence)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        // 문제가 함께 제공된 경우 문제도 생성
        if (request.getQuestion() != null) {
            createQuizQuestion(savedQuiz, request.getQuestion());
        }

        log.info("퀴즈 생성 완료: id={}, sequence={}", savedQuiz.getId(), sequence);
        return QuizSummaryResponse.from(savedQuiz);
    }

    /**
     * 코스 ID로 퀴즈 목록 조회 (강사용)
     */
    public List<QuizSummaryResponse> getQuizzesByCourseId(Long courseId) {
        log.info("코스 ID {}의 퀴즈 목록 조회", courseId);

        List<Quiz> quizzes = quizRepository.findByCourse_IdOrderBySequenceAsc(courseId);

        return quizzes.stream()
                .map(QuizSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 퀴즈 수정 (강사용)
     */
    @Transactional
    public QuizSummaryResponse updateQuiz(Long quizId, QuizUpdateRequest request) {
        log.info("퀴즈 수정: quizId={}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId));

        // 기본 정보 업데이트
        if (request.getTitle() != null || request.getDescription() != null ||
            request.getPassingScore() != null || request.getMaxAttempts() != null) {
            quiz.updateInfo(
                    request.getTitle() != null ? request.getTitle() : quiz.getTitle(),
                    request.getDescription() != null ? request.getDescription() : quiz.getDescription(),
                    request.getPassingScore() != null ? request.getPassingScore() : quiz.getPassingScore(),
                    request.getMaxAttempts() != null ? request.getMaxAttempts() : quiz.getMaxAttempts()
            );
        }

        // 위치 변경 시 sequence 재계산
        if (request.getPosition() != null) {
            com.studyblock.domain.course.entity.Lecture targetLecture = null;
            if (request.getTargetLectureId() != null) {
                targetLecture = lectureRepository.findById(request.getTargetLectureId())
                        .orElseThrow(() -> new IllegalArgumentException("대상 강의를 찾을 수 없습니다."));
            }

            // Sequence 재계산
            QuizCreateRequest tempRequest = new QuizCreateRequest();
            tempRequest.setSectionId(quiz.getSectionId());
            tempRequest.setPosition(request.getPosition());
            tempRequest.setTargetLectureId(request.getTargetLectureId());

            com.studyblock.domain.course.entity.Section section = sectionRepository.findById(quiz.getSectionId())
                    .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다."));

            Integer newSequence = calculateSequence(tempRequest, section);

            quiz.updatePosition(request.getPosition(), targetLecture, newSequence);
        }

        log.info("퀴즈 수정 완료: quizId={}", quizId);
        return QuizSummaryResponse.from(quiz);
    }

    /**
     * 퀴즈 삭제 (강사용)
     */
    @Transactional
    public void deleteQuiz(Long quizId) {
        log.info("퀴즈 삭제: quizId={}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId));

        quizRepository.delete(quiz);
        log.info("퀴즈 삭제 완료: quizId={}", quizId);
    }

    // ===== Private Helper Methods =====

    /**
     * 퀴즈 위치에 따라 sequence 자동 계산
     */
    private Integer calculateSequence(QuizCreateRequest request, com.studyblock.domain.course.entity.Section section) {
        switch (request.getPosition()) {
            case SECTION_START:
                return 5;

            case AFTER_LECTURE:
                if (request.getTargetLectureId() == null) {
                    throw new IllegalArgumentException("AFTER_LECTURE 위치에는 target_lecture_id가 필요합니다.");
                }
                com.studyblock.domain.course.entity.Lecture targetLecture = lectureRepository.findById(request.getTargetLectureId())
                        .orElseThrow(() -> new IllegalArgumentException("대상 강의를 찾을 수 없습니다. ID=" + request.getTargetLectureId()));
                return targetLecture.getSequence() + 5;

            case SECTION_END:
                // 섹션의 강의 목록 조회
                List<com.studyblock.domain.course.entity.Lecture> lectures =
                        lectureRepository.findByCourseIdOrderBySequenceAsc(section.getCourse().getId())
                                .stream()
                                .filter(l -> l.getSection().getId().equals(section.getId()))
                                .collect(Collectors.toList());

                if (lectures.isEmpty()) {
                    // 강의가 없으면 퀴즈 중 최대 sequence + 10
                    Integer maxQuizSeq = quizRepository.findMaxSequenceBySectionId(section.getId());
                    return maxQuizSeq != null ? maxQuizSeq + 10 : 10;
                }

                // 마지막 강의 sequence + 10
                com.studyblock.domain.course.entity.Lecture lastLecture = lectures.stream()
                        .max(java.util.Comparator.comparing(com.studyblock.domain.course.entity.Lecture::getSequence))
                        .orElseThrow();
                return lastLecture.getSequence() + 10;

            default:
                throw new IllegalArgumentException("잘못된 퀴즈 위치입니다: " + request.getPosition());
        }
    }

    /**
     * 퀴즈 문제 생성 (내부 헬퍼 메서드)
     */
    private void createQuizQuestion(Quiz quiz, QuizQuestionCreateRequest questionRequest) {
        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(questionRequest.getQuestion())
                .questionType(questionRequest.getQuestionType())
                .explanation(questionRequest.getExplanation())
                .points(questionRequest.getPoints())
                .sequence(1)
                .build();

        QuizQuestion savedQuestion = quizQuestionRepository.save(question);

        // 옵션 생성 (객관식인 경우)
        if (questionRequest.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            if (questionRequest.getOptions() != null) {
                Integer correctAnswerIndex = ((Number) questionRequest.getCorrectAnswer()).intValue();

                for (int i = 0; i < questionRequest.getOptions().size(); i++) {
                    QuizOption option = QuizOption.builder()
                            .quizQuestion(savedQuestion)
                            .optionText(questionRequest.getOptions().get(i))
                            .isCorrect(i == correctAnswerIndex)
                            .sequence(i + 1)
                            .build();
                    savedQuestion.addOption(option);
                }
            }
        } else {
            // 주관식/단답형: 정답을 옵션으로 저장
            QuizOption option = QuizOption.builder()
                    .quizQuestion(savedQuestion)
                    .optionText(questionRequest.getCorrectAnswer().toString())
                    .isCorrect(true)
                    .sequence(1)
                    .build();
            savedQuestion.addOption(option);
        }

        quiz.addQuestion(savedQuestion);
    }
}
