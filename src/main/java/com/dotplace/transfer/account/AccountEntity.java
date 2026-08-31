package com.dotplace.transfer.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

  @Id private UUID id;

  @Column(name = "account_number", nullable = false, unique = true, length = 34)
  private String accountNumber;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(nullable = false, length = 3)
  private String currency;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AccountEntity() {}

  public UUID getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean canDebit(BigDecimal amount) {
    return balance.compareTo(amount) >= 0;
  }

  public void debit(BigDecimal amount, Instant updatedAt) {
    if (!canDebit(amount)) {
      throw new IllegalStateException("Account balance cannot become negative");
    }
    balance = balance.subtract(amount);
    this.updatedAt = updatedAt;
  }

  public void credit(BigDecimal amount, Instant updatedAt) {
    balance = balance.add(amount);
    this.updatedAt = updatedAt;
  }
}
