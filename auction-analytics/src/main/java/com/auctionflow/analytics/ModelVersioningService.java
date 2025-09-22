package com.auctionflow.analytics;

import ml.dmlc.xgboost4j.java.Booster;
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

    public void saveModel(Booster model, String version) throws Exception {
        Path modelDir = Paths.get(modelStoragePath);
        Files.createDirectories(modelDir);

        Path modelPath = modelDir.resolve("price_prediction_model_" + version + ".model");
        try (FileOutputStream fos = new FileOutputStream(modelPath.toFile())) {
            model.saveModel(fos);
        }
        logger.info("Model saved with version: {}", version);
    }

    public Booster loadLatestModel() throws Exception {
        Path modelDir = Paths.get(modelStoragePath);
        if (!Files.exists(modelDir)) {
            throw new RuntimeException("Model directory does not exist");
        }

        Optional<Path> latestModel = Files.list(modelDir)
                .filter(p -> p.toString().endsWith(".model"))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()));

        if (latestModel.isPresent()) {
            try (FileInputStream fis = new FileInputStream(latestModel.get().toFile())) {
                Booster model = XGBoost.loadModel(fis);
                logger.info("Loaded model: {}", latestModel.get().getFileName());
                return model;
            }
        } else {
            throw new RuntimeException("No model found");
        }
    }

    public String generateVersion() {
        return String.valueOf(System.currentTimeMillis());
    }
}