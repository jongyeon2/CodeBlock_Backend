package com.studyblock.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.studyblock.domain.auth.entity.UserRole;
import com.studyblock.domain.common.BaseTimeEntity;
import com.studyblock.domain.user.enums.Gender;
import com.studyblock.domain.user.enums.JoinType;
import com.studyblock.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseTimeEntity {

    // PK AUTO INCREMENT와 유사하다
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 회원 ID -> UNIQUE로 설정 다 계정일 경우 제거해야함
    @Column(name = "member_id", nullable = false, unique = true)
    private String memberId;

    // 사용자 비밀번호
    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    // 사용자 전화번호
    @Column(nullable = false)
    private String phone;

    // 사용자 이메일 (OAuth2 또는 로컬 이메일)
    @Column(length = 100)
    private String email;

    // 사용자 생년월일
    @Column(nullable = false)
    private LocalDate birth;

    // 사용자 성별
    // 여기서는 TINYINT지만 아래에 있는 GetGenderENum()에서 변환해준다.
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer gender;

    // 활동/정지 등 상태 코드 정수 저장
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status = UserStatus.ACTIVE.getValue();

    // 유저의 가입경로 (TINYINT로 저장)
    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer jointype;

    // OAuth2 제공자별 고유 ID (카카오/구글/네이버에서 발급한 ID)
    @Column(name = "oauth_provider_id", length = 100)
    private String oauthProviderId;

    // 크리에이터 모드 ON/OFF
    @Column(name = "is_creator", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isCreator = false;

    // 사용자 닉네임
    private String nickname;

    // 사용자 소개
    private String intro;

    // 사용자 프로필 이미지
    private String img;

    // 사용자 관심사
    private String interests;

    // 유저는 하나의 강사 프로필을 가질 수 있다. 1:1
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private InstructorProfile instructorProfile;

    // 유저는 여러 역할(Role)을 가질 수 있다. 1:N
    // LAZY 로딩이므로 명시적으로 조회하지 않으면 쿼리 발생하지 않음
    // @JsonIgnore: Jackson 직렬화 시 순환 참조 방지 (User → userRoles → UserRole → user → User 무한 루프 방지)
    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

    // 유저는 여러 관심 카테고리를 가질 수 있다. 1:N
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.studyblock.domain.category.entity.UserCategory> userCategories = new ArrayList<>();

    // Builder어노테이션으로 스트림 형식으로 사용할 수 있게 만듬
    @Builder
    public User(Long id, String name, String memberId, String password, String phone, String email,
                LocalDate birth, Integer gender, Integer jointype, String oauthProviderId,
                String nickname, String intro, String img, String interests) {
        this.id = id;
        this.name = name;
        this.memberId = memberId;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.birth = birth;
        this.gender = gender;
        this.jointype = jointype;
        this.oauthProviderId = oauthProviderId;
        this.nickname = nickname;
        this.intro = intro;
        this.img = img;
        this.interests = interests;
        this.status = UserStatus.ACTIVE.getValue();
        this.isCreator = false;
    }

    // Business methods -> TinyInt에 있는 값에 이름표 달아주기
    public void updateStatus(UserStatus userStatus) {
        this.status = userStatus.getValue();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void promoteToCreator() {
        this.isCreator = true;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE.getValue();
    }

    public Gender getGenderEnum() {
        return Gender.fromValue(this.gender);
    }

    public UserStatus getStatusEnum() {
        return UserStatus.fromValue(this.status);
    }

    public JoinType getJoinTypeEnum() {
        return JoinType.fromValue(this.jointype);
    }

    // OAuth2 로그인 여부 확인
    public boolean isOAuthUser() {
        return this.oauthProviderId != null && !this.oauthProviderId.isEmpty();
    }

    /**
     * OAuth2 사용자 생성 (정적 팩토리 메서드)
     * - UserProfile 테이블과 병합되어 프로필 정보도 함께 초기화
     */
    public static User createOAuth2User(JoinType joinType, String oauthProviderId,
                                        String email, String name, String profileImageUrl,
                                        LocalDate birth, Integer gender) {
        return User.builder()
                .name(name)
                .memberId(joinType.name().toLowerCase() + "_" + oauthProviderId) // 예: kakao_1234567890
                .password("") // OAuth2는 비밀번호 없음
                .phone("") // 초기값
                .email(email)
                .birth(birth)
                .gender(gender)
                .jointype(joinType.getValue())
                .oauthProviderId(oauthProviderId)
                // 프로필 정보 초기화 (UserProfile 테이블 병합으로 추가)
                .nickname(name) // 초기 닉네임은 이름과 동일
                .intro("") // 빈 자기소개
                .img(profileImageUrl) // SNS 프로필 이미지
                .interests("") // 빈 관심사
                .build();
    }

    /**
     * OAuth2 재로그인 시 정보 업데이트
     */
    public void updateOAuth2Info(String email, String name) {
        if (email != null && !email.isEmpty()) {
            this.email = email;
        }
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
    }

    /**
     * OAuth 제공자 정보 업데이트 (여러 SNS 로그인 지원)
     * - 최초 가입 경로(jointype)는 변경하지 않음
     * - oauth_provider_id만 최신으로 업데이트
     */
    public void updateOAuthProvider(JoinType providerType, String oauthProviderId) {
        this.oauthProviderId = oauthProviderId;
    }

    // 프로필 정보 업데이트
    public void updateProfile(String nickname, String intro, String img, String interests, LocalDate birth) {
        this.nickname = nickname;
        this.intro = intro;
        this.img = img;
        this.interests = interests;
        this.birth = birth;
    }

    //프로필 정보 업데이트 (이미지 제외)
    public void updateProfileWithoutImage(String nickname, String intro, String interests, LocalDate birth){
        this.nickname = nickname;
        this.intro = intro;
        this.interests = interests;
        this.birth = birth;
    }

    // 닉네임 업데이트
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 소개 업데이트
    public void updateIntro(String intro) {
        this.intro = intro;
    }

    // 프로필 이미지 업데이트
    public void updateImg(String img) {
        this.img = img;
    }

    // 관심사 업데이트
    public void updateInterests(String interests) {
        this.interests = interests;
    }

    // 유저 상태 변경
    public void updateStatus(Integer status) {
        this.status = status;
    }
}
