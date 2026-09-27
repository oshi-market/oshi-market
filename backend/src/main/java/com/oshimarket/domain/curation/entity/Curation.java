package com.oshimarket.domain.curation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 작품/캐릭터별 구매처 큐레이션. item과는 FK 없이 work_tag/character_tag 문자열 매칭으로만
 * 연결한다 (3_아키텍처및서비스흐름.md). MVP 범위에 등록/수정 API가 없는 조회 전용 콘텐츠라
 * 도메인 로직 없이 단순 데이터 홀더로 둔다.
 */
@Entity
@Table(name = "curation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_tag", nullable = false, length = 50)
    private String workTag;

    @Column(name = "character_tag", length = 50)
    private String characterTag;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;
}
