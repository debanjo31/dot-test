package com.dotplace.transfer.account;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select a from AccountEntity a
            where a.accountNumber in :accountNumbers
            order by a.accountNumber
            """)
  List<AccountEntity> lockByAccountNumbers(
      @Param("accountNumbers") Collection<String> accountNumbers);
}
