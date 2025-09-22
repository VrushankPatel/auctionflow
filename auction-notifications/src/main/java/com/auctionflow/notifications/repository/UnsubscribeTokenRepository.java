package com.auctionflow.notifications.repository;

import com.auctionflow.notifications.entity.UnsubscribeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnsubscribeTokenRepository extends JpaRepository<UnsubscribeToken, Long> {

    Optional<UnsubscribeToken> findByToken(String token);

    Optional<UnsubscribeToken> findByUserIdAndEmail(String userId, String email);
}