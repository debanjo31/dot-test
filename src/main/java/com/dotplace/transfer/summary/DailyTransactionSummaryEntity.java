package com.dotplace.transfer.summary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_transaction_summaries")
public class DailyTransactionSummaryEntity {

  @Id
  @Column(name = "summary_date")
  private LocalDate summaryDate;

  @Column(name = "total_count", nullable = false)
  private long totalCount;

  @Column(name = "successful_count", nullable = false)
  private long successfulCount;

  @Column(name = "insufficient_funds_count", nullable = false)
  private long insufficientFundsCount;

  @Column(name = "failed_count", nullable = false)
  private long failedCount;

  @Column(name = "total_amount", nullable = false, precision = 21, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "total_fees", nullable = false, precision = 21, scale = 2)
  private BigDecimal totalFees;

  @Column(name = "total_billed_amount", nullable = false, precision = 21, scale = 2)
  private BigDecimal totalBilledAmount;

  @Column(name = "total_commission", nullable = false, precision = 21, scale = 2)
  private BigDecimal totalCommission;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  protected DailyTransactionSummaryEntity() {}

  public DailyTransactionSummaryEntity(
      LocalDate summaryDate, SummaryTotals totals, Instant generatedAt) {
    this.summaryDate = summaryDate;
    update(totals, generatedAt);
  }

  public void update(SummaryTotals totals, Instant generatedAt) {
    totalCount = totals.totalCount();
    successfulCount = totals.successfulCount();
    insufficientFundsCount = totals.insufficientFundsCount();
    failedCount = totals.failedCount();
    totalAmount = totals.totalAmount();
    totalFees = totals.totalFees();
    totalBilledAmount = totals.totalBilledAmount();
    totalCommission = totals.totalCommission();
    this.generatedAt = generatedAt;
  }

  public LocalDate getSummaryDate() {
    return summaryDate;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public long getSuccessfulCount() {
    return successfulCount;
  }

  public long getInsufficientFundsCount() {
    return insufficientFundsCount;
  }

  public long getFailedCount() {
    return failedCount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getTotalFees() {
    return totalFees;
  }

  public BigDecimal getTotalBilledAmount() {
    return totalBilledAmount;
  }

  public BigDecimal getTotalCommission() {
    return totalCommission;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }
}
