package com.dotplace.transfer.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionPolicyTest {

  private final CommissionPolicy policy = new CommissionPolicy();

  @Test
  void calculatesTwentyPercentAndRoundsToTwoDecimals() {
    assertThat(policy.calculate(new BigDecimal("5.05")))
        .isEqualByComparingTo(new BigDecimal("1.01"));
    assertThat(policy.calculate(new BigDecimal("100.00")))
        .isEqualByComparingTo(new BigDecimal("20.00"));
  }
}
