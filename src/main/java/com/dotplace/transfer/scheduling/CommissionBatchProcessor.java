package com.dotplace.transfer.scheduling;

import com.dotplace.transfer.transaction.CommissionPolicy;
import com.dotplace.transfer.transaction.TransactionStatus;
import com.dotplace.transfer.transaction.TransferTransactionEntity;
import com.dotplace.transfer.transaction.TransferTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionBatchProcessor {

  private final TransferTransactionRepository repository;
  private final CommissionPolicy commissionPolicy;
  private final Clock clock;

  public CommissionBatchProcessor(
      TransferTransactionRepository repository, CommissionPolicy commissionPolicy, Clock clock) {
    this.repository = repository;
    this.commissionPolicy = commissionPolicy;
    this.clock = clock;
  }

  @Transactional
  public int processNextBatch(int batchSize) {
    List<TransferTransactionEntity> transactions =
        repository.findByCommissionProcessedAtIsNullAndStatusNotOrderByCreatedAt(
            TransactionStatus.PROCESSING, PageRequest.of(0, batchSize));
    Instant processedAt = clock.instant();
    transactions.forEach(transaction -> applyCommission(transaction, processedAt));
    return transactions.size();
  }

  private void applyCommission(TransferTransactionEntity transaction, Instant processedAt) {
    if (transaction.getStatus() == TransactionStatus.SUCCESSFUL) {
      transaction.applyCommission(
          true, commissionPolicy.calculate(transaction.getTransactionFee()), processedAt);
    } else {
      transaction.applyCommission(false, new BigDecimal("0.00"), processedAt);
    }
  }
}
