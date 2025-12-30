package com.studyblock.domain.auth.service;

import com.studyblock.domain.auth.dto.LocalSignupRequest;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.enums.JoinType;
import com.studyblock.domain.user.repository.UserRepository;
import com.studyblock.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private LocalSignupRequest request;
    private User user;

    @BeforeEach
    void 테스트_데이터_준비() {
        // 테스트용 회원가입 요청 데이터 준비
        request = new LocalSignupRequest();
        request.setMemberId("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123!");
        request.setName("홍길동");
        request.setBirth(LocalDate.of(2000, 1, 1));
        request.setPhone("01012345678");
        request.setGender(1); // MALE

        // 테스트용 User 엔티티 준비
        user = User.builder()
                .memberId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .birth(LocalDate.of(2000, 1, 1))
                .phone("01012345678")
                .gender(1)
                .jointype(JoinType.LOCAL.getValue())
                .oauthProviderId(null)
                .nickname("홍길동")
                .intro("")
                .img(null)
                .interests("")
                .build();
    }

    @Test
    void 로컬_회원가입_성공() {
        // given - Mock 동작 정의
        given(userRepository.findByMemberId(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("accessToken123");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refreshToken123");

        // when - 실제 메서드 호출
        Map<String, String> result = authService.registerLocalUser(request);

        // then - 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo("accessToken123");
        assertThat(result.get("refreshToken")).isEqualTo("refreshToken123");

        // 메서드 호출 횟수 검증
        verify(userRepository, times(1)).findByMemberId("testuser");
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123!");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).createAccessToken(any());
        verify(jwtTokenProvider, times(1)).createRefreshToken(any());
    }

    @Test
    void 아이디가_중복되면_예외_발생() {
        // given - 이미 존재하는 아이디
        given(userRepository.findByMemberId("testuser")).willReturn(Optional.of(user));

        // when & then - 예외 발생 확인
        assertThatThrownBy(() -> authService.registerLocalUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");

        // save()가 호출되지 않았는지 확인
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 이메일이_중복되면_예외_발생() {
        // given
        given(userRepository.findByMemberId(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.registerLocalUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        // save()가 호출되지 않았는지 확인
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 사용_가능한_아이디_확인() {
        // given
        given(userRepository.findByMemberId("newuser")).willReturn(Optional.empty());

        // when
        boolean result = authService.isMemberIdAvailable("newuser");

        // then
        assertThat(result).isTrue();
        verify(userRepository, times(1)).findByMemberId("newuser");
    }

    @Test
    void 이미_사용중인_아이디_확인() {
        // given
        given(userRepository.findByMemberId("testuser")).willReturn(Optional.of(user));

        // when
        boolean result = authService.isMemberIdAvailable("testuser");

        // then
        assertThat(result).isFalse();
        verify(userRepository, times(1)).findByMemberId("testuser");
    }

    @Test
    void 사용_가능한_이메일_확인() {
        // given
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());

        // when
        boolean result = authService.isEmailAvailable("new@example.com");

        // then
        assertThat(result).isTrue();
        verify(userRepository, times(1)).findByEmail("new@example.com");
    }

    @Test
    void 이미_사용중인_이메일_확인() {
        // given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));

        // when
        boolean result = authService.isEmailAvailable("test@example.com");

        // then
        assertThat(result).isFalse();
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void 이메일_인증_코드_전송() {
        // when - 예외가 발생하지 않는지 확인
        assertThatCode(() -> authService.sendVerificationCode("test@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void 이메일_인증_코드_확인_성공() {
        // when
        boolean result = authService.verifyEmailCode("test@example.com", "123456");

        // then - 임시로 항상 true 반환
        assertThat(result).isTrue();
    }

    @Test
    void 비밀번호가_암호화되어_저장됨() {
        // given
        given(userRepository.findByMemberId(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123!")).willReturn("encodedPassword123");
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refreshToken");

        // when
        authService.registerLocalUser(request);

        // then - 비밀번호 암호화 메서드가 호출되었는지 확인
        verify(passwordEncoder, times(1)).encode("password123!");
    }

    @Test
    void JWT_토큰이_정상적으로_발급됨() {
        // given
        given(userRepository.findByMemberId(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("testAccessToken");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("testRefreshToken");

        // when
        Map<String, String> result = authService.registerLocalUser(request);

        // then
        verify(jwtTokenProvider, times(1)).createAccessToken(any());
        verify(jwtTokenProvider, times(1)).createRefreshToken(any());
        assertThat(result).containsKeys("accessToken", "refreshToken");
    }
}