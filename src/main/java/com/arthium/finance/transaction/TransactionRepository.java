package com.arthium.finance.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findByUserIdAndRecurringTrueAndNextRecurringDateLessThanEqual(UUID userId, Instant moment);

    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.id IN :ids AND t.userId = :userId")
    int deleteByIdInAndUserId(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN ABS(amount) ELSE 0 END), 0) AS totalIncome,
                   COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN ABS(amount) ELSE 0 END), 0) AS totalExpense,
                   COUNT(*) AS transactionCount
            FROM transactions
            WHERE user_id = :userId
              AND (CAST(:from AS timestamptz) IS NULL OR date >= :from)
              AND (CAST(:to AS timestamptz) IS NULL OR date <= :to)
            """, nativeQuery = true)
    SummaryTotals sumTotals(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT category AS category,
                   COALESCE(SUM(ABS(amount)), 0) AS total,
                   COUNT(*) AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = :type
              AND (CAST(:from AS timestamptz) IS NULL OR date >= :from)
              AND (CAST(:to AS timestamptz) IS NULL OR date <= :to)
            GROUP BY category
            ORDER BY total DESC
            """, nativeQuery = true)
    List<CategoryTotal> sumByCategory(@Param("userId") UUID userId, @Param("type") String type,
                                       @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT LOWER(category) AS category,
                   COALESCE(SUM(ABS(amount)), 0) AS total,
                   COUNT(*) AS count
            FROM transactions
            WHERE user_id = :userId
              AND type = :type
              AND date >= :from
              AND date <= :to
            GROUP BY LOWER(category)
            """, nativeQuery = true)
    List<CategoryTotal> sumByLowerCategory(@Param("userId") UUID userId, @Param("type") String type,
                                            @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT period,
                   COALESCE(SUM(CASE WHEN type = 'INCOME' THEN ABS(amount) ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN ABS(amount) ELSE 0 END), 0) AS expenses,
                   COALESCE(SUM(CASE WHEN type = 'INCOME' THEN 1 ELSE 0 END), 0) AS incomeCount,
                   COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN 1 ELSE 0 END), 0) AS expenseCount
            FROM (
                SELECT type, amount, to_char(date AT TIME ZONE 'UTC', :format) AS period
                FROM transactions
                WHERE user_id = :userId
                  AND (CAST(:from AS timestamptz) IS NULL OR date >= :from)
                  AND (CAST(:to AS timestamptz) IS NULL OR date <= :to)
            ) bucketed
            GROUP BY period
            ORDER BY period
            """, nativeQuery = true)
    List<PeriodTotal> sumByPeriod(@Param("userId") UUID userId, @Param("from") Instant from,
                                   @Param("to") Instant to, @Param("format") String format);
}
