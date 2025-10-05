package com.auctionflow.api.config;

import com.auctionflow.api.entities.Item;
import com.auctionflow.api.entities.User;
import com.auctionflow.api.repositories.ItemRepository;
import com.auctionflow.api.repositories.UserRepository;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Configuration
public class GraphQLConfig {

    // @Bean
    // public DataLoaderRegistry batchLoaderRegistry(ItemRepository itemRepository, UserRepository userRepository) {
        // BatchLoaderRegistry registry = new BatchLoaderRegistry();
        // DataLoaderRegistry registry = new DataLoaderRegistry();

        // DataLoader for Items
        // BatchLoader<String, Item> itemBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
        //     List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
        //     List<Item> items = itemRepository.findAllById(uuids);
        //     return ids.stream().map(id -> items.stream().filter(item -> item.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        // });
        // registry.register("itemDataLoader", DataLoader.newDataLoader(itemBatchLoader));

        // DataLoader for Users
        // BatchLoader<String, User> userBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
        //     List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
        //     List<User> users = userRepository.findAllById(uuids);
        //     return ids.stream().map(id -> users.stream().filter(user -> user.getId().toString().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        // });
        // registry.register("userDataLoader", DataLoader.newDataLoader(userBatchLoader));

        // return registry;
    // }

    @Bean
    public DataLoaderRegistry dataLoaderRegistry(ItemRepository itemRepository, UserRepository userRepository) {
        DataLoaderRegistry registry = new DataLoaderRegistry();

        // DataLoader for Items
        BatchLoader<String, Item> itemBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<String> stringIds = ids;
            List<Item> items = itemRepository.findAllById(stringIds);
            return ids.stream().map(id -> items.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("itemDataLoader", DataLoader.newDataLoader(itemBatchLoader));

        // DataLoader for Users
        BatchLoader<String, User> userBatchLoader = ids -> CompletableFuture.supplyAsync(() -> {
            List<Long> longIds = ids.stream().map(Long::valueOf).collect(Collectors.toList());
            List<User> users = userRepository.findAllById(longIds);
            return ids.stream().map(id -> users.stream().filter(user -> user.getId().equals(Long.valueOf(id))).findFirst().orElse(null)).collect(Collectors.toList());
        });
        registry.register("userDataLoader", DataLoader.newDataLoader(userBatchLoader));

        return registry;
    }

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType moneyScalar = GraphQLScalarType.newScalar()
                .name("Money")
                .description("A monetary value")
                .coercing(new Coercing<BigDecimal, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof BigDecimal) {
                            return ((BigDecimal) dataFetcherResult).toString();
                        }
                        return null;
                    }

                    @Override
                    public BigDecimal parseValue(Object input) {
                        if (input instanceof String) {
                            return new BigDecimal((String) input);
                        }
                        return null;
                    }

                    @Override
                    public BigDecimal parseLiteral(Object input) {
                        if (input instanceof graphql.language.StringValue) {
                            return new BigDecimal(((graphql.language.StringValue) input).getValue());
                        }
                        return null;
                    }
                })
                .build();

        GraphQLScalarType longScalar = GraphQLScalarType.newScalar()
                .name("Long")
                .description("A 64-bit signed integer")
                .coercing(new Coercing<Long, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof Long) {
                            return ((Long) dataFetcherResult).toString();
                        }
                        return null;
                    }

                    @Override
                    public Long parseValue(Object input) {
                        if (input instanceof String) {
                            return Long.valueOf((String) input);
                        }
                        return null;
                    }

                    @Override
                    public Long parseLiteral(Object input) {
                        if (input instanceof graphql.language.IntValue) {
                            return ((graphql.language.IntValue) input).getValue().longValue();
                        }
                        return null;
                    }
                })
                .build();

        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(longScalar)
                .scalar(moneyScalar);
    }
}