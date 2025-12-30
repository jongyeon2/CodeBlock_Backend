package com.studyblock.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Secret Key 생성
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 토큰의 주체
                .claim("type", "access") // Access/Refresh 구분용
                .issuedAt(now) // 발급 시각
                .expiration(expiration) // 만료 시각
                .signWith(getSigningKey()) // SecretKey로 서명
                .compact(); // JWT 문자열로 변환
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 토큰의 주체
                .claim("type", "refresh") // Access/Refresh 구분용
                .issuedAt(now) // 발급 시각
                .expiration(expiration) // 만료 시각
                .signWith(getSigningKey()) // SecretKey로 서명
                .compact(); // JWT 문자열로 변환
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                // JWT를 검증할 때 사용할 SecretKey 등록
                // AccessToken/RefreshToken 생성시
                // .signWith()에 사용했던 같은 키여야 함
                .verifyWith(getSigningKey())
                .build()

                // 전달 받은 token 문자열을 해석하고,
                // Header + Payload + Signature 세 부분을 분리해 검증
                .parseSignedClaims(token)
                .getPayload(); // 실제 JWT의 본문 부분(payload)을 가져옴

        // 토큰을 생성할 때 .subject(String.valueOf(userId))로 넣은 값(sub) 꺼냄
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}
