package com.dotplace.transfer.scheduling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CommissionProcessingService {

  private final CommissionBatchProcessor batchProcessor;
  private final int batchSize;

  public CommissionProcessingService(
      CommissionBatchProcessor batchProcessor,
      @Value("${app.commission.batch-size:500}") int batchSize) {
    this.batchProcessor = batchProcessor;
    this.batchSize = batchSize;
  }

  public int processOutstanding() {
    int total = 0;
    int processed;
    do {
      processed = batchProcessor.processNextBatch(batchSize);
      total += processed;
    } while (processed == batchSize);
    return total;
  }
}
