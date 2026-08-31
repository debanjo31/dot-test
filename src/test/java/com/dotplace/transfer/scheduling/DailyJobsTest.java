package com.dotplace.transfer.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotplace.transfer.summary.DailySummaryService;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DailyJobsTest {

  @Test
  void delegatesLockedJobsToIdempotentApplicationServices() {
    CommissionProcessingService commissionService = Mockito.mock(CommissionProcessingService.class);
    DailySummaryService summaryService = Mockito.mock(DailySummaryService.class);
    when(commissionService.processOutstanding()).thenReturn(3);
    when(summaryService.materializeMissingPastSummaries()).thenReturn(2);
    DailyJobs jobs = new DailyJobs(commissionService, summaryService);

    try (MockedStatic<LockAssert> ignored = Mockito.mockStatic(LockAssert.class)) {
      jobs.processCommissions();
      jobs.generateSummaries();
    }

    verify(commissionService).processOutstanding();
    verify(summaryService).materializeMissingPastSummaries();
  }
}
