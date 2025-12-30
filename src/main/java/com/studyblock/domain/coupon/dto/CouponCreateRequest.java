package com.studyblock.domain.coupon.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.studyblock.domain.coupon.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequest {
    private String name;
    private String description;
    private CouponType type;
    private Integer discountValue;
    private Integer minimumAmount;
    private Integer maximumDiscount;
    
    // 날짜 형식 유연하게 처리 (yyyy-MM-dd 또는 yyyy-MM-ddTHH:mm:ss)
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime validFrom;
    
    // validUntil은 날짜만 받으면 23:59:59로 설정
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializerForUntil.class)
    private LocalDateTime validUntil;
    
    private Integer usageLimit;
    private Boolean isActive;
    
    // 커스텀 LocalDateTime Deserializer
    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText().trim();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }
            
            // yyyy-MM-dd 형식인 경우 (날짜만)
            if (dateStr.length() == 10 && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    // validFrom은 00:00:00, validUntil은 23:59:59로 설정
                    // 필드명으로 구분할 수 없으므로 validUntil인 경우만 MAX로 설정
                    // 하지만 필드명 접근이 어려우므로 기본값으로 MIN 사용
                    return date.atTime(LocalTime.MIN);
                } catch (DateTimeParseException e) {
                    throw new IOException("날짜 형식이 올바르지 않습니다: " + dateStr, e);
                }
            }
            
            // yyyy-MM-ddTHH:mm:ss 형식인 경우
            if (dateStr.contains("T")) {
                try {
                    if (dateStr.contains(".")) {
                        return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER_WITH_MILLIS);
                    } else {
                        return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
                    }
                } catch (DateTimeParseException e) {
                    // 기본 ISO 형식으로 시도
                    try {
                        return LocalDateTime.parse(dateStr);
                    } catch (DateTimeParseException e2) {
                        throw new IOException("날짜 시간 형식이 올바르지 않습니다: " + dateStr, e2);
                    }
                }
            }
            
            // 기본 ISO 형식으로 시도
            try {
                return LocalDateTime.parse(dateStr);
            } catch (DateTimeParseException e) {
                throw new IOException("날짜 형식이 올바르지 않습니다: " + dateStr, e);
            }
        }
    }
    
    // validUntil용 커스텀 LocalDateTime Deserializer (날짜만 받으면 23:59:59로 설정)
    public static class FlexibleLocalDateTimeDeserializerForUntil extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText().trim();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }
            
            // yyyy-MM-dd 형식인 경우 (날짜만) - validUntil은 23:59:59로 설정
            if (dateStr.length() == 10 && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    return date.atTime(LocalTime.MAX);
                } catch (DateTimeParseException e) {
                    throw new IOException("날짜 형식이 올바르지 않습니다: " + dateStr, e);
                }
            }
            
            // yyyy-MM-ddTHH:mm:ss 형식인 경우
            if (dateStr.contains("T")) {
                try {
                    if (dateStr.contains(".")) {
                        return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER_WITH_MILLIS);
                    } else {
                        return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
                    }
                } catch (DateTimeParseException e) {
                    // 기본 ISO 형식으로 시도
                    try {
                        return LocalDateTime.parse(dateStr);
                    } catch (DateTimeParseException e2) {
                        throw new IOException("날짜 시간 형식이 올바르지 않습니다: " + dateStr, e2);
                    }
                }
            }
            
            // 기본 ISO 형식으로 시도
            try {
                return LocalDateTime.parse(dateStr);
            } catch (DateTimeParseException e) {
                throw new IOException("날짜 형식이 올바르지 않습니다: " + dateStr, e);
            }
        }
    }
}
