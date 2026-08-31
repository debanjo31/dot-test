package com.dotplace.transfer.scheduling;

import com.dotplace.transfer.summary.DailySummaryService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyJobs {

  private static final Logger LOGGER = LoggerFactory.getLogger(DailyJobs.class);

  private final CommissionProcessingService commissionService;
  private final DailySummaryService summaryService;

  public DailyJobs(
      CommissionProcessingService commissionService, DailySummaryService summaryService) {
    this.commissionService = commissionService;
    this.summaryService = summaryService;
  }

  @Scheduled(cron = "${app.commission.cron}", zone = "${app.business-zone}")
  @SchedulerLock(
      name = "dailyCommissionProcessing",
      lockAtMostFor = "PT30M",
      lockAtLeastFor = "PT5S")
  public void processCommissions() {
    LockAssert.assertLocked();
    int processed = commissionService.processOutstanding();
    LOGGER.info("Commission processing completed: processed={}", processed);
  }

  @Scheduled(cron = "${app.summary.cron}", zone = "${app.business-zone}")
  @SchedulerLock(name = "dailySummaryGeneration", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5S")
  public void generateSummaries() {
    LockAssert.assertLocked();
    int generated = summaryService.materializeMissingPastSummaries();
    LOGGER.info("Daily summary generation completed: generated={}", generated);
  }
}
