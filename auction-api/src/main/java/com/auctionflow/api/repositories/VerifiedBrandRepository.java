package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.VerifiedBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerifiedBrandRepository extends JpaRepository<VerifiedBrand, Long> {
    List<VerifiedBrand> findByActiveTrue();
    Optional<VerifiedBrand> findByBrandNameAndActiveTrue(String brandName);
}