package com.fintrack.persistence.repository;

import com.fintrack.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByAccountId(Long accountId);

    List<TransactionEntity> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.account.user.id = :userId
          AND t.transactionDate >= :from
          AND t.transactionDate < :to
        ORDER BY t.transactionDate DESC
        """)
    List<TransactionEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.category.id = :categoryId
          AND FUNCTION('MONTH', t.transactionDate) = :month
          AND FUNCTION('YEAR',  t.transactionDate) = :year
        """)
    List<TransactionEntity> findByCategoryAndPeriod(
            @Param("categoryId") Long categoryId,
            @Param("month") int month,
            @Param("year") int year
    );
}
