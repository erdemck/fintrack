package com.fintrack.persistence.repository;

import com.fintrack.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    List<AccountEntity> findByUserId(Long userId);

    List<AccountEntity> findByUserIdAndCurrency(Long userId, String currency);
}
