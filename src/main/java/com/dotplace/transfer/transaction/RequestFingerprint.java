package com.dotplace.transfer.transaction;

import com.dotplace.transfer.transaction.api.TransferRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RequestFingerprint {

  public String create(TransferRequest request) {
    String canonical =
        String.join(
            "\n",
            request.sourceAccountNumber().trim(),
            request.destinationAccountNumber().trim(),
            request.amount().stripTrailingZeros().toPlainString(),
            request.description() == null ? "" : request.description().trim());
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
