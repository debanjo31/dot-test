package com.dotplace.transfer.transaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransferTransactionRepository
    extends JpaRepository<TransferTransactionEntity, UUID>,
        JpaSpecificationExecutor<TransferTransactionEntity> {

  Optional<TransferTransactionEntity> findByIdempotencyKey(String idempotencyKey);

  List<TransferTransactionEntity> findByCommissionProcessedAtIsNullAndStatusNotOrderByCreatedAt(
      TransactionStatus status, Pageable pageable);

  @Query("select min(t.createdAt) from TransferTransactionEntity t")
  Optional<Instant> findEarliestCreatedAt();
}
