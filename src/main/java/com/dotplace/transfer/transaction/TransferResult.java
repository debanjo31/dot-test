package com.dotplace.transfer.transaction;

public record TransferResult(TransferTransactionEntity transaction, boolean replayed) {}
