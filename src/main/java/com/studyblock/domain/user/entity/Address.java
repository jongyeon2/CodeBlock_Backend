package com.studyblock.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 10)
    private String zipcode;

    @Column(name = "base_address")
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Builder
    public Address(User user, String zipcode, String baseAddress, String detailAddress) {
        this.user = user;
        this.zipcode = zipcode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    // Business methods
    public void updateAddress(String zipcode, String baseAddress, String detailAddress) {
        this.zipcode = zipcode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    public String getFullAddress() {
        return String.format("%s %s", baseAddress != null ? baseAddress : "",
                                      detailAddress != null ? detailAddress : "").trim();
    }
}
