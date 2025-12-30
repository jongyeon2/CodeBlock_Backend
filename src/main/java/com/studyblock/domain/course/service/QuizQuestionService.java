package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.QuizQuestionCreateRequest;
import com.studyblock.domain.course.dto.QuizQuestionResponse;
import com.studyblock.domain.course.dto.QuizQuestionUpdateRequest;
import com.studyblock.domain.course.entity.Quiz;
import com.studyblock.domain.course.entity.QuizOption;
import com.studyblock.domain.course.entity.QuizQuestion;
import com.studyblock.domain.course.enums.QuestionType;
import com.studyblock.domain.course.repository.QuizQuestionRepository;
import com.studyblock.domain.course.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizQuestionService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizRepository quizRepository;

    /**
     * 문제 추가
     */
    @Transactional
    public QuizQuestionResponse createQuestion(Long quizId, QuizQuestionCreateRequest request) {
        log.info("문제 추가: quizId={}, questionType={}", quizId, request.getQuestionType());

        // 1. Quiz 조회
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다. ID=" + quizId));

        // 2. Sequence 자동 계산
        Integer maxSequence = quizQuestionRepository.findMaxSequenceByQuizId(quizId);
        Integer newSequence = (maxSequence != null ? maxSequence : 0) + 1;

        // 3. 유효성 검증
        validateQuestionCreate(request);

        // 4. QuizQuestion 생성
        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .questionText(request.getQuestion())
                .questionType(request.getQuestionType())
                .explanation(request.getExplanation())
                .points(request.getPoints())
                .sequence(newSequence)
                .build();

        QuizQuestion savedQuestion = quizQuestionRepository.save(question);

        // 5. 옵션 생성
        createOptions(savedQuestion, request);

        log.info("문제 추가 완료: questionId={}, sequence={}", savedQuestion.getId(), newSequence);
        return QuizQuestionResponse.from(savedQuestion);
    }

    /**
     * 문제 수정
     */
    @Transactional
    public QuizQuestionResponse updateQuestion(Long quizId, Long questionId, QuizQuestionUpdateRequest request) {
        log.info("문제 수정: quizId={}, questionId={}", quizId, questionId);

        // 1. QuizQuestion 조회 (quizId도 함께 검증)
        QuizQuestion question = quizQuestionRepository.findByQuiz_IdAndId(quizId, questionId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. quizId=" + quizId + ", questionId=" + questionId));

        // 2. 유효성 검증
        validateQuestionUpdate(question, request);

        // 3. 문제 내용 수정
        question.updateQuestion(request.getQuestion(), request.getExplanation(), request.getPoints());

        // 4. 옵션 수정 (options가 있으면 전체 교체)
        if (request.getOptions() != null) {
            updateOptions(question, request);
        }

        // 5. 정답 수정 (correctAnswer만 있으면)
        if (request.getCorrectAnswer() != null && request.getOptions() == null) {
            updateCorrectAnswer(question, request.getCorrectAnswer());
        }

        log.info("문제 수정 완료: questionId={}", questionId);
        return QuizQuestionResponse.from(question);
    }

    /**
     * 문제 삭제
     */
    @Transactional
    public void deleteQuestion(Long quizId, Long questionId) {
        log.info("문제 삭제: quizId={}, questionId={}", quizId, questionId);

        // QuizQuestion 조회 (quizId도 함께 검증)
        QuizQuestion question = quizQuestionRepository.findByQuiz_IdAndId(quizId, questionId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다. quizId=" + quizId + ", questionId=" + questionId));

        quizQuestionRepository.delete(question);
        log.info("문제 삭제 완료: questionId={}", questionId);
    }

    // ===== Private Helper Methods =====

    /**
     * 문제 생성 유효성 검증
     */
    private void validateQuestionCreate(QuizQuestionCreateRequest request) {
        QuestionType type = request.getQuestionType();

        // 객관식: 옵션 필수 (최소 2개, 최대 10개)
        if (type == QuestionType.MULTIPLE_CHOICE) {
            if (request.getOptions() == null || request.getOptions().isEmpty()) {
                throw new IllegalArgumentException("객관식 문제는 선택지가 필요합니다.");
            }
            if (request.getOptions().size() < 2) {
                throw new IllegalArgumentException("객관식 문제는 최소 2개의 선택지가 필요합니다.");
            }
            if (request.getOptions().size() > 10) {
                throw new IllegalArgumentException("객관식 문제는 최대 10개의 선택지만 허용됩니다.");
            }

            // correctAnswer가 Integer 타입인지, 범위 내인지 검증
            if (!(request.getCorrectAnswer() instanceof Integer)) {
                throw new IllegalArgumentException("객관식 문제의 정답은 숫자(옵션 인덱스)여야 합니다.");
            }
            Integer answerIndex = (Integer) request.getCorrectAnswer();
            if (answerIndex < 0 || answerIndex >= request.getOptions().size()) {
                throw new IllegalArgumentException("정답 인덱스가 선택지 범위를 벗어났습니다. (0-" + (request.getOptions().size() - 1) + ")");
            }
        } else {
            // 주관식/단답형: correctAnswer가 String이어야 함
            if (!(request.getCorrectAnswer() instanceof String)) {
                throw new IllegalArgumentException("주관식/단답형 문제의 정답은 텍스트여야 합니다.");
            }
            String answerText = (String) request.getCorrectAnswer();
            if (answerText.trim().isEmpty()) {
                throw new IllegalArgumentException("정답 텍스트는 비어있을 수 없습니다.");
            }
        }
    }

    /**
     * 문제 수정 유효성 검증
     */
    private void validateQuestionUpdate(QuizQuestion question, QuizQuestionUpdateRequest request) {
        // 옵션 수정 시 검증
        if (request.getOptions() != null) {
            if (!question.isMultipleChoice()) {
                throw new IllegalArgumentException("주관식/단답형 문제는 선택지를 가질 수 없습니다.");
            }
            if (request.getOptions().size() < 2 || request.getOptions().size() > 10) {
                throw new IllegalArgumentException("객관식 문제는 2~10개의 선택지가 필요합니다.");
            }

            // correctAnswer도 함께 있어야 함
            if (request.getCorrectAnswer() == null) {
                throw new IllegalArgumentException("선택지를 수정할 때는 정답도 함께 지정해야 합니다.");
            }

            // correctAnswer 범위 검증
            if (!(request.getCorrectAnswer() instanceof Integer)) {
                throw new IllegalArgumentException("객관식 문제의 정답은 숫자(옵션 인덱스)여야 합니다.");
            }
            Integer answerIndex = (Integer) request.getCorrectAnswer();
            if (answerIndex < 0 || answerIndex >= request.getOptions().size()) {
                throw new IllegalArgumentException("정답 인덱스가 선택지 범위를 벗어났습니다.");
            }
        }

        // 정답만 수정 시 검증
        if (request.getCorrectAnswer() != null && request.getOptions() == null) {
            if (question.isMultipleChoice()) {
                // 객관식: 정답 인덱스가 기존 옵션 범위 내인지
                if (!(request.getCorrectAnswer() instanceof Integer)) {
                    throw new IllegalArgumentException("객관식 문제의 정답은 숫자(옵션 인덱스)여야 합니다.");
                }
                Integer answerIndex = (Integer) request.getCorrectAnswer();
                if (answerIndex < 0 || answerIndex >= question.getOptions().size()) {
                    throw new IllegalArgumentException("정답 인덱스가 선택지 범위를 벗어났습니다. (0-" + (question.getOptions().size() - 1) + ")");
                }
            } else {
                // 주관식/단답형: String이어야 함
                if (!(request.getCorrectAnswer() instanceof String)) {
                    throw new IllegalArgumentException("주관식/단답형 문제의 정답은 텍스트여야 합니다.");
                }
            }
        }
    }

    /**
     * 옵션 생성 (문제 생성 시)
     */
    private void createOptions(QuizQuestion question, QuizQuestionCreateRequest request) {
        QuestionType type = request.getQuestionType();

        if (type == QuestionType.MULTIPLE_CHOICE) {
            // 객관식: 옵션 생성
            Integer correctIndex = (Integer) request.getCorrectAnswer();
            List<String> options = request.getOptions();

            for (int i = 0; i < options.size(); i++) {
                QuizOption option = QuizOption.builder()
                        .quizQuestion(question)
                        .optionText(options.get(i))
                        .isCorrect(i == correctIndex)
                        .sequence(i + 1)
                        .build();
                question.addOption(option);
            }
        } else {
            // 주관식/단답형: 정답을 옵션으로 저장 (isCorrect=true)
            String answerText = (String) request.getCorrectAnswer();
            QuizOption option = QuizOption.builder()
                    .quizQuestion(question)
                    .optionText(answerText)
                    .isCorrect(true)
                    .sequence(1)
                    .build();
            question.addOption(option);
        }
    }

    /**
     * 옵션 전체 교체 (문제 수정 시)
     */
    private void updateOptions(QuizQuestion question, QuizQuestionUpdateRequest request) {
        // 기존 옵션 전체 삭제
        question.clearOptions();

        // 새 옵션 생성
        Integer correctIndex = (Integer) request.getCorrectAnswer();
        List<String> options = request.getOptions();

        for (int i = 0; i < options.size(); i++) {
            QuizOption option = QuizOption.builder()
                    .quizQuestion(question)
                    .optionText(options.get(i))
                    .isCorrect(i == correctIndex)
                    .sequence(i + 1)
                    .build();
            question.addOption(option);
        }
    }

    /**
     * 정답만 수정 (옵션 변경 없이)
     */
    private void updateCorrectAnswer(QuizQuestion question, Object correctAnswer) {
        if (question.isMultipleChoice()) {
            // 객관식: 기존 옵션의 isCorrect만 변경
            Integer newCorrectIndex = (Integer) correctAnswer;
            List<QuizOption> options = question.getOptions();

            for (int i = 0; i < options.size(); i++) {
                QuizOption option = options.get(i);
                if (i == newCorrectIndex) {
                    option.markAsCorrect();
                } else {
                    option.markAsIncorrect();
                }
            }
        } else {
            // 주관식/단답형: 옵션 텍스트 변경
            String newAnswerText = (String) correctAnswer;
            QuizOption option = question.getCorrectOption();
            if (option != null) {
                option.updateOption(newAnswerText, true);
            }
        }
    }
}