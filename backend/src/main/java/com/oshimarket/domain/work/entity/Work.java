package com.oshimarket.domain.work.entity;

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
 * 작품 마스터. 상품 등록 시 작품명 드롭다운 목록 + 입력값 검증용.
 * item/curation의 work_tag는 FK가 아닌 문자열로 두고 이 테이블의 name과 맞춘다 (4_DB분석서.md 참고).
 * 작품 추가는 코드 수정 없이 DB에 INSERT.
 */
@Entity
@Table(name = "work")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Work(String name) {
        this.name = name;
    }

    public static Work of(String name) {
        return new Work(name);
    }
}
