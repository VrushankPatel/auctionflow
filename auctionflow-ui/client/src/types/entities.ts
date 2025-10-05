export interface User {
  id: string;
  email: string;
  displayName: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN';
  kycStatus?: string;
}

export interface Auction {
  id: string;
  itemId: string;
  sellerId: string;
  title: string;
  description: string;
  category: string;
  condition?: string;
  images: string[];
  auctionType: 'ENGLISH' | 'DUTCH' | 'SEALED';
  startingPrice: number;
  reservePrice?: number;
  buyNowPrice?: number;
  currentHighestBid?: number;
  bidCount: number;
  hiddenReserve: boolean;
  startTime: string;
  endTime: string;
  status: 'PENDING' | 'ACTIVE' | 'CLOSED' | 'CANCELLED';
  seller?: {
    id: string;
    displayName: string;
  };
  isWatched?: boolean;
}

export interface Bid {
  id: string;
  auctionId: string;
  bidderId: string;
  amount: number;
  status: 'ACCEPTED' | 'REJECTED' | 'OUTBID';
  timestamp: string;
  bidderName?: string;
}

export interface BidHistoryItem extends Bid {
  auctionTitle?: string;
  auctionStatus?: string;
}

export interface Category {
  id: string;
  name: string;
  parentId?: string;
}

export interface BidIncrement {
  minAmount: number;
  maxAmount: number;
  increment: number;
}
