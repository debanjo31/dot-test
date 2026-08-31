package com.dotplace.transfer.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class CommissionProcessingServiceTest {

  @Test
  void keepsProcessingFullBatchesUntilTheFinalPartialBatch() {
    CommissionBatchProcessor processor = Mockito.mock(CommissionBatchProcessor.class);
    when(processor.processNextBatch(2)).thenReturn(2, 2, 1);
    CommissionProcessingService service = new CommissionProcessingService(processor, 2);

    assertThat(service.processOutstanding()).isEqualTo(5);
    InOrder order = inOrder(processor);
    order.verify(processor, Mockito.times(3)).processNextBatch(2);
  }
}
