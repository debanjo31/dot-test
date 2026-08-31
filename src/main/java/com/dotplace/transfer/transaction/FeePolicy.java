package com.dotplace.transfer.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class FeePolicy implements MoneyPolicy {

  static final BigDecimal RATE = new BigDecimal("0.005");
  static final BigDecimal CAP = new BigDecimal("100.00");

  @Override
  public BigDecimal calculate(BigDecimal amount) {
    BigDecimal calculated = amount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    return calculated.min(CAP).setScale(2, RoundingMode.HALF_UP);
  }
}
