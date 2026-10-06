package com.oshimarket.domain.transaction.controller;

import com.oshimarket.domain.transaction.dto.TransactionResponse;
import com.oshimarket.domain.transaction.dto.TransactionStatusRequest;
import com.oshimarket.domain.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 거래 요청·조회·상태 변경 API (내 거래 내역 조회는 TransactionHistoryController)
@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/chatrooms/{chatRoomId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse request(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long memberId) {
        return transactionService.request(chatRoomId, memberId);
    }

    @GetMapping("/transactions/{transactionId}")
    public TransactionResponse get(@PathVariable Long transactionId, @AuthenticationPrincipal Long memberId) {
        return transactionService.get(transactionId, memberId);
    }

    @PatchMapping("/transactions/{transactionId}/status")
    public TransactionResponse changeStatus(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TransactionStatusRequest request
    ) {
        return transactionService.changeStatus(transactionId, memberId, request.status());
    }
}
