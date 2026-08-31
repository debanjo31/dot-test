package com.dotplace.transfer.transaction;

import com.dotplace.transfer.transaction.api.PageResponse;
import com.dotplace.transfer.transaction.api.TransactionMapper;
import com.dotplace.transfer.transaction.api.TransactionResponse;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionSearchService {

  private final TransferTransactionRepository repository;
  private final TransactionMapper mapper;

  public TransactionSearchService(
      TransferTransactionRepository repository, TransactionMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TransactionResponse> search(
      TransactionStatus status,
      String accountNumber,
      Instant from,
      Instant to,
      int page,
      int size,
      Sort.Direction direction) {
    if (from != null && to != null && !from.isBefore(to)) {
      throw new InvalidTransferException("'from' must be earlier than 'to'");
    }

    Specification<TransferTransactionEntity> specification = Specification.unrestricted();
    if (status != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (accountNumber != null && !accountNumber.isBlank()) {
      String normalized = accountNumber.trim();
      specification =
          specification.and(
              (root, query, cb) ->
                  cb.or(
                      cb.equal(root.get("sourceAccountNumber"), normalized),
                      cb.equal(root.get("destinationAccountNumber"), normalized)));
    }
    if (from != null) {
      specification =
          specification.and(
              (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
    }
    if (to != null) {
      specification =
          specification.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
    }

    PageRequest pageable =
        PageRequest.of(page, size, Sort.by(direction, "createdAt").and(Sort.by("id")));
    Page<TransactionResponse> result =
        repository.findAll(specification, pageable).map(mapper::toResponse);
    return PageResponse.from(result);
  }
}
