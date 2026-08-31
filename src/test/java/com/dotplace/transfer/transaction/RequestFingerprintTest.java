package com.dotplace.transfer.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.dotplace.transfer.transaction.api.TransferRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

  private final RequestFingerprint fingerprint = new RequestFingerprint();

  @Test
  void createsStableFingerprintForEquivalentNumbersAndWhitespace() {
    TransferRequest first =
        new TransferRequest(" 1000000001 ", "1000000002", new BigDecimal("10.00"), " rent ");
    TransferRequest second =
        new TransferRequest("1000000001", "1000000002", new BigDecimal("10"), "rent");

    assertThat(fingerprint.create(first)).isEqualTo(fingerprint.create(second)).hasSize(64);
  }

  @Test
  void changesWhenBusinessContentChanges() {
    TransferRequest first =
        new TransferRequest("1000000001", "1000000002", new BigDecimal("10"), null);
    TransferRequest second =
        new TransferRequest("1000000001", "1000000002", new BigDecimal("11"), null);

    assertThat(fingerprint.create(first)).isNotEqualTo(fingerprint.create(second));
  }
}
