package com.dotplace.transfer.transaction.api;

import com.dotplace.transfer.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    String reference,
    String sourceAccountNumber,
    String destinationAccountNumber,
    String currency,
    BigDecimal amount,
    BigDecimal transactionFee,
    BigDecimal billedAmount,
    String description,
    Instant createdAt,
    TransactionStatus status,
    String statusMessage,
    Boolean commissionWorthy,
    BigDecimal commission,
    Instant commissionProcessedAt) {}
