package com.studyblock.domain.course.service;

import com.studyblock.domain.course.dto.MaterialDeleteResponse;
import com.studyblock.domain.course.dto.MaterialResponse;
import com.studyblock.domain.course.dto.MaterialUpdateRequest;
import com.studyblock.domain.course.entity.LectureResource;
import com.studyblock.domain.course.entity.Section;
import com.studyblock.domain.course.repository.LectureResourceRepository;
import com.studyblock.domain.course.repository.SectionRepository;
import com.studyblock.domain.user.entity.InstructorProfile;
import com.studyblock.domain.user.entity.User;
import com.studyblock.infrastructure.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 섹션 자료 관리 서비스
 * 섹션 단위로 자료를 업로드/수정/삭제하는 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SectionMaterialService {

    private final LectureResourceRepository resourceRepository;
    private final SectionRepository sectionRepository;
    private final S3StorageService s3StorageService;

    // 권한 검증용 리포지토리
    private final com.studyblock.domain.enrollment.repository.CourseEnrollmentRepository courseEnrollmentRepository;
    private final com.studyblock.domain.user.repository.LectureOwnershipRepository lectureOwnershipRepository;

    // Presigned URL 유효시간 (분)
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 60;

    // 파일 크기 제한 (100MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;

    /**
     * 섹션 자료 목록 조회
     * @param sectionId 섹션 ID
     * @param currentUser 현재 사용자 (강사 또는 수강생)
     * @return 자료 목록
     */
    public List<MaterialResponse> getMaterialsBySection(Long sectionId, User currentUser) {
        log.info("섹션 ID {}의 자료 목록 조회 - 사용자: {}", sectionId, currentUser.getEmail());

        // 섹션 조회
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId));

        // 권한 검증 (강사 또는 수강생)
        verifySectionAccess(section, currentUser);

        // 자료 목록 조회 (업로드 시간 역순)
        List<LectureResource> resources = resourceRepository.findBySectionIdOrderByUploadAtDesc(sectionId);

        // MaterialResponse로 변환 (Presigned URL 생성)
        return resources.stream()
                .map(resource -> {
                    String presignedUrl = s3StorageService.generatePresignedUrl(
                            resource.getFileUrl(),
                            PRESIGNED_URL_EXPIRATION_MINUTES
                    );
                    return MaterialResponse.from(resource, presignedUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 섹션 자료 업로드
     * @param sectionId 섹션 ID
     * @param file 업로드할 파일
     * @param title 자료 제목 (선택, null이면 파일명 사용)
     * @param description 자료 설명 (선택)
     * @param currentUser 현재 사용자 (강사 권한 필요)
     * @return 업로드된 자료 정보
     */
    @Transactional
    public MaterialResponse uploadMaterial(Long sectionId, MultipartFile file,
                                          String title, String description,
                                          User currentUser) {
        log.info("섹션 ID {}에 자료 업로ద - 파일명: {}, 크기: {} bytes",
                sectionId, file.getOriginalFilename(), file.getSize());

        // 섹션 조회
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("섹션을 찾을 수 없습니다. ID=" + sectionId));

        // 권한 검증
        verifySectionOwnership(section, currentUser);

        // 파일 검증
        validateFile(file);

        // S3에 파일 업로드
        String folderPath = String.format("materials/section-%d", sectionId);
        String fileUrl = s3StorageService.uploadFile(file, folderPath);

        // 제목이 없으면 파일명 사용
        String resourceTitle = (title != null && !title.isBlank())
                ? title
                : file.getOriginalFilename();

        // LectureResource 엔티티 생성 (섹션 자료이므로 lecture는 null)
        LectureResource resource = LectureResource.builder()
                .section(section)
                .lecture(null)  // 섹션 자료
                .title(resourceTitle)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .description(description)
                .sequence(getNextSequence(sectionId))
                .build();

        LectureResource savedResource = resourceRepository.save(resource);

        log.info("자료 업로드 완료 - ID: {}, S3 URL: {}", savedResource.getId(), fileUrl);

        // Presigned URL 생성
        String presignedUrl = s3StorageService.generatePresignedUrl(
                fileUrl,
                PRESIGNED_URL_EXPIRATION_MINUTES
        );

        return MaterialResponse.from(savedResource, presignedUrl);
    }

    /**
     * 자료 메타데이터 수정
     * @param materialId 자료 ID
     * @param request 수정 요청 (제목, 설명)
     * @param currentUser 현재 사용자 (강사 권한 필요)
     * @return 수정된 자료 정보
     */
    @Transactional
    public MaterialResponse updateMaterial(Long materialId, MaterialUpdateRequest request,
                                          User currentUser) {
        log.info("자료 ID {} 메타데이터 수정 - 제목: {}", materialId, request.getTitle());

        // 자료 조회
        LectureResource resource = resourceRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("자료를 찾을 수 없습니다. ID=" + materialId));

        // 권한 검증
        verifySectionOwnership(resource.getSection(), currentUser);

        // 메타데이터 수정 (파일은 그대로)
        resource.updateResource(request.getTitle(), request.getDescription());

        log.info("자료 메타데이터 수정 완료 - ID: {}", materialId);

        // Presigned URL 생성
        String presignedUrl = s3StorageService.generatePresignedUrl(
                resource.getFileUrl(),
                PRESIGNED_URL_EXPIRATION_MINUTES
        );

        return MaterialResponse.from(resource, presignedUrl);
    }

    /**
     * 자료 삭제
     * @param materialId 자료 ID
     * @param currentUser 현재 사용자 (강사 권한 필요)
     * @return 삭제 결과
     */
    @Transactional
    public MaterialDeleteResponse deleteMaterial(Long materialId, User currentUser) {
        log.info("자료 ID {} 삭제 시작", materialId);

        // 자료 조회
        LectureResource resource = resourceRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("자료를 찾을 수 없습니다. ID=" + materialId));

        // 권한 검증
        verifySectionOwnership(resource.getSection(), currentUser);

        String fileUrl = resource.getFileUrl();

        // DB에서 자료 삭제
        resourceRepository.deleteById(materialId);

        // S3에서 파일 삭제
        try {
            s3StorageService.deleteFile(fileUrl);
            log.info("S3 파일 삭제 완료 - URL: {}", fileUrl);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 - URL: {}", fileUrl, e);
            // S3 삭제 실패해도 DB는 이미 삭제됨 (정합성 유지)
            // 향후 별도 배치로 S3 orphan 파일 정리 가능
        }

        log.info("자료 삭제 완료 - ID: {}", materialId);

        return MaterialDeleteResponse.success(materialId);
    }

    /**
     * 섹션 접근 권한 검증 (조회용 - 강사 + 수강생)
     * @param section 섹션
     * @param currentUser 현재 사용자
     * @throws IllegalArgumentException 권한이 없는 경우
     */
    private void verifySectionAccess(Section section, User currentUser) {
        Long courseId = section.getCourse().getId();
        Long userId = currentUser.getId();

        // 1. 강사 본인 체크
        if (isInstructor(section, currentUser)) {
            log.debug("강사 본인 접근 - 섹션 자료 조회 허용: userId={}, sectionId={}",
                    userId, section.getId());
            return;
        }

        // 2. 전체 코스 수강 체크
        if (hasFullCourseAccess(courseId, userId)) {
            log.debug("전체 코스 수강생 접근 - 섹션 자료 조회 허용: userId={}, courseId={}",
                    userId, courseId);
            return;
        }

        // 3. 섹션 구매 체크
        if (hasSectionAccess(section.getId(), userId)) {
            log.debug("섹션 구매 수강생 접근 - 섹션 자료 조회 허용: userId={}, sectionId={}",
                    userId, section.getId());
            return;
        }

        // 권한 없음
        throw new IllegalArgumentException(
                "해당 섹션의 자료를 조회할 권한이 없습니다. 코스 또는 섹션을 구매해주세요.");
    }

    /**
     * 섹션 소유자 권한 검증 (쓰기용 - 강사만)
     * @param section 섹션
     * @param currentUser 현재 사용자
     * @throws IllegalArgumentException 권한이 없는 경우
     */
    private void verifySectionOwnership(Section section, User currentUser) {
        // 강사 프로필이 없으면 권한 없음
        if (currentUser.getInstructorProfile() == null) {
            throw new IllegalArgumentException("강사만 섹션 자료를 업로드/수정/삭제할 수 있습니다.");
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();

        // 섹션이 속한 코스의 강사와 현재 사용자가 일치하지 않으면 권한 없음
        if (!section.getCourse().getInstructor().getId().equals(instructorProfile.getId())) {
            throw new IllegalArgumentException("해당 섹션의 소유자만 자료를 업로드/수정/삭제할 수 있습니다.");
        }
    }

    /**
     * 강사 본인인지 확인
     */
    private boolean isInstructor(Section section, User currentUser) {
        if (currentUser.getInstructorProfile() == null) {
            return false;
        }

        InstructorProfile instructorProfile = currentUser.getInstructorProfile();
        return section.getCourse().getInstructor().getId().equals(instructorProfile.getId());
    }

    /**
     * 전체 코스 수강 여부 확인
     */
    private boolean hasFullCourseAccess(Long courseId, Long userId) {
        return courseEnrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    /**
     * 섹션 구매 여부 확인
     */
    private boolean hasSectionAccess(Long sectionId, Long userId) {
        return lectureOwnershipRepository.findActiveOwnershipsByUserId(userId).stream()
                .filter(ownership -> ownership.isActive())
                .anyMatch(ownership -> ownership.getSection().getId().equals(sectionId));
    }

    /**
     * 파일 검증
     * @param file 업로드할 파일
     * @throws IllegalArgumentException 파일이 유효하지 않은 경우
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 제한: 100MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("파일 크기는 100MB를 초과할 수 없습니다. (현재: %.2fMB)",
                            file.getSize() / (1024.0 * 1024.0))
            );
        }

        // 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }

        // 확장자 검증 (선택사항 - 추후 필요시 활성화)
        // validateFileExtension(originalFilename);
    }

    /**
     * 다음 sequence 번호 조회
     * @param sectionId 섹션 ID
     * @return 다음 sequence 번호
     */
    private Integer getNextSequence(Long sectionId) {
        List<LectureResource> resources = resourceRepository.findBySectionIdOrderBySequenceAsc(sectionId);

        if (resources.isEmpty()) {
            return 1;
        }

        return resources.stream()
                .map(LectureResource::getSequence)
                .filter(seq -> seq != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    // 추후 필요시 활성화
    /*
    private void validateFileExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(
            "pdf", "zip", "rar", "7z",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "java", "py", "js", "html", "css"
        );

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + extension);
        }
    }
    */
}