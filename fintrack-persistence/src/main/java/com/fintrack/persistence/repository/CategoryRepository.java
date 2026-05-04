package com.fintrack.persistence.repository;

import com.fintrack.persistence.entity.CategoryEntity;
import com.fintrack.persistence.entity.CategoryEntity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByUserId(Long userId);

    List<CategoryEntity> findByUserIdAndType(Long userId, CategoryType type);

    Optional<CategoryEntity> findByUserIdAndName(Long userId, String name);
}
