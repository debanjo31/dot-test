package com.dotplace.transfer.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CommissionPolicy implements MoneyPolicy {

  static final BigDecimal RATE = new BigDecimal("0.20");

  @Override
  public BigDecimal calculate(BigDecimal transactionFee) {
    return transactionFee.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
  }
}
