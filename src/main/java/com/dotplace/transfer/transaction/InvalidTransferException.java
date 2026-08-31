package com.dotplace.transfer.transaction;

public class InvalidTransferException extends RuntimeException {

  public InvalidTransferException(String message) {
    super(message);
  }
}
