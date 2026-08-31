package com.dotplace.transfer.transaction;

public class AccountNotFoundException extends RuntimeException {

  private final String transactionReference;
  private final boolean replayed;

  public AccountNotFoundException(String message, String transactionReference, boolean replayed) {
    super(message);
    this.transactionReference = transactionReference;
    this.replayed = replayed;
  }

  public String getTransactionReference() {
    return transactionReference;
  }

  public boolean isReplayed() {
    return replayed;
  }
}
