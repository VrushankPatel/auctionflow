package com.auctionflow.api;

import com.auctionflow.api.dtos.ItemDTO;
import com.auctionflow.api.entities.Item;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.services.ItemValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemValidationService itemValidationService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ItemDTO> createItem(@Valid @RequestBody CreateItemRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Assume UserService to get user by email
        // For simplicity, assume sellerId is email or something, but need UserService
        // Placeholder: get sellerId from auth
        UUID sellerId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Item item = new Item();
        item.setId(UUID.randomUUID().toString());
        item.setSellerId(sellerId.toString());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setBrand(request.getBrand());
        item.setSerialNumber(request.getSerialNumber());
        // item.setImages(request.getImages());
        item.setMetadata(request.getMetadata());

        // Validate
        ItemValidationService.ValidationResult validation = itemValidationService.validateItem(item.getCategoryId(), item.getTitle(), item.getDescription(), item.getBrand(), item.getSerialNumber());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        itemRepository.save(item);
        ItemDTO dto = new ItemDTO(item);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO> getItem(@PathVariable String id) {
        Optional<Item> itemOpt = itemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ItemDTO dto = new ItemDTO(itemOpt.get());
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<ItemDTO>> listItems(@RequestParam Optional<String> sellerId) {
        List<Item> items;
        if (sellerId.isPresent()) {
            items = itemRepository.findBySellerId(sellerId.get());
        } else {
            items = itemRepository.findAll();
        }
        List<ItemDTO> dtos = items.stream().map(ItemDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@itemSecurityService.canEditItem(#id, authentication.principal)")
    public ResponseEntity<ItemDTO> updateItem(@PathVariable String id, @Valid @RequestBody UpdateItemRequest request) {
        Optional<Item> itemOpt = itemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Item item = itemOpt.get();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
        item.setBrand(request.getBrand());
        item.setSerialNumber(request.getSerialNumber());
        // item.setImages(request.getImages());
        item.setMetadata(request.getMetadata());

        ItemValidationService.ValidationResult validation = itemValidationService.validateItem(item.getCategoryId(), item.getTitle(), item.getDescription(), item.getBrand(), item.getSerialNumber());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        itemRepository.save(item);
        ItemDTO dto = new ItemDTO(item);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@itemSecurityService.canEditItem(#id, authentication.principal)")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // DTOs
    public static class CreateItemRequest {
        private String title;
        private String description;
        private String categoryId;
        private String brand;
        private String serialNumber;
        private List<String> images;
        private com.fasterxml.jackson.databind.JsonNode metadata;

        // getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

        public List<String> images() { return images; }
        public void setImages(List<String> images) { this.images = images; }

        public com.fasterxml.jackson.databind.JsonNode getMetadata() { return metadata; }
        public void setMetadata(com.fasterxml.jackson.databind.JsonNode metadata) { this.metadata = metadata; }
    }

    public static class UpdateItemRequest extends CreateItemRequest {
    }
}