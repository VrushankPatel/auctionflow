package com.auctionflow.api.config;

import com.auctionflow.api.entities.Item;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.repositories.UserRepository;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Configuration
public class GraphQLConfig {

    @Bean
    public BatchLoaderRegistry batchLoaderRegistry(ItemRepository itemRepository, UserRepository userRepository) {
        BatchLoaderRegistry registry = new BatchLoaderRegistry();
        DataLoaderRegistry registry = new DataLoaderRegistry();

        // DataLoader for Items
        BatchLoader<String, Item> itemBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
            List<Item> items = itemRepository.findAllById(uuids);
            return ids.stream().map(id -> items.stream().filter(item -> item.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("itemDataLoader", DataLoader.newDataLoader(itemBatchLoader));

        // DataLoader for Users
        BatchLoader<String, User> userBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
            List<User> users = userRepository.findAllById(uuids);
            return ids.stream().map(id -> users.stream().filter(user -> user.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("userDataLoader", DataLoader.newDataLoader(userBatchLoader));

        return registry;
    }

    @Bean
    public DataLoaderRegistry dataLoaderRegistry(ItemRepository itemRepository, UserRepository userRepository) {
        DataLoaderRegistry registry = new DataLoaderRegistry();

        // DataLoader for Items
        BatchLoader<String, Item> itemBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
            List<Item> items = itemRepository.findAllById(uuids);
            return ids.stream().map(id -> items.stream().filter(item -> item.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("itemDataLoader", DataLoader.newDataLoader(itemBatchLoader));

        // DataLoader for Users
        BatchLoader<String, User> userBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
            List<User> users = userRepository.findAllById(uuids);
            return ids.stream().map(id -> users.stream().filter(user -> user.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("userDataLoader", DataLoader.newDataLoader(userBatchLoader));

        return registry;
    }
}