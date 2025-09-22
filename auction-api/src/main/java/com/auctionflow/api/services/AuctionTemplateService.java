package com.auctionflow.api.services;

import com.auctionflow.api.entities.Auction;
import com.auctionflow.api.entities.AuctionTemplate;
import com.auctionflow.api.entities.Item;
import com.auctionflow.api.repositories.AuctionTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuctionTemplateService {

    @Autowired
    private AuctionTemplateRepository templateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Assuming we have AuctionRepository, but since it's not shown, I'll use a placeholder
    // @Autowired
    // private AuctionRepository auctionRepository;

    // @Autowired
    // private ItemRepository itemRepository;

    public List<AuctionTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public List<AuctionTemplate> getTemplatesByCreator(String creatorId) {
        return templateRepository.findByCreatorId(creatorId);
    }

    public List<AuctionTemplate> getPublicTemplates() {
        return templateRepository.findByIsPublicTrue();
    }

    public List<AuctionTemplate> searchPublicTemplates(String query) {
        return templateRepository.searchPublicTemplates(query);
    }

    public Optional<AuctionTemplate> getTemplateById(String id) {
        return templateRepository.findById(id);
    }

    @Transactional
    public AuctionTemplate createTemplate(String name, String description, String creatorId, String templateData, boolean isPublic) {
        AuctionTemplate template = new AuctionTemplate(name, description, creatorId, templateData, isPublic);
        return templateRepository.save(template);
    }

    @Transactional
    public AuctionTemplate updateTemplate(String id, String name, String description, String templateData, boolean isPublic) {
        AuctionTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setName(name);
        template.setDescription(description);
        template.setTemplateData(templateData);
        template.setPublic(isPublic);
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(String id) {
        templateRepository.deleteById(id);
    }

    // Method to create auction from template
    // This is a placeholder - need to integrate with actual auction creation logic
    @Transactional
    public Auction createAuctionFromTemplate(String templateId, String sellerId) {
        AuctionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        try {
            JsonNode templateJson = objectMapper.readTree(template.getTemplateData());

            // Extract data from template JSON
            String itemTitle = templateJson.get("itemTitle").asText();
            String itemDescription = templateJson.get("itemDescription").asText();
            String categoryId = templateJson.get("categoryId").asText();
            BigDecimal reservePrice = new BigDecimal(templateJson.get("reservePrice").asText());
            BigDecimal buyNowPrice = templateJson.has("buyNowPrice") ? new BigDecimal(templateJson.get("buyNowPrice").asText()) : null;
            Instant startTs = Instant.now();
            Instant endTs = startTs.plusSeconds(templateJson.get("durationSeconds").asLong());

            // Create item
            Item item = new Item();
            item.setId(UUID.randomUUID().toString());
            item.setSellerId(sellerId);
            item.setTitle(itemTitle);
            item.setDescription(itemDescription);
            item.setCategoryId(categoryId);
            // itemRepository.save(item); // placeholder

            // Create auction
            Auction auction = new Auction();
            auction.setId(UUID.randomUUID().toString());
            auction.setItemId(item.getId());
            auction.setStatus("PENDING");
            auction.setStartTs(startTs);
            auction.setEndTs(endTs);
            // auction.setEncryptedReservePrice(encryptService.encrypt(reservePrice.toString())); // placeholder
            auction.setBuyNowPrice(buyNowPrice);
            auction.setHiddenReserve(templateJson.get("hiddenReserve").asBoolean());

            // auctionRepository.save(auction); // placeholder

            return auction;

        } catch (Exception e) {
            throw new RuntimeException("Invalid template data", e);
        }
    }

    // Bulk create auctions from template
    @Transactional
    public List<Auction> bulkCreateAuctions(String templateId, String sellerId, int count) {
        // Implementation would create multiple auctions
        // For now, just create one as example
        Auction auction = createAuctionFromTemplate(templateId, sellerId);
        return List.of(auction);
    }

    // Save draft auction as template
    @Transactional
    public AuctionTemplate saveDraftAsTemplate(String auctionId, String creatorId) {
        // This would extract auction data and save as template
        // Placeholder
        String templateData = "{\"itemTitle\":\"Draft Item\",\"itemDescription\":\"Draft Description\",\"categoryId\":\"1\",\"reservePrice\":\"100.00\",\"durationSeconds\":3600,\"hiddenReserve\":false}";
        return createTemplate("Draft Auction", "Saved draft", creatorId, templateData, false);
    }
}