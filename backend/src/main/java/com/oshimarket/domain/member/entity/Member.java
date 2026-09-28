package com.oshimarket.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.trustScore = 0;
    }

    public static Member of(String email, String encodedPassword, String nickname) {
        return new Member(email, encodedPassword, nickname);
    }

    /** 프로필 수정(MVP 범위: 닉네임만) — 1_MVP기획서.md "최소 필드만 구현". */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
