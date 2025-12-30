package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.CourseQuestionAnswerCreateRequest;
import com.studyblock.domain.course.dto.CourseQuestionAnswerResponse;
import com.studyblock.domain.course.dto.CourseQuestionAnswerUpdateRequest;
import com.studyblock.domain.course.dto.CourseQuestionCreateRequest;
import com.studyblock.domain.course.dto.CourseQuestionResponse;
import com.studyblock.domain.course.dto.CourseQuestionUpdateRequest;
import com.studyblock.domain.course.entity.Course;
import com.studyblock.domain.course.entity.CourseQuestion;
import com.studyblock.domain.course.entity.CourseQuestionAnswer;
import com.studyblock.domain.course.enums.CourseQuestionStatus;
import com.studyblock.domain.course.repository.CourseQuestionRepository;
import com.studyblock.domain.course.repository.CourseRepository;
import com.studyblock.domain.course.repository.CourseQuestionAnswerRepository;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseQuestionService {

    private final CourseQuestionRepository courseQuestionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseQuestionAnswerRepository courseQuestionAnswerRepository;

    /**
     * Q&A 목록 조회
     */
    public Page<CourseQuestionResponse> getQuestions(Long courseId, CourseQuestionStatus status, Pageable pageable) {
        log.info("코스 ID {} Q&A 목록 조회 요청. status={}", courseId, status);

        Page<CourseQuestion> questions = status != null
                ? courseQuestionRepository.findByCourseIdAndStatus(courseId, status, pageable)
                : courseQuestionRepository.findByCourseId(courseId, pageable);

        List<Long> questionIds = questions.stream()
                .map(CourseQuestion::getId)
                .toList();

        Map<Long, CourseQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(CourseQuestion::getId, Function.identity()));

        Map<Long, List<CourseQuestionAnswerResponse>> answerMap = loadAnswersByQuestionIds(questionIds, questionMap, null);

        return questions.map(question -> {
            List<CourseQuestionAnswerResponse> responses =
                    answerMap.getOrDefault(question.getId(), Collections.emptyList());
            return CourseQuestionResponse.from(question, responses);
        });
    }

    /**
     * 질문 등록
     */
    @Transactional
    public CourseQuestionResponse createQuestion(Long courseId, CourseQuestionCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다. ID=" + courseId));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID=" + request.getUserId()));

        CourseQuestion question = CourseQuestion.builder()
                .course(course)
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        CourseQuestion saved = courseQuestionRepository.save(question);

        return CourseQuestionResponse.from(saved, Collections.emptyList());
    }

    /**
     * 질문 수정
     */
    @Transactional
    public CourseQuestionResponse updateQuestion(Long courseId, Long questionId, CourseQuestionUpdateRequest request) {
        CourseQuestion question = courseQuestionRepository.findById(questionId)
                .filter(q -> q.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다. ID=" + questionId));

        question.updateQuestion(request.getTitle(), request.getContent());

        List<CourseQuestionAnswerResponse> answers =
                loadAnswersByQuestionIds(
                        List.of(question.getId()),
                        Map.of(question.getId(), question),
                        null)
                        .getOrDefault(question.getId(), Collections.emptyList());
        return CourseQuestionResponse.from(question, answers);
    }

    @Transactional
    public CourseQuestionAnswerResponse createAnswer(Long courseId, Long questionId, Long userId,
                                                     CourseQuestionAnswerCreateRequest request) {
        CourseQuestion question = courseQuestionRepository.findById(questionId)
                .filter(q -> q.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다. ID=" + questionId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID=" + userId));

        CourseQuestionAnswer answer = CourseQuestionAnswer.builder()
                .question(question)
                .author(user)
                .content(request.getContent())
                .build();

        question.addAnswer(answer);
        courseQuestionAnswerRepository.save(answer);
        question.updateAnswer(question.getAnswer(), CourseQuestionStatus.ANSWERED);

        return CourseQuestionAnswerResponse.from(answer, userId);
    }

    @Transactional
    public CourseQuestionAnswerResponse updateAnswer(Long courseId, Long answerId, Long userId,
                                                     CourseQuestionAnswerUpdateRequest request) {
        CourseQuestionAnswer answer = courseQuestionAnswerRepository.findByIdAndQuestion_Course_IdAndDeletedFalse(answerId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다. ID=" + answerId));

        if (!answer.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 답변만 수정할 수 있습니다.");
        }

        answer.updateContent(request.getContent());

        return CourseQuestionAnswerResponse.from(answer, userId);
    }

    @Transactional
    public void deleteAnswer(Long courseId, Long answerId, Long userId) {
        CourseQuestionAnswer answer = courseQuestionAnswerRepository.findByIdAndQuestion_Course_IdAndDeletedFalse(answerId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다. ID=" + answerId));

        if (!answer.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("본인 답변만 삭제할 수 있습니다.");
        }

        CourseQuestion question = answer.getQuestion();
        question.getAnswers().remove(answer);
        courseQuestionAnswerRepository.delete(answer);

        long remainingAnswers = courseQuestionAnswerRepository.countByQuestion_IdAndDeletedFalse(question.getId());
        if (remainingAnswers == 0) {
            question.updateAnswer(question.getAnswer(), CourseQuestionStatus.PENDING);
        }
    }

    /**
     * 질문 삭제
     */
    @Transactional
    public void deleteQuestion(Long courseId, Long questionId) {
        CourseQuestion question = courseQuestionRepository.findById(questionId)
                .filter(q -> q.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다. ID=" + questionId));

        courseQuestionRepository.delete(question);
    }

    private Map<Long, List<CourseQuestionAnswerResponse>> loadAnswersByQuestionIds(
            List<Long> questionIds,
            Map<Long, CourseQuestion> questionMap,
            Long currentUserId
    ) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<CourseQuestionAnswerResponse>> answerMap = courseQuestionAnswerRepository.findByQuestionIds(questionIds).stream()
                .collect(Collectors.groupingBy(
                        answer -> answer.getQuestion().getId(),
                        Collectors.mapping(
                                answer -> CourseQuestionAnswerResponse.from(answer, currentUserId),
                                Collectors.toList()
                        )
                ));

        // legacy fallback
        questionMap.values().forEach(question -> {
            if ((question.getAnswer() != null && !question.getAnswer().isBlank())
                    && !answerMap.containsKey(question.getId())) {
                CourseQuestionAnswerResponse legacy = CourseQuestionAnswerResponse.builder()
                        .id(null)
                        .authorId(question.getUser().getId())
                        .authorNickname(question.getUser().getNickname())
                        .avatarUrl(question.getUser().getImg())
                        .role("STUDENT")
                        .content(question.getAnswer())
                        .createdAt(question.getUpdatedAt())
                        .updatedAt(question.getUpdatedAt())
                        .isOwner(currentUserId != null && question.getUser().getId().equals(currentUserId))
                        .build();
                answerMap.put(question.getId(), List.of(legacy));
            }
        });

        return answerMap;
    }
}
