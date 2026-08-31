package com.dotplace.transfer.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_transactions")
public class TransferTransactionEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 36)
  private String reference;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
  private String idempotencyKey;

  @Column(name = "request_fingerprint", nullable = false, length = 64)
  private String requestFingerprint;

  @Column(name = "source_account_number", nullable = false, length = 34)
  private String sourceAccountNumber;

  @Column(name = "destination_account_number", nullable = false, length = 34)
  private String destinationAccountNumber;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "transaction_fee", nullable = false, precision = 19, scale = 2)
  private BigDecimal transactionFee;

  @Column(name = "billed_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal billedAmount;

  @Column(length = 255)
  private String description;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TransactionStatus status;

  @Column(name = "status_message", nullable = false, length = 255)
  private String statusMessage;

  @Column(name = "commission_worthy")
  private Boolean commissionWorthy;

  @Column(precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(name = "commission_processed_at")
  private Instant commissionProcessedAt;

  protected TransferTransactionEntity() {}

  public UUID getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }

  public String getSourceAccountNumber() {
    return sourceAccountNumber;
  }

  public String getDestinationAccountNumber() {
    return destinationAccountNumber;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getTransactionFee() {
    return transactionFee;
  }

  public BigDecimal getBilledAmount() {
    return billedAmount;
  }

  public String getDescription() {
    return description;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public Boolean getCommissionWorthy() {
    return commissionWorthy;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public Instant getCommissionProcessedAt() {
    return commissionProcessedAt;
  }

  public void markSuccessful() {
    status = TransactionStatus.SUCCESSFUL;
    statusMessage = "Transfer completed successfully";
  }

  public void markInsufficientFunds() {
    status = TransactionStatus.INSUFFICIENT_FUNDS;
    statusMessage = "Source account has insufficient funds for the billed amount";
  }

  public void markFailed(String message) {
    status = TransactionStatus.FAILED;
    statusMessage = message;
  }

  public void applyCommission(boolean worthy, BigDecimal commission, Instant processedAt) {
    this.commissionWorthy = worthy;
    this.commission = commission;
    this.commissionProcessedAt = processedAt;
  }
}
