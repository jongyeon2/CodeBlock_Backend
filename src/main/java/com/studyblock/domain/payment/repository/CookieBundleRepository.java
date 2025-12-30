package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.CookieBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CookieBundleRepository extends JpaRepository<CookieBundle, Long> {
    
    // 활성화된 쿠키 번들 조회
    List<CookieBundle> findByIsActiveTrue();
    
    // 활성화된 쿠키 번들 조회 (이름순 정렬)
    List<CookieBundle> findByIsActiveTrueOrderByNameAsc();
    
    // 특정 가격 범위의 활성화된 번들 조회
    List<CookieBundle> findByIsActiveTrueAndPriceBetween(Long minPrice, Long maxPrice);
}
