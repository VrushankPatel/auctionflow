export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: {
    id: string;
    email: string;
    displayName: string;
    role: string;
  };
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  role: 'BUYER' | 'SELLER';
}

export interface AuctionListResponse {
  auctions: any[];
  total: number;
  hasMore: boolean;
}

export interface PlaceBidRequest {
  amount: number;
  idempotencyKey: string;
}

export interface PlaceBidResponse {
  bidId: string;
  auctionId: string;
  amount: number;
  status: string;
  timestamp: string;
}

export interface ApiError {
  error: string;
  message: string;
}

export interface WatchlistResponse {
  message: string;
}
