package com.oshimarket.domain.review.repository;

import com.oshimarket.domain.review.entity.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTransactionId(Long transactionId);

    /** 내 거래 내역 한 페이지에 대해 별점 여부를 한 번에 조회 (N+1 방지). */
    List<Review> findByTransactionIdIn(Collection<Long> transactionIds);
}
