package com.dotplace.transfer.summary;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTransactionSummaryRepository
    extends JpaRepository<DailyTransactionSummaryEntity, LocalDate> {}
