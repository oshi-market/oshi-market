package com.oshimarket.domain.transaction.repository;

import com.oshimarket.domain.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);

    Page<Transaction> findByBuyerIdOrSellerId(Long buyerId, Long sellerId, Pageable pageable);

    /**
     * 상태 변경 시 거래 row를 SELECT ... FOR UPDATE로 잠근다. 판매자 '완료'와 구매자 '취소'가 동시에 들어와도
     * 두 번째 요청은 바뀐 상태를 보고 canChangeTo에서 거절된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);
}
