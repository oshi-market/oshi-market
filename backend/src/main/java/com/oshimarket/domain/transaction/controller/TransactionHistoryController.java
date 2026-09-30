package com.oshimarket.domain.transaction.controller;

import com.oshimarket.domain.transaction.dto.TransactionHistoryResponse;
import com.oshimarket.domain.transaction.dto.TransactionRole;
import com.oshimarket.domain.transaction.service.TransactionHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 내 거래 내역 조회. 로그인 필요 (SecurityConfig의 anyRequest().authenticated()). */
@RestController
@RequestMapping("/api/members/me/transactions")
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionHistoryController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    /** role=BUY(구매) / SELL(판매), 생략하면 전체. */
    @GetMapping
    public Page<TransactionHistoryResponse> getMyTransactions(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) TransactionRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return transactionHistoryService.getMyTransactions(memberId, role, pageable);
    }
}
