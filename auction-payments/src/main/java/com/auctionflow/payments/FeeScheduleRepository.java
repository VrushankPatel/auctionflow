package com.auctionflow.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, Long> {

    Optional<FeeSchedule> findByFeeTypeAndActiveTrue(FeeSchedule.FeeType feeType);
}