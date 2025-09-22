export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

export interface RegisterRequest {
  email: string;
  displayName: string;
  password: string;
  role?: string;
}

export interface RegisterResponse {
  message: string;
  userId: number;
}

export interface CreateAuctionRequest {
  itemId: string;
  categoryId: string;
  auctionType: string;
  reservePrice: number;
  buyNowPrice: number;
  startTime: string;
  endTime: string;
  hiddenReserve?: boolean;
}

export interface AuctionSummary {
  id: string;
  title: string;
  category: string;
  currentBid: number;
  endTime: string;
  status: string;
}

export interface AuctionDetails {
  id: string;
  title: string;
  description: string;
  category: string;
  reservePrice: number;
  buyNowPrice: number;
  currentHighestBid: number;
  startTime: string;
  endTime: string;
  status: string;
  sellerId: string;
}

export interface PlaceBidRequest {
  amount: number;
  idempotencyKey?: string;
}

export interface Bid {
  id: string;
  bidderId: string;
  amount: number;
  timestamp: string;
  accepted: boolean;
}

export interface BidHistory {
  bids: Bid[];
  page: number;
  size: number;
  total: number;
}