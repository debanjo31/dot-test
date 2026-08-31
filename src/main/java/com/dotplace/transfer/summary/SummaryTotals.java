package com.dotplace.transfer.summary;

import java.math.BigDecimal;

public record SummaryTotals(
    long totalCount,
    long successfulCount,
    long insufficientFundsCount,
    long failedCount,
    BigDecimal totalAmount,
    BigDecimal totalFees,
    BigDecimal totalBilledAmount,
    BigDecimal totalCommission) {}
