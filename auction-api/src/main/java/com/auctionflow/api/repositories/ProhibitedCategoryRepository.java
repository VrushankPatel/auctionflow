package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.ProhibitedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProhibitedCategoryRepository extends JpaRepository<ProhibitedCategory, Long> {
    List<ProhibitedCategory> findByActiveTrue();
    Optional<ProhibitedCategory> findByCategoryIdAndActiveTrue(String categoryId);
}