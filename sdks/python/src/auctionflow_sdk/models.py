from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime
from decimal import Decimal

class AuthRequest(BaseModel):
    username: str
    password: str

class AuthResponse(BaseModel):
    accessToken: str
    refreshToken: str

class RegisterRequest(BaseModel):
    email: str
    displayName: str
    password: str
    role: Optional[str] = None

class RegisterResponse(BaseModel):
    message: str
    userId: int

class CreateAuctionRequest(BaseModel):
    itemId: str
    categoryId: str
    auctionType: str
    reservePrice: Decimal
    buyNowPrice: Decimal
    startTime: datetime
    endTime: datetime
    hiddenReserve: Optional[bool] = False

class AuctionSummary(BaseModel):
    id: str
    title: str
    category: str
    currentBid: Decimal
    endTime: datetime
    status: str

class AuctionDetails(BaseModel):
    id: str
    title: str
    description: str
    category: str
    reservePrice: Decimal
    buyNowPrice: Decimal
    currentHighestBid: Decimal
    startTime: datetime
    endTime: datetime
    status: str
    sellerId: str

class PlaceBidRequest(BaseModel):
    amount: Decimal
    idempotencyKey: Optional[str] = None

class Bid(BaseModel):
    id: str
    bidderId: str
    amount: Decimal
    timestamp: datetime
    accepted: bool

class BidHistory(BaseModel):
    bids: List[Bid]
    page: int
    size: int
    total: int