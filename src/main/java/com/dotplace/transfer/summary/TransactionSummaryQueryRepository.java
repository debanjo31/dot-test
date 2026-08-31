package com.dotplace.transfer.summary;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionSummaryQueryRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public TransactionSummaryQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public SummaryTotals calculate(Instant fromInclusive, Instant toExclusive) {
    String sql =
        """
                SELECT
                    COUNT(*) AS total_count,
                    COUNT(*) FILTER (WHERE status = 'SUCCESSFUL') AS successful_count,
                    COUNT(*) FILTER (WHERE status = 'INSUFFICIENT_FUNDS') AS insufficient_funds_count,
                    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_count,
                    COALESCE(SUM(amount), 0) AS total_amount,
                    COALESCE(SUM(transaction_fee), 0) AS total_fees,
                    COALESCE(SUM(billed_amount), 0) AS total_billed_amount,
                    COALESCE(SUM(commission), 0) AS total_commission
                FROM transfer_transactions
                WHERE created_at >= :fromInclusive AND created_at < :toExclusive
                  AND status <> 'PROCESSING'
                """;
    return jdbcTemplate.queryForObject(
        sql,
        Map.of(
            "fromInclusive",
            fromInclusive.atOffset(ZoneOffset.UTC),
            "toExclusive",
            toExclusive.atOffset(ZoneOffset.UTC)),
        TransactionSummaryQueryRepository::mapSummary);
  }

  private static SummaryTotals mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
    return new SummaryTotals(
        resultSet.getLong("total_count"),
        resultSet.getLong("successful_count"),
        resultSet.getLong("insufficient_funds_count"),
        resultSet.getLong("failed_count"),
        money(resultSet, "total_amount"),
        money(resultSet, "total_fees"),
        money(resultSet, "total_billed_amount"),
        money(resultSet, "total_commission"));
  }

  private static BigDecimal money(ResultSet resultSet, String column) throws SQLException {
    return resultSet.getBigDecimal(column).setScale(2);
  }
}
