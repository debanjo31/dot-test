package com.dotplace.transfer.transaction;

public record TransferResult(TransferTransactionEntity transaction, boolean replayed) {

  public boolean hasMissingAccount() {
    String message = transaction.getStatusMessage();
    return transaction.getStatus() == TransactionStatus.FAILED
        && message != null
        && message.contains("account")
        && message.endsWith("not found");
  }
}
