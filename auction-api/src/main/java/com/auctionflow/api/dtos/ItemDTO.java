package com.auctionflow.api.dtos;

import com.auctionflow.api.entities.Item;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class ItemDTO {
    private String id;
    private String sellerId;
    private String title;
    private String description;
    private String categoryId;
    private String brand;
    private String serialNumber;
    private List<String> images;
    private JsonNode metadata;

    public ItemDTO(Item item) {
        this.id = item.getId();
        this.sellerId = item.getSellerId().toString();
        this.title = item.getTitle();
        this.description = item.getDescription();
        this.categoryId = item.getCategoryId();
        this.brand = item.getBrand();
        this.serialNumber = item.getSerialNumber();
        this.images = item.getImages();
        this.metadata = item.getMetadata();
    }

    // getters
    public String getId() { return id; }
    public String getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategoryId() { return categoryId; }
    public String getBrand() { return brand; }
    public String getSerialNumber() { return serialNumber; }
    public List<String> getImages() { return images; }
    public JsonNode getMetadata() { return metadata; }
}