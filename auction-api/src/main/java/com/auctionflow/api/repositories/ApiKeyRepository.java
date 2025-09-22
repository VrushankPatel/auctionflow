package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByServiceNameAndRevokedAtIsNull(String serviceName);
    Optional<ApiKey> findByHashedKeyAndRevokedAtIsNull(String hashedKey);
}