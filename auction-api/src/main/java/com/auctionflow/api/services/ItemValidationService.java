package com.auctionflow.api.services;

import com.auctionflow.api.entities.ProhibitedCategory;
import com.auctionflow.api.entities.VerifiedBrand;
import com.auctionflow.api.repositories.ProhibitedCategoryRepository;
import com.auctionflow.api.repositories.VerifiedBrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemValidationService {

    @Autowired
    private ProhibitedCategoryRepository prohibitedCategoryRepository;

    @Autowired
    private VerifiedBrandRepository verifiedBrandRepository;

    public ValidationResult validateItem(String categoryId, String title, String description, String brand, String serialNumber) {
        ValidationResult result = new ValidationResult();

        // Check prohibited categories
        Optional<ProhibitedCategory> prohibited = prohibitedCategoryRepository.findByCategoryIdAndActiveTrue(categoryId);
        if (prohibited.isPresent()) {
            result.setValid(false);
            result.setReason("Category '" + categoryId + "' is prohibited: " + prohibited.get().getReason());
            return result;
        }

        // Check for counterfeit keywords in title/description
        String combinedText = (title + " " + description).toLowerCase();
        if (combinedText.contains("counterfeit") || combinedText.contains("fake") || combinedText.contains("replica")) {
            result.setValid(false);
            result.setReason("Item appears to be counterfeit or replica");
            return result;
        }

        // Check brand verification if brand is provided
        if (brand != null && !brand.isEmpty() && !isBrandVerified(brand)) {
            result.setValid(false);
            result.setReason("Brand '" + brand + "' is not verified. Please contact support.");
            return result;
        }

        // For now, assume valid if no issues
        result.setValid(true);
        return result;
    }

    public boolean isBrandVerified(String brandName) {
        return verifiedBrandRepository.findByBrandNameAndActiveTrue(brandName).isPresent();
    }

    public List<VerifiedBrand> getVerifiedBrands() {
        return verifiedBrandRepository.findByActiveTrue();
    }

    public static class ValidationResult {
        private boolean valid;
        private String reason;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}