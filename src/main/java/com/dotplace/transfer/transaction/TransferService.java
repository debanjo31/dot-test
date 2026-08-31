package com.dotplace.transfer.transaction;

import com.dotplace.transfer.account.AccountEntity;
import com.dotplace.transfer.account.AccountRepository;
import com.dotplace.transfer.transaction.api.TransferRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

  private final TransferClaimRepository claimRepository;
  private final TransferTransactionRepository transactionRepository;
  private final AccountRepository accountRepository;
  private final FeePolicy feePolicy;
  private final RequestFingerprint requestFingerprint;
  private final EntityManager entityManager;
  private final Clock clock;

  public TransferService(
      TransferClaimRepository claimRepository,
      TransferTransactionRepository transactionRepository,
      AccountRepository accountRepository,
      FeePolicy feePolicy,
      RequestFingerprint requestFingerprint,
      EntityManager entityManager,
      Clock clock) {
    this.claimRepository = claimRepository;
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.feePolicy = feePolicy;
    this.requestFingerprint = requestFingerprint;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  @Transactional
  public TransferResult transfer(String suppliedIdempotencyKey, TransferRequest request) {
    String idempotencyKey = suppliedIdempotencyKey.trim();
    String sourceAccountNumber = request.sourceAccountNumber().trim();
    String destinationAccountNumber = request.destinationAccountNumber().trim();
    if (sourceAccountNumber.equals(destinationAccountNumber)) {
      throw new InvalidTransferException("Source and destination accounts must be different");
    }

    BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
    BigDecimal fee = feePolicy.calculate(amount);
    BigDecimal billedAmount = amount.add(fee);
    String fingerprint = requestFingerprint.create(request);
    UUID id = UUID.randomUUID();
    Instant createdAt = clock.instant();

    boolean claimed =
        claimRepository.claim(
            id,
            id.toString(),
            idempotencyKey,
            fingerprint,
            sourceAccountNumber,
            destinationAccountNumber,
            amount,
            fee,
            billedAmount,
            normalizeDescription(request.description()),
            createdAt);

    if (!claimed) {
      TransferTransactionEntity existing =
          transactionRepository
              .findByIdempotencyKey(idempotencyKey)
              .orElseThrow(
                  () ->
                      new IllegalStateException("Idempotency claim exists without a transaction"));
      if (!existing.getRequestFingerprint().equals(fingerprint)) {
        throw new IdempotencyConflictException();
      }
      return new TransferResult(existing, true);
    }

    entityManager.clear();
    TransferTransactionEntity transaction =
        transactionRepository
            .findById(id)
            .orElseThrow(() -> new IllegalStateException("Claimed transfer was not found"));

    List<AccountEntity> lockedAccounts =
        accountRepository.lockByAccountNumbers(
            List.of(sourceAccountNumber, destinationAccountNumber));
    Map<String, AccountEntity> accountsByNumber =
        lockedAccounts.stream()
            .collect(Collectors.toMap(AccountEntity::getAccountNumber, Function.identity()));

    AccountEntity source = accountsByNumber.get(sourceAccountNumber);
    AccountEntity destination = accountsByNumber.get(destinationAccountNumber);
    if (source == null || destination == null) {
      transaction.markFailed(missingAccountMessage(source, destination));
      return new TransferResult(transaction, false);
    }
    if (!source.getCurrency().equals(destination.getCurrency())) {
      transaction.markFailed("Source and destination account currencies do not match");
      return new TransferResult(transaction, false);
    }
    if (!source.canDebit(billedAmount)) {
      transaction.markInsufficientFunds();
      return new TransferResult(transaction, false);
    }

    Instant updatedAt = clock.instant();
    source.debit(billedAmount, updatedAt);
    destination.credit(amount, updatedAt);
    transaction.markSuccessful();
    return new TransferResult(transaction, false);
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }

  private static String missingAccountMessage(AccountEntity source, AccountEntity destination) {
    if (source == null && destination == null) {
      return "Source and destination accounts were not found";
    }
    return source == null ? "Source account was not found" : "Destination account was not found";
  }
}
