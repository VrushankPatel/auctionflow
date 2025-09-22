package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.RefreshStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshStatusRepository extends JpaRepository<RefreshStatus, String> {
}