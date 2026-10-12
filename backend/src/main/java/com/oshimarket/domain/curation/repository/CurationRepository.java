package com.oshimarket.domain.curation.repository;

import com.oshimarket.domain.curation.entity.Curation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurationRepository extends JpaRepository<Curation, Long> {

    /**
     * 필터 없으면 workTag/characterTag 둘 다 null로 넘어와 전체 목록을 반환한다.
     * 판매 기간이 끝난 구매처는 숨기지 않고 맨 뒤로 보낸다 (period_end가 NULL이면 상시 판매).
     */
    @Query("""
            SELECT c FROM Curation c
            WHERE (:workTag IS NULL OR c.workTag = :workTag)
              AND (:characterTag IS NULL OR c.characterTag = :characterTag)
            ORDER BY CASE WHEN c.periodEnd < CURRENT_DATE THEN 1 ELSE 0 END, c.id
            """)
    List<Curation> search(@Param("workTag") String workTag, @Param("characterTag") String characterTag);
}
