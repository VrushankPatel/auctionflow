package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ComplianceCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceCheckRepository extends JpaRepository<ComplianceCheck, Long> {

    List<ComplianceCheck> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ComplianceCheck> findByUserIdAndCheckType(Long userId, ComplianceCheck.CheckType checkType);
}