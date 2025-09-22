package com.auctionflow.api.repositories;

import com.auctionflow.api.entities.DocumentUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, Long> {

    List<DocumentUpload> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<DocumentUpload> findByUserIdAndDocumentType(Long userId, DocumentUpload.DocumentType documentType);
}