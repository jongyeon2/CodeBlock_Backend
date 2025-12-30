package com.studyblock.domain.enrollment.enums;

/**
 * 수강신청 출처
 * Source/origin of how the enrollment was created
 */
public enum EnrollmentSource {
    /**
     * 현금 결제로 구매
     * Purchased with real money (cash/card)
     */
    PURCHASE_CASH("현금 구매", "Purchased with cash payment"),

    /**
     * 쿠키(가상화폐)로 구매
     * Purchased with platform virtual currency (cookies)
     */
    PURCHASE_COOKIE("쿠키 구매", "Purchased with cookies"),

    /**
     * 관리자가 부여
     * Granted by administrator
     */
    ADMIN_GRANT("관리자 부여", "Granted by admin"),

    /**
     * 프로모션/이벤트로 무료 제공
     * Free promotional access
     */
    PROMOTIONAL("프로모션", "Promotional access");

    private final String koreanName;
    private final String description;

    EnrollmentSource(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 유료 구매인지 확인
     * Check if enrollment was purchased (not free)
     */
    public boolean isPurchased() {
        return this == PURCHASE_CASH || this == PURCHASE_COOKIE;
    }

    /**
     * 현금 결제인지 확인
     * Check if purchased with real money
     */
    public boolean isCashPurchase() {
        return this == PURCHASE_CASH;
    }

    /**
     * 쿠키 결제인지 확인
     * Check if purchased with cookies
     */
    public boolean isCookiePurchase() {
        return this == PURCHASE_COOKIE;
    }

    /**
     * 무료로 부여받은 것인지 확인
     * Check if enrollment was granted for free
     */
    public boolean isFree() {
        return this == ADMIN_GRANT || this == PROMOTIONAL;
    }
}
