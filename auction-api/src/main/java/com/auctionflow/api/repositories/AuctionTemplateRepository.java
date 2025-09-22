package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.AuctionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionTemplateRepository extends JpaRepository<AuctionTemplate, String> {

    List<AuctionTemplate> findByCreatorId(String creatorId);

    List<AuctionTemplate> findByIsPublicTrue();

    @Query("SELECT t FROM AuctionTemplate t WHERE t.isPublic = true AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<AuctionTemplate> searchPublicTemplates(@Param("query") String query);
}