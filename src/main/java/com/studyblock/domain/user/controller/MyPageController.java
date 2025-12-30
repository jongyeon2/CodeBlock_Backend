package com.studyblock.domain.user.controller;

import com.studyblock.domain.community.dto.PostResponse;
import com.studyblock.domain.course.dto.CourseReviewResponse;
import com.studyblock.domain.user.dto.*;
import com.studyblock.domain.course.dto.CourseResponse;
import com.studyblock.domain.coupon.dto.UserCouponResponseDTO;
import com.studyblock.domain.user.dto.OwnedSectionResponse;
import com.studyblock.domain.user.dto.UserProfileResponse;
import com.studyblock.domain.user.dto.UserUpdateDTO;
import com.studyblock.domain.user.service.MyPageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {
    //마이페이지 컨트롤러
    private final MyPageService myPageService;

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    //유저 정보 가져오기
    @GetMapping("/user/{userId}")
    public UserProfileResponse getUserProfile(@PathVariable Long userId) {
        return myPageService.getUserProfile(userId);
    }

    //유저 정보 수정하기
    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateUserProfile(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        myPageService.updateUser(id, dto);
    }

    //유저의 찜 목록 불러오기
    @GetMapping("/wishlist/{id}")
    public List<CourseResponse> getWishlistCourses(@PathVariable Long id){
        return myPageService.getWishlistCourses(id);
    }

    //찜 목록에서 삭제하기
    @DeleteMapping("/rmwish/{userId}/{courseId}")
    public void removeFromWishlist(@PathVariable Long userId, @PathVariable Long courseId) {
        myPageService.removeWish(userId, courseId);
    }

    //찜 토글 (찜 추가/해제)
    @PostMapping("/wishlist/{userId}/{courseId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable Long userId, @PathVariable Long courseId) {
        boolean isAdded = myPageService.toggleWishlist(userId, courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("isWishlisted", isAdded);
        response.put("message", isAdded ? "찜 목록에 추가되었습니다." : "찜 목록에서 제거되었습니다.");
        return ResponseEntity.ok(response);
    }

    //찜 여부 확인
    @GetMapping("/wishlist/{userId}/check/{courseId}")
    public ResponseEntity<Map<String, Boolean>> checkWishlist(@PathVariable Long userId, @PathVariable Long courseId) {
        boolean isWishlisted = myPageService.isInWishlist(userId, courseId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isWishlisted", isWishlisted);
        return ResponseEntity.ok(response);
    }

    //유저 탈퇴
    @PutMapping(value = "/withdraw/{id}")
    public void withdraw(@PathVariable Long id) {
        myPageService.withdraw(id);
    }

    // 보유 강의 불러오기
    @GetMapping(value = "/mysection/{userId}")
    public List<OwnedSectionResponse> getUserOwn(@PathVariable Long userId){
        return myPageService.getUserOwn(userId);
    }

    //유저 비밀번호 변경
    // 1. 비밀번호 검증
    @PostMapping("/password/verify")
    public ResponseEntity<PasswordVerifyResponse> verifyCurrentPassword(@RequestHeader("User-Id") Long userId, @RequestBody PasswordVerifyRequest request){
        String token = myPageService.verifyCurrentPassword(userId, request.getCurrentPassword());
        return ResponseEntity.ok(new PasswordVerifyResponse(token));
    }
    // 2. 비밀번호 변경
    @PutMapping("/password/change")
    public ResponseEntity<Map<String, String>> changePassword(@RequestHeader("User-Id") Long userId, @RequestBody PasswordChangeRequest request){
        myPageService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    //프로필 이미지 변경
    @PostMapping("/upload-profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestHeader("User-Id") Long userId, @RequestParam("file") MultipartFile file){
        String imageUrl = myPageService.uploadProfileImage(userId, file);
        Map<String, String> response = new HashMap<>();
        response.put("url", imageUrl);
        response.put("message","프로필 이미지 업로드 완료");

        return ResponseEntity.ok(response);
    }

    //나의 문의 내역
    @GetMapping("/myQnA/{userId}")
    public ResponseEntity<List<PostResponse>> getMyQnA(@PathVariable Long userId){
        List<PostResponse> posts = myPageService.getMyQnA(userId);
        return ResponseEntity.ok(posts);
    }



    //유저 보유 쿠폰 조회
    // GET /api/mypage/coupons/{userId} 요청시 유저의 보유 쿠폰 목록 조회
    // return 유저의 보유 쿠폰 목록
    @GetMapping("/coupons/{userId}")
    public ResponseEntity<Map<String, Object>> getUserCoupons(@PathVariable Long userId) {
        try {
            log.info("유저 쿠폰 목록 조회 요청 - userId: {}", userId);

            // 사용자 쿠폰 조회
            List<UserCouponResponseDTO> coupons = myPageService.getUserCoupons(userId);

            log.info("유저 쿠폰 목록 조회 완료 - userId: {}, count: {}", userId, coupons != null ? coupons.size() : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "쿠폰 목록을 조회했습니다");
            response.put("data", coupons != null ? coupons : List.of());
            response.put("count", coupons != null ? coupons.size() : 0);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("유저 쿠폰 목록 조회 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (IllegalStateException e) {
            log.error("유저 쿠폰 목록 조회 중 데이터 불일치 - userId: {}, error: {}", userId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            log.error("유저 쿠폰 목록 조회 중 오류 발생 - userId: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "쿠폰 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //마이페이지 내가 작성한 강의평 목록 조회
    @GetMapping("/reviews/{userId}")
    public ResponseEntity<List<CourseReviewResponse>> getMyReviews(@PathVariable Long userId) {
        List<CourseReviewResponse> reviews = myPageService.getMyReviews(userId);
        return ResponseEntity.ok(reviews);
    }
}
