package com.dotplace.transfer.transaction;

import java.math.BigDecimal;

public interface MoneyPolicy {

  BigDecimal calculate(BigDecimal baseAmount);
}
