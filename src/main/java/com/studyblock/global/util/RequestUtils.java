package com.studyblock.global.util;

import jakarta.servlet.http.HttpServletRequest;

// HTTP 요청 관련 공통 유틸리티
// IP 주소, User-Agent, 결제 소스 등 요청 정보 추출
public class RequestUtils {

    // 클라이언트 IP 주소 추출 (프록시 환경 고려)
    // X-Forwarded-For, Proxy-Client-IP 등 다양한 헤더 확인
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // X-Forwarded-For는 여러 IP가 있을 수 있음 (첫 번째 IP 사용)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    // User-Agent 추출
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    // 결제 소스 판단 (WEB, MOBILE, API)
    // User-Agent를 분석하여 접속 환경 판별
    public static String determinePaymentSource(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "API";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
            return "MOBILE";
        }

        return "WEB";
    }
}
