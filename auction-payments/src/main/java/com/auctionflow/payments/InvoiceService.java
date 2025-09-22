package com.auctionflow.payments;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final FeeScheduleRepository feeScheduleRepository;

    // Simple tax rate, configurable later
    private static final BigDecimal TAX_RATE = BigDecimal.ZERO;

    public InvoiceService(InvoiceRepository invoiceRepository, FeeScheduleRepository feeScheduleRepository) {
        this.invoiceRepository = invoiceRepository;
        this.feeScheduleRepository = feeScheduleRepository;
    }

    @Transactional
    public Invoice generateInvoice(String auctionId, String sellerId, String buyerId, BigDecimal winningBidAmount) {
        // Check if invoice already exists
        if (invoiceRepository.findByAuctionId(auctionId).isPresent()) {
            throw new IllegalStateException("Invoice already exists for auction: " + auctionId);
        }

        BigDecimal platformFee = calculatePlatformFee(winningBidAmount);
        BigDecimal taxAmount = calculateTax(winningBidAmount);
        BigDecimal netPayout = winningBidAmount.subtract(platformFee).subtract(taxAmount);

        Invoice invoice = new Invoice(auctionId, sellerId, buyerId, winningBidAmount, platformFee, taxAmount, netPayout);
        invoice = invoiceRepository.save(invoice);

        // Create invoice items
        List<InvoiceItem> items = new ArrayList<>();
        items.add(new InvoiceItem(invoice, InvoiceItem.ItemType.SALE, "Auction sale amount", winningBidAmount));
        items.add(new InvoiceItem(invoice, InvoiceItem.ItemType.PLATFORM_FEE, "Platform fee", platformFee.negate()));
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new InvoiceItem(invoice, InvoiceItem.ItemType.TAX, "Tax", taxAmount.negate()));
        }
        items.add(new InvoiceItem(invoice, InvoiceItem.ItemType.NET_PAYOUT, "Net payout to seller", netPayout));

        invoice.setItems(items);
        return invoiceRepository.save(invoice);
    }

    private BigDecimal calculatePlatformFee(BigDecimal amount) {
        FeeSchedule feeSchedule = feeScheduleRepository.findByFeeTypeAndActiveTrue(FeeSchedule.FeeType.PLATFORM_FEE)
                .orElseThrow(() -> new IllegalStateException("No active platform fee schedule found"));

        if (feeSchedule.getCalculationType() == FeeSchedule.CalculationType.PERCENTAGE) {
            return amount.multiply(feeSchedule.getValue()).setScale(2, RoundingMode.HALF_UP);
        } else {
            return feeSchedule.getValue();
        }
    }

    private BigDecimal calculateTax(BigDecimal amount) {
        return amount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}