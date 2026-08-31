package com.dotplace.transfer.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransferClaimRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public TransferClaimRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean claim(
      UUID id,
      String reference,
      String idempotencyKey,
      String fingerprint,
      String sourceAccountNumber,
      String destinationAccountNumber,
      BigDecimal amount,
      BigDecimal fee,
      BigDecimal billedAmount,
      String description,
      Instant createdAt) {
    String sql =
        """
                INSERT INTO transfer_transactions (
                    id, reference, idempotency_key, request_fingerprint,
                    source_account_number, destination_account_number,
                    amount, transaction_fee, billed_amount, description,
                    created_at, status, status_message
                ) VALUES (
                    :id, :reference, :idempotencyKey, :fingerprint,
                    :sourceAccountNumber, :destinationAccountNumber,
                    :amount, :fee, :billedAmount, :description,
                    :createdAt, 'PROCESSING', 'Transfer is being processed'
                )
                ON CONFLICT (idempotency_key) DO NOTHING
                """;

    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("reference", reference)
            .addValue("idempotencyKey", idempotencyKey)
            .addValue("fingerprint", fingerprint)
            .addValue("sourceAccountNumber", sourceAccountNumber)
            .addValue("destinationAccountNumber", destinationAccountNumber)
            .addValue("amount", amount)
            .addValue("fee", fee)
            .addValue("billedAmount", billedAmount)
            .addValue("description", description)
            .addValue("createdAt", createdAt.atOffset(ZoneOffset.UTC));
    int inserted = jdbcTemplate.update(sql, parameters);
    return inserted == 1;
  }
}
