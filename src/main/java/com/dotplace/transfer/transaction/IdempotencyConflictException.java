package com.dotplace.transfer.transaction;

public class IdempotencyConflictException extends RuntimeException {

  public IdempotencyConflictException() {
    super("The Idempotency-Key has already been used with a different request");
  }
}
