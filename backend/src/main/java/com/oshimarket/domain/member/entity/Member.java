package com.oshimarket.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회원 엔티티. 테이블명은 USER가 PostgreSQL 예약어라 user_account 사용
 * (4_DB분석서.md 참고).
 */
@Entity
@Table(name = "user_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt로 해싱된 값만 저장. 평문 저장 금지 (4_DB분석서.md). */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    /** 판매자 경험치. 구매자가 준 별점에 따라 오르내리고(최소 0), 레벨은 SellerLevel이 계산 (V7). */
    @Column(name = "exp", nullable = false)
    private int exp;

    /** 받은 별점 개수·합계 (평균 별점 = ratingSum / reviewCount). */
    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "rating_sum", nullable = false)
    private int ratingSum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public static Member of(String email, String encodedPassword, String nickname) {
        return new Member(email, encodedPassword, nickname);
    }

    /**
     * 이메일은 대소문자를 구분하지 않으므로 앞뒤 공백을 지우고 소문자로 맞춰 저장·조회한다.
     * (Test@a.com과 test@a.com이 다른 계정으로 가입되는 것 방지. DB에도 같은 규칙의 CHECK 제약이 있음 — V6)
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 프로필 수정(MVP 범위: 닉네임만) — 1_MVP기획서.md "최소 필드만 구현". */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
