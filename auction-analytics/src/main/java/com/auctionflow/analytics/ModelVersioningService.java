package com.auctionflow.analytics;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

@Service
public class ModelVersioningService {

    private static final Logger logger = LoggerFactory.getLogger(ModelVersioningService.class);

    @Value("${model.storage.path:/tmp/models}")
    private String modelStoragePath;

    public void saveModel(Object model, String version) throws Exception {
        Path modelDir = Paths.get(modelStoragePath);
        Files.createDirectories(modelDir);

        Path modelPath = modelDir.resolve("price_prediction_model_" + version + ".model");
        // Dummy save
        Files.writeString(modelPath, "dummy model");
        logger.info("Dummy model saved with version: {}", version);
    }

    public Object loadLatestModel() throws Exception {
        Path modelDir = Paths.get(modelStoragePath);
        if (!Files.exists(modelDir)) {
            throw new RuntimeException("Model directory does not exist");
        }

        Optional<Path> latestModel = Files.list(modelDir)
                .filter(p -> p.toString().endsWith(".model"))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

        if (latestModel.isPresent()) {
            logger.info("Dummy loaded model: {}", latestModel.get().getFileName());
            return new Object(); // dummy
        } else {
            logger.warn("No model found, returning null");
            return null;
        }
    }

    public String generateVersion() {
        return String.valueOf(System.currentTimeMillis());
    }
}