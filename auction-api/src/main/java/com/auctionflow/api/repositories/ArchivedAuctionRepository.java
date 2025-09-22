package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ArchivedAuction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchivedAuctionRepository extends JpaRepository<ArchivedAuction, String> {
}