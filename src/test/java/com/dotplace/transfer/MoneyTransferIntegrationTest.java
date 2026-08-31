package com.dotplace.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotplace.transfer.scheduling.CommissionProcessingService;
import com.dotplace.transfer.summary.DailySummaryService;
import com.dotplace.transfer.transaction.TransactionStatus;
import com.dotplace.transfer.transaction.TransferResult;
import com.dotplace.transfer.transaction.TransferService;
import com.dotplace.transfer.transaction.api.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MoneyTransferIntegrationTest {

  private static final String SOURCE = "1000000001";
  private static final String DESTINATION = "1000000002";

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("money_transfer_test")
          .withUsername("test")
          .withPassword("test");

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired NamedParameterJdbcTemplate jdbcTemplate;
  @Autowired TransferService transferService;
  @Autowired CommissionProcessingService commissionService;
  @Autowired DailySummaryService summaryService;
  @Autowired Clock clock;

  @BeforeEach
  void resetDatabase() {
    jdbcTemplate
        .getJdbcTemplate()
        .execute("TRUNCATE daily_transaction_summaries, transfer_transactions, accounts");
    insertAccount(SOURCE, "100000.00");
    insertAccount(DESTINATION, "50000.00");
  }

  @Test
  void successfulTransferCalculatesMoneyAndMovesBalancesAtomically() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "successful-transfer")
                .header("X-Correlation-Id", "test-correlation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("1000.00", "Rent")))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Correlation-Id", "test-correlation"))
        .andExpect(jsonPath("$.amount").value(1000.00))
        .andExpect(jsonPath("$.transactionFee").value(5.00))
        .andExpect(jsonPath("$.billedAmount").value(1005.00))
        .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

    assertThat(balance(SOURCE)).isEqualByComparingTo("98995.00");
    assertThat(balance(DESTINATION)).isEqualByComparingTo("51000.00");
  }

  @Test
  void replayReturnsOriginalTransactionWithoutDebitingTwice() throws Exception {
    String request = transferJson("1000.00", "Rent");
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "replay-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "replay-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(header().string("Idempotent-Replay", "true"));

    assertThat(balance(SOURCE)).isEqualByComparingTo("98995.00");
    assertThat(transactionCount()).isEqualTo(1);
  }

  @Test
  void changedReplayIsRejectedAndOriginalTransferRemainsTheOnlyDebit() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "conflict-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("100.00", "First")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "conflict-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("200.00", "Changed")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

    assertThat(balance(SOURCE)).isEqualByComparingTo("99899.50");
    assertThat(transactionCount()).isEqualTo(1);
  }

  @Test
  void recordsInsufficientFundsAndMissingAccountsWithoutChangingBalances() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "insufficient-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("999999.00", null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("INSUFFICIENT_FUNDS"));

    String missingAccountRequest =
        objectMapper.writeValueAsString(
            Map.of(
                "sourceAccountNumber",
                SOURCE,
                "destinationAccountNumber",
                "9999999999",
                "amount",
                new BigDecimal("50.00")));
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "missing-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingAccountRequest))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").value("Destination account was not found"))
        .andExpect(jsonPath("$.transactionStatus").value("FAILED"))
        .andExpect(jsonPath("$.transactionReference").isNotEmpty());

    assertThat(balance(SOURCE)).isEqualByComparingTo("100000.00");
    assertThat(balance(DESTINATION)).isEqualByComparingTo("50000.00");
  }

  @Test
  void missingSourceAndDestinationReturnNotFoundAndReplayTheSameError() throws Exception {
    String request =
        objectMapper.writeValueAsString(
            Map.of(
                "sourceAccountNumber",
                "8888888888",
                "destinationAccountNumber",
                "9999999999",
                "amount",
                new BigDecimal("50.00")));

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "both-accounts-missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").value("Source and destination accounts were not found"))
        .andExpect(jsonPath("$.transactionStatus").value("FAILED"))
        .andExpect(jsonPath("$.transactionReference").isNotEmpty());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "both-accounts-missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Idempotent-Replay", "true"))
        .andExpect(jsonPath("$.detail").value("Source and destination accounts were not found"));

    assertThat(transactionCount()).isEqualTo(1);
  }

  @Test
  void validatesRequestsAndSearchesByStatusAccountAndDateRange() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "search-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("200.00", "Searchable")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/transactions")
                .param("status", "SUCCESSFUL")
                .param("accountNumber", DESTINATION)
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2099-01-01T00:00:00Z")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.totalElements").value(1));

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("0.00", null)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/v1/transactions")
                .param("from", "2026-01-02T00:00:00Z")
                .param("to", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void commissionAndCurrentDaySummaryAreCorrectAndIdempotent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "summary-success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("1000.00", null)))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header("Idempotency-Key", "summary-failure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferJson("999999.00", null)))
        .andExpect(status().isCreated());

    assertThat(commissionService.processOutstanding()).isEqualTo(2);
    assertThat(commissionService.processOutstanding()).isZero();

    LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Africa/Lagos")));
    mockMvc
        .perform(get("/api/v1/transaction-summaries/{date}", today))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.successfulCount").value(1))
        .andExpect(jsonPath("$.insufficientFundsCount").value(1))
        .andExpect(jsonPath("$.totalCommission").value(1.00))
        .andExpect(jsonPath("$.materialized").value(false));

    mockMvc
        .perform(get("/api/v1/transaction-summaries/{date}", today.plusDays(1)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void historicalSummariesFallBackLiveThenMaterializeIdempotently() throws Exception {
    LocalDate yesterday = LocalDate.now(clock.withZone(ZoneId.of("Africa/Lagos"))).minusDays(1);

    mockMvc
        .perform(get("/api/v1/transaction-summaries/{date}", yesterday))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(0))
        .andExpect(jsonPath("$.materialized").value(false));

    assertThat(summaryService.materializeMissingPastSummaries()).isEqualTo(1);
    assertThat(summaryService.materializeMissingPastSummaries()).isEqualTo(1);

    mockMvc
        .perform(get("/api/v1/transaction-summaries/{date}", yesterday))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCount").value(0))
        .andExpect(jsonPath("$.materialized").value(true));

    assertThat(summaryService.regenerate(yesterday).materialized()).isTrue();
  }

  @Test
  void concurrentTransfersNeverOverdrawTheSourceAccount() throws Exception {
    jdbcTemplate.update(
        "UPDATE accounts SET balance = :balance WHERE account_number = :accountNumber",
        Map.of("balance", new BigDecimal("100.00"), "accountNumber", SOURCE));

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<TransferResult> first =
          executor.submit(() -> concurrentTransfer("concurrent-1", ready, start));
      Future<TransferResult> second =
          executor.submit(() -> concurrentTransfer("concurrent-2", ready, start));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(
              java.util.List.of(
                  first.get(20, TimeUnit.SECONDS).transaction().getStatus(),
                  second.get(20, TimeUnit.SECONDS).transaction().getStatus()))
          .containsExactlyInAnyOrder(
              TransactionStatus.SUCCESSFUL, TransactionStatus.INSUFFICIENT_FUNDS);
      assertThat(balance(SOURCE)).isEqualByComparingTo("0.50");
      assertThat(balance(DESTINATION)).isEqualByComparingTo("50099.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentRequestsWithTheSameIdempotencyKeyDebitExactlyOnce() throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<TransferResult> first =
          executor.submit(() -> concurrentTransfer("same-concurrent-key", ready, start));
      Future<TransferResult> second =
          executor.submit(() -> concurrentTransfer("same-concurrent-key", ready, start));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(
              java.util.List.of(
                  first.get(20, TimeUnit.SECONDS).replayed(),
                  second.get(20, TimeUnit.SECONDS).replayed()))
          .containsExactlyInAnyOrder(false, true);
      assertThat(balance(SOURCE)).isEqualByComparingTo("99900.50");
      assertThat(balance(DESTINATION)).isEqualByComparingTo("50099.00");
      assertThat(transactionCount()).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  private TransferResult concurrentTransfer(String key, CountDownLatch ready, CountDownLatch start)
      throws InterruptedException {
    ready.countDown();
    if (!start.await(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Concurrent test did not start");
    }
    return transferService.transfer(
        key,
        new TransferRequest(SOURCE, DESTINATION, new BigDecimal("99.00"), "Concurrent transfer"));
  }

  private String transferJson(String amount, String description) throws Exception {
    java.util.HashMap<String, Object> request = new java.util.HashMap<>();
    request.put("sourceAccountNumber", SOURCE);
    request.put("destinationAccountNumber", DESTINATION);
    request.put("amount", new BigDecimal(amount));
    if (description != null) {
      request.put("description", description);
    }
    return objectMapper.writeValueAsString(request);
  }

  private void insertAccount(String accountNumber, String balance) {
    String sql =
        """
                INSERT INTO accounts (
                    id, account_number, balance, currency, version, created_at, updated_at
                ) VALUES (:id, :accountNumber, :balance, 'NGN', 0, now(), now())
                """;
    jdbcTemplate.update(
        sql,
        Map.of(
            "id",
            UUID.randomUUID(),
            "accountNumber",
            accountNumber,
            "balance",
            new BigDecimal(balance)));
  }

  private BigDecimal balance(String accountNumber) {
    return jdbcTemplate.queryForObject(
        "SELECT balance FROM accounts WHERE account_number = :accountNumber",
        Map.of("accountNumber", accountNumber),
        BigDecimal.class);
  }

  private int transactionCount() {
    return jdbcTemplate
        .getJdbcTemplate()
        .queryForObject("SELECT COUNT(*) FROM transfer_transactions", Integer.class);
  }
}
