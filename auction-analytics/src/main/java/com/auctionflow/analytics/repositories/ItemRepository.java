package com.auctionflow.analytics.repositories;

import com.auctionflow.analytics.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findBySellerId(Long sellerId);
}