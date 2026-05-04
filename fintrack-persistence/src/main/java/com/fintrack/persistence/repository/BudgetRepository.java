package com.fintrack.persistence.repository;

import com.fintrack.persistence.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<BudgetEntity, Long> {

    List<BudgetEntity> findByUserId(Long userId);

    List<BudgetEntity> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    Optional<BudgetEntity> findByUserIdAndCategoryIdAndMonthAndYear(
            Long userId, Long categoryId, int month, int year);
}
