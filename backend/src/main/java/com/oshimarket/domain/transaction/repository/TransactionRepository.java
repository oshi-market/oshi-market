package com.oshimarket.domain.transaction.repository;

import com.oshimarket.domain.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);

    Page<Transaction> findByBuyerIdOrSellerId(Long buyerId, Long sellerId, Pageable pageable);
}
