package com.oshimarket.domain.work.repository;

import com.oshimarket.domain.work.entity.Work;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRepository extends JpaRepository<Work, Long> {

    /** name 컬럼이 한국어 collation(V4)이라 DB 정렬만으로 가나다순. */
    List<Work> findAllByOrderByNameAsc();

    boolean existsByName(String name);
}
