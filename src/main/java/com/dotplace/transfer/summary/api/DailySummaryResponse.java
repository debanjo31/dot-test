package com.dotplace.transfer.summary.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailySummaryResponse(
    LocalDate date,
    String currency,
    long totalCount,
    long successfulCount,
    long insufficientFundsCount,
    long failedCount,
    BigDecimal totalAmount,
    BigDecimal totalFees,
    BigDecimal totalBilledAmount,
    BigDecimal totalCommission,
    Instant generatedAt,
    boolean materialized) {}
