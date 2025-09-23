package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    Optional<Item> findByIdAndSellerId(String id, String sellerId);
    java.util.List<Item> findBySellerId(String sellerId);
}