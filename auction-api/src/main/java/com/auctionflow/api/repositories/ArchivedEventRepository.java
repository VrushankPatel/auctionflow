package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ArchivedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivedEventRepository extends JpaRepository<ArchivedEvent, Long> {
    List<ArchivedEvent> findByAggregateId(String aggregateId);
}