package models

import (
	"time"
)

type AuthRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type AuthResponse struct {
	AccessToken  string `json:"accessToken"`
	RefreshToken string `json:"refreshToken"`
}

type RegisterRequest struct {
	Email       string `json:"email"`
	DisplayName string `json:"displayName"`
	Password    string `json:"password"`
	Role        string `json:"role,omitempty"`
}

type RegisterResponse struct {
	Message string `json:"message"`
	UserID  int64  `json:"userId"`
}

type CreateAuctionRequest struct {
	ItemID        string    `json:"itemId"`
	CategoryID    string    `json:"categoryId"`
	AuctionType   string    `json:"auctionType"`
	ReservePrice  float64   `json:"reservePrice"`
	BuyNowPrice   float64   `json:"buyNowPrice"`
	StartTime     time.Time `json:"startTime"`
	EndTime       time.Time `json:"endTime"`
	HiddenReserve bool      `json:"hiddenReserve,omitempty"`
}

type AuctionSummary struct {
	ID         string    `json:"id"`
	Title      string    `json:"title"`
	Category   string    `json:"category"`
	CurrentBid float64   `json:"currentBid"`
	EndTime    time.Time `json:"endTime"`
	Status     string    `json:"status"`
}

type AuctionDetails struct {
	ID                string    `json:"id"`
	Title             string    `json:"title"`
	Description       string    `json:"description"`
	Category          string    `json:"category"`
	ReservePrice      float64   `json:"reservePrice"`
	BuyNowPrice       float64   `json:"buyNowPrice"`
	CurrentHighestBid float64   `json:"currentHighestBid"`
	StartTime         time.Time `json:"startTime"`
	EndTime           time.Time `json:"endTime"`
	Status            string    `json:"status"`
	SellerID          string    `json:"sellerId"`
}

type PlaceBidRequest struct {
	Amount         float64 `json:"amount"`
	IdempotencyKey string  `json:"idempotencyKey,omitempty"`
}

type Bid struct {
	ID        string    `json:"id"`
	BidderID  string    `json:"bidderId"`
	Amount    float64   `json:"amount"`
	Timestamp time.Time `json:"timestamp"`
	Accepted  bool      `json:"accepted"`
}

type BidHistory struct {
	Bids  []Bid `json:"bids"`
	Page  int   `json:"page"`
	Size  int   `json:"size"`
	Total int64 `json:"total"`
}
