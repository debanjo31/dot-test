package com.dotplace.transfer.summary;

import com.dotplace.transfer.summary.api.DailySummaryResponse;
import com.dotplace.transfer.transaction.InvalidTransferException;
import com.dotplace.transfer.transaction.TransferTransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySummaryService {

  private final DailyTransactionSummaryRepository summaryRepository;
  private final TransactionSummaryQueryRepository queryRepository;
  private final TransferTransactionRepository transactionRepository;
  private final Clock clock;
  private final ZoneId businessZone;
  private final String currency;

  public DailySummaryService(
      DailyTransactionSummaryRepository summaryRepository,
      TransactionSummaryQueryRepository queryRepository,
      TransferTransactionRepository transactionRepository,
      Clock clock,
      @Value("${app.business-zone:Africa/Lagos}") String businessZone,
      @Value("${app.currency:NGN}") String currency) {
    this.summaryRepository = summaryRepository;
    this.queryRepository = queryRepository;
    this.transactionRepository = transactionRepository;
    this.clock = clock;
    this.businessZone = ZoneId.of(businessZone);
    this.currency = currency;
  }

  @Transactional(readOnly = true)
  public DailySummaryResponse get(LocalDate date) {
    LocalDate today = LocalDate.now(clock.withZone(businessZone));
    if (date.isAfter(today)) {
      throw new InvalidTransferException("Summary date cannot be in the future");
    }
    if (date.isBefore(today)) {
      return summaryRepository
          .findById(date)
          .map(entity -> toResponse(entity, true))
          .orElseGet(() -> liveSummary(date));
    }
    return liveSummary(date);
  }

  @Transactional
  public int materializeMissingPastSummaries() {
    LocalDate yesterday = LocalDate.now(clock.withZone(businessZone)).minusDays(1);
    LocalDate earliest =
        transactionRepository
            .findEarliestCreatedAt()
            .map(instant -> instant.atZone(businessZone).toLocalDate())
            .orElse(yesterday);
    if (earliest.isAfter(yesterday)) {
      earliest = yesterday;
    }

    int generated = 0;
    for (LocalDate date = earliest; !date.isAfter(yesterday); date = date.plusDays(1)) {
      if (summaryRepository.existsById(date) && !date.equals(yesterday)) {
        continue;
      }
      LocalDate summaryDate = date;
      SummaryTotals totals = calculate(summaryDate);
      DailyTransactionSummaryEntity summary =
          summaryRepository
              .findById(summaryDate)
              .orElseGet(
                  () -> new DailyTransactionSummaryEntity(summaryDate, totals, clock.instant()));
      summary.update(totals, clock.instant());
      summaryRepository.save(summary);
      generated++;
    }
    return generated;
  }

  @Transactional
  public DailySummaryResponse regenerate(LocalDate date) {
    SummaryTotals totals = calculate(date);
    DailyTransactionSummaryEntity summary =
        summaryRepository
            .findById(date)
            .orElseGet(() -> new DailyTransactionSummaryEntity(date, totals, clock.instant()));
    summary.update(totals, clock.instant());
    return toResponse(summaryRepository.save(summary), true);
  }

  private DailySummaryResponse liveSummary(LocalDate date) {
    return toResponse(date, calculate(date), clock.instant(), false);
  }

  private SummaryTotals calculate(LocalDate date) {
    Instant start = date.atStartOfDay(businessZone).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(businessZone).toInstant();
    return queryRepository.calculate(start, end);
  }

  private DailySummaryResponse toResponse(
      DailyTransactionSummaryEntity entity, boolean materialized) {
    return new DailySummaryResponse(
        entity.getSummaryDate(),
        currency,
        entity.getTotalCount(),
        entity.getSuccessfulCount(),
        entity.getInsufficientFundsCount(),
        entity.getFailedCount(),
        entity.getTotalAmount(),
        entity.getTotalFees(),
        entity.getTotalBilledAmount(),
        entity.getTotalCommission(),
        entity.getGeneratedAt(),
        materialized);
  }

  private DailySummaryResponse toResponse(
      LocalDate date, SummaryTotals totals, Instant generatedAt, boolean materialized) {
    return new DailySummaryResponse(
        date,
        currency,
        totals.totalCount(),
        totals.successfulCount(),
        totals.insufficientFundsCount(),
        totals.failedCount(),
        totals.totalAmount(),
        totals.totalFees(),
        totals.totalBilledAmount(),
        totals.totalCommission(),
        generatedAt,
        materialized);
  }
}
