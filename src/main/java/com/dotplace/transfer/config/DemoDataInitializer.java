package com.dotplace.transfer.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final Clock clock;

  public DemoDataInitializer(NamedParameterJdbcTemplate jdbcTemplate, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    createAccount("1000000001", new BigDecimal("100000.00"));
    createAccount("1000000002", new BigDecimal("50000.00"));
  }

  private void createAccount(String accountNumber, BigDecimal balance) {
    String sql =
        """
                INSERT INTO accounts (
                    id, account_number, balance, currency, version, created_at, updated_at
                ) VALUES (
                    :id, :accountNumber, :balance, 'NGN', 0, :now, :now
                )
                ON CONFLICT (account_number) DO NOTHING
                """;
    jdbcTemplate.update(
        sql,
        Map.of(
            "id",
            UUID.randomUUID(),
            "accountNumber",
            accountNumber,
            "balance",
            balance,
            "now",
            clock.instant().atOffset(ZoneOffset.UTC)));
  }
}
