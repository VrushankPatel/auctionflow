package com.auctionflow.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "items")
public class Item {
    @Id
    private String id;
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(name = "title", columnDefinition = "VARCHAR(255)")
    private String title;
    private String description;
    @Column(name = "category_id")
    private String categoryId;
    private String brand;
    @Column(name = "serial_number")
    private String serialNumber;
    private List<String> images;
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

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

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public JsonNode getMetadata() { return metadata; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
}