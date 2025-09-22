package com.auctionflow.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByActiveTrue();
    Optional<TaxRate> findByCountryCodeAndStateCodeAndCategoryAndActiveTrue(String countryCode, String stateCode, String category);
    Optional<TaxRate> findByCountryCodeAndCategoryAndActiveTrue(String countryCode, String category);
    Optional<TaxRate> findByCountryCodeAndStateCodeAndActiveTrue(String countryCode, String stateCode);
    Optional<TaxRate> findByCountryCodeAndActiveTrue(String countryCode);
}