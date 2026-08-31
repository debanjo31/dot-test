package com.dotplace.transfer.transaction.api;

import com.dotplace.transfer.transaction.TransferTransactionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  private final String currency;

  public TransactionMapper(@Value("${app.currency:NGN}") String currency) {
    this.currency = currency;
  }

  public TransactionResponse toResponse(TransferTransactionEntity transaction) {
    return new TransactionResponse(
        transaction.getReference(),
        transaction.getSourceAccountNumber(),
        transaction.getDestinationAccountNumber(),
        currency,
        transaction.getAmount(),
        transaction.getTransactionFee(),
        transaction.getBilledAmount(),
        transaction.getDescription(),
        transaction.getCreatedAt(),
        transaction.getStatus(),
        transaction.getStatusMessage(),
        transaction.getCommissionWorthy(),
        transaction.getCommission(),
        transaction.getCommissionProcessedAt());
  }
}
