package com.dotplace.transfer.transaction.api;

import com.dotplace.transfer.transaction.AccountNotFoundException;
import com.dotplace.transfer.transaction.TransactionSearchService;
import com.dotplace.transfer.transaction.TransactionStatus;
import com.dotplace.transfer.transaction.TransferResult;
import com.dotplace.transfer.transaction.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions")
public class TransferController {

  private final TransferService transferService;
  private final TransactionSearchService searchService;
  private final TransactionMapper mapper;

  public TransferController(
      TransferService transferService,
      TransactionSearchService searchService,
      TransactionMapper mapper) {
    this.transferService = transferService;
    this.searchService = searchService;
    this.mapper = mapper;
  }

  @PostMapping("/transfers")
  @Operation(summary = "Process an idempotent account-to-account transfer")
  public ResponseEntity<TransactionResponse> transfer(
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      @Valid @RequestBody TransferRequest request) {
    TransferResult result = transferService.transfer(idempotencyKey, request);
    if (result.hasMissingAccount()) {
      throw new AccountNotFoundException(
          result.transaction().getStatusMessage(),
          result.transaction().getReference(),
          result.replayed());
    }
    TransactionResponse response = mapper.toResponse(result.transaction());
    if (result.replayed()) {
      return ResponseEntity.ok().header("Idempotent-Replay", "true").body(response);
    }
    return ResponseEntity.status(201).body(response);
  }

  @GetMapping("/transactions")
  @Operation(summary = "Search and page through transaction history")
  public PageResponse<TransactionResponse> transactions(
      @RequestParam(required = false) TransactionStatus status,
      @RequestParam(required = false) @Size(max = 34) String accountNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
    return searchService.search(status, accountNumber, from, to, page, size, direction);
  }
}
