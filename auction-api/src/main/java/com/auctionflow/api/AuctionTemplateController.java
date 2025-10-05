package com.auctionflow.api;

import com.auctionflow.api.entities.Auction;
import com.auctionflow.api.entities.AuctionTemplate;
import com.auctionflow.api.services.AuctionTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auction-templates")
@Profile("!ui-only")
public class AuctionTemplateController {

    @Autowired
    private AuctionTemplateService templateService;

    @PostMapping
    @Operation(summary = "Create auction template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuctionTemplate> createTemplate(@RequestBody CreateTemplateRequest request) {
        AuctionTemplate template = templateService.createTemplate(
                request.getName(),
                request.getDescription(),
                request.getCreatorId(),
                request.getTemplateData(),
                request.isPublic()
        );
        return ResponseEntity.ok(template);
    }

    @GetMapping
    @Operation(summary = "Get all templates for current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<AuctionTemplate>> getMyTemplates(@RequestParam String creatorId) {
        List<AuctionTemplate> templates = templateService.getTemplatesByCreator(creatorId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public templates (marketplace)")
    public ResponseEntity<List<AuctionTemplate>> getPublicTemplates(@RequestParam Optional<String> query) {
        List<AuctionTemplate> templates = query.isPresent() ?
                templateService.searchPublicTemplates(query.get()) :
                templateService.getPublicTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<AuctionTemplate> getTemplate(@PathVariable String id) {
        Optional<AuctionTemplate> template = templateService.getTemplateById(id);
        return template.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuctionTemplate> updateTemplate(@PathVariable String id, @RequestBody UpdateTemplateRequest request) {
        try {
            AuctionTemplate template = templateService.updateTemplate(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getTemplateData(),
                    request.isPublic()
            );
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/create-auction")
    @Operation(summary = "Create auction from template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Auction> createAuctionFromTemplate(@PathVariable String id, @RequestParam String sellerId) {
        try {
            Auction auction = templateService.createAuctionFromTemplate(id, sellerId);
            return ResponseEntity.ok(auction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/bulk-create")
    @Operation(summary = "Bulk create auctions from template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<Auction>> bulkCreateAuctions(@PathVariable String id, @RequestParam String sellerId, @RequestParam int count) {
        try {
            List<Auction> auctions = templateService.bulkCreateAuctions(id, sellerId, count);
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save-draft")
    @Operation(summary = "Save draft auction as template")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuctionTemplate> saveDraftAsTemplate(@RequestParam String auctionId, @RequestParam String creatorId) {
        AuctionTemplate template = templateService.saveDraftAsTemplate(auctionId, creatorId);
        return ResponseEntity.ok(template);
    }

    // DTOs
    public static class CreateTemplateRequest {
        private String name;
        private String description;
        private String creatorId;
        private String templateData;
        private boolean isPublic;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCreatorId() { return creatorId; }
        public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

        public String getTemplateData() { return templateData; }
        public void setTemplateData(String templateData) { this.templateData = templateData; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    }

    public static class UpdateTemplateRequest {
        private String name;
        private String description;
        private String templateData;
        private boolean isPublic;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTemplateData() { return templateData; }
        public void setTemplateData(String templateData) { this.templateData = templateData; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    }
}