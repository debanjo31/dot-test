package com.dotplace.transfer.summary.api;

import com.dotplace.transfer.summary.DailySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transaction-summaries")
@Tag(name = "Transaction summaries")
public class DailySummaryController {

  private final DailySummaryService summaryService;

  public DailySummaryController(DailySummaryService summaryService) {
    this.summaryService = summaryService;
  }

  @GetMapping("/{date}")
  @Operation(summary = "Get a transaction summary for today or an earlier business date")
  public DailySummaryResponse get(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return summaryService.get(date);
  }
}
