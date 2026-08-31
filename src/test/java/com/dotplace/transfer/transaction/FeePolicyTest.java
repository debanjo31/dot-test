package com.dotplace.transfer.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeePolicyTest {

  private final FeePolicy policy = new FeePolicy();

  @Test
  void calculatesAndRoundsHalfUp() {
    assertThat(policy.calculate(new BigDecimal("1001.00")))
        .isEqualByComparingTo(new BigDecimal("5.01"));
    assertThat(policy.calculate(new BigDecimal("1.00")))
        .isEqualByComparingTo(new BigDecimal("0.01"));
  }

  @Test
  void capsFeeAtOneHundredNaira() {
    assertThat(policy.calculate(new BigDecimal("20000.00")))
        .isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(policy.calculate(new BigDecimal("999999.00")))
        .isEqualByComparingTo(new BigDecimal("100.00"));
  }
}
