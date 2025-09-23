package com.auctionflow.common.service;

import com.auctionflow.core.domain.events.AuctionExtendedEvent;
import com.auctionflow.core.domain.valueobjects.AntiSnipePolicy;
import com.auctionflow.core.domain.valueobjects.AuctionId;

import java.time.Duration;
import java.time.Instant;

public interface AntiSnipeExtension {
    AuctionExtendedEvent calculateExtensionIfNeeded(AuctionId auctionId, Instant bidTime, Instant currentEndTime,
                                           Duration originalDuration, AntiSnipePolicy policy, long currentExtensions);
}