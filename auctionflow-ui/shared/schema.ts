import { pgTable, text, varchar, decimal, timestamp, boolean, jsonb } from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

export const users = pgTable("users", {
  id: varchar("id").primaryKey().default(sql`gen_random_uuid()`),
  email: text("email").notNull().unique(),
  password: text("password").notNull(),
  displayName: text("display_name").notNull(),
  role: text("role").notNull().default("BUYER"),
  kycStatus: text("kyc_status").default("PENDING"),
  createdAt: timestamp("created_at").defaultNow(),
});

export const auctions = pgTable("auctions", {
  id: varchar("id").primaryKey().default(sql`gen_random_uuid()`),
  itemId: varchar("item_id").notNull(),
  sellerId: varchar("seller_id").notNull(),
  title: text("title").notNull(),
  description: text("description").notNull(),
  category: text("category").notNull(),
  condition: text("condition"),
  images: jsonb("images").$type<string[]>().default(sql`'[]'`),
  auctionType: text("auction_type").notNull().default("ENGLISH"),
  startingPrice: decimal("starting_price", { precision: 10, scale: 2 }).notNull(),
  reservePrice: decimal("reserve_price", { precision: 10, scale: 2 }),
  buyNowPrice: decimal("buy_now_price", { precision: 10, scale: 2 }),
  currentHighestBid: decimal("current_highest_bid", { precision: 10, scale: 2 }),
  bidCount: text("bid_count").default("0"),
  hiddenReserve: boolean("hidden_reserve").default(false),
  startTime: timestamp("start_time").notNull(),
  endTime: timestamp("end_time").notNull(),
  status: text("status").notNull().default("PENDING"),
  createdAt: timestamp("created_at").defaultNow(),
});

export const bids = pgTable("bids", {
  id: varchar("id").primaryKey().default(sql`gen_random_uuid()`),
  auctionId: varchar("auction_id").notNull(),
  bidderId: varchar("bidder_id").notNull(),
  amount: decimal("amount", { precision: 10, scale: 2 }).notNull(),
  status: text("status").notNull().default("ACCEPTED"),
  idempotencyKey: text("idempotency_key"),
  timestamp: timestamp("timestamp").defaultNow(),
});

export const watchlist = pgTable("watchlist", {
  id: varchar("id").primaryKey().default(sql`gen_random_uuid()`),
  userId: varchar("user_id").notNull(),
  auctionId: varchar("auction_id").notNull(),
  createdAt: timestamp("created_at").defaultNow(),
});

export const insertUserSchema = createInsertSchema(users).pick({
  email: true,
  password: true,
  displayName: true,
  role: true,
});

export const insertAuctionSchema = createInsertSchema(auctions).omit({
  id: true,
  createdAt: true,
  currentHighestBid: true,
  bidCount: true,
  status: true,
});

export const insertBidSchema = createInsertSchema(bids).omit({
  id: true,
  timestamp: true,
  status: true,
});

export type InsertUser = z.infer<typeof insertUserSchema>;
export type User = typeof users.$inferSelect;
export type InsertAuction = z.infer<typeof insertAuctionSchema>;
export type Auction = typeof auctions.$inferSelect;
export type InsertBid = z.infer<typeof insertBidSchema>;
export type Bid = typeof bids.$inferSelect;
export type Watchlist = typeof watchlist.$inferSelect;
