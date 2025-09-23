package com.auctionflow.analytics.repositories;

import com.auctionflow.analytics.entities.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findByUserId(Long userId);
}