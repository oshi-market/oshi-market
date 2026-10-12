package com.oshimarket.domain.transaction.dto;

import com.oshimarket.domain.transaction.entity.TransactionStatus;
import jakarta.validation.constraints.NotNull;

/** PATCH /api/transactions/{transactionId}/status 요청 본문. 예: {"status": "IN_PROGRESS"} */
public record TransactionStatusRequest(@NotNull TransactionStatus status) {
}
