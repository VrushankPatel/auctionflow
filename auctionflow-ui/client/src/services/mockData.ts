import type { Auction, Bid, Category, BidIncrement } from '@/types/entities';
import type { AuctionListResponse, PlaceBidResponse, LoginResponse } from '@/types/api';

export const mockAuctions: Auction[] = [
  {
    id: 'auction-1',
    itemId: 'item-1',
    sellerId: 'seller-1',
    title: 'Vintage Leica M3 Camera - 1960s Collectors Edition',
    description: 'Rare vintage Leica M3 rangefinder camera from the 1960s. In excellent condition with original leather case. Perfect working order, recently serviced. A must-have for collectors.',
    category: 'electronics',
    condition: 'Excellent',
    images: [
      'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&h=600&fit=crop',
      'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600&h=600&fit=crop',
      'https://images.unsplash.com/photo-1606925797300-0b35e9d1794e?w=600&h=600&fit=crop',
      'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&h=600&fit=crop&sat=-100',
    ],
    auctionType: 'ENGLISH',
    startingPrice: 500,
    reservePrice: 800,
    buyNowPrice: 1200,
    currentHighestBid: 650,
    bidCount: 8,
    hiddenReserve: false,
    startTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    status: 'ACTIVE',
    seller: {
      id: 'seller-1',
      displayName: 'PhotoExpert',
    },
    isWatched: false,
  },
  {
    id: 'auction-2',
    itemId: 'item-2',
    sellerId: 'seller-2',
    title: 'Original Abstract Art - Modern Canvas Painting 48x36',
    description: 'Contemporary abstract painting on premium canvas. Vibrant colors with textured layers. Artist signed and authenticated. Ready to hang.',
    category: 'art',
    images: ['https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=600&h=600&fit=crop'],
    auctionType: 'ENGLISH',
    startingPrice: 200,
    reservePrice: 400,
    currentHighestBid: 350,
    bidCount: 12,
    hiddenReserve: false,
    startTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
    status: 'ACTIVE',
    seller: {
      id: 'seller-2',
      displayName: 'ArtCollector99',
    },
    isWatched: true,
  },
  {
    id: 'auction-3',
    itemId: 'item-3',
    sellerId: 'seller-3',
    title: 'Rolex Submariner Watch - Vintage 1970s Automatic',
    description: 'Authentic vintage Rolex Submariner from the 1970s. Automatic movement in excellent working condition. Comes with original box and documentation.',
    category: 'jewelry',
    images: ['https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=600&h=600&fit=crop'],
    auctionType: 'ENGLISH',
    startingPrice: 5000,
    reservePrice: 7500,
    currentHighestBid: 6200,
    bidCount: 24,
    hiddenReserve: false,
    startTime: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 5 * 60 * 60 * 1000).toISOString(),
    status: 'ACTIVE',
    seller: {
      id: 'seller-3',
      displayName: 'LuxuryTimepieces',
    },
    isWatched: false,
  },
  {
    id: 'auction-4',
    itemId: 'item-4',
    sellerId: 'seller-1',
    title: 'Vintage Fender Stratocaster Electric Guitar 1978',
    description: 'Classic 1978 Fender Stratocaster in sunburst finish. Original pickups, professionally maintained. Plays beautifully with authentic vintage tone.',
    category: 'electronics',
    images: ['https://images.unsplash.com/photo-1556449895-a33c9dba33dd?w=600&h=600&fit=crop'],
    auctionType: 'ENGLISH',
    startingPrice: 1500,
    currentHighestBid: 1850,
    bidCount: 15,
    hiddenReserve: false,
    startTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
    endTime: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
    status: 'ACTIVE',
    seller: {
      id: 'seller-1',
      displayName: 'PhotoExpert',
    },
    isWatched: false,
  },
];

export const mockBids: Record<string, Bid[]> = {
  'auction-1': [
    {
      id: 'bid-1',
      auctionId: 'auction-1',
      bidderId: 'user-1',
      amount: 650,
      status: 'ACCEPTED',
      timestamp: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
      bidderName: 'JohnCollector',
    },
    {
      id: 'bid-2',
      auctionId: 'auction-1',
      bidderId: 'user-2',
      amount: 620,
      status: 'OUTBID',
      timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
      bidderName: 'CameraFan',
    },
    {
      id: 'bid-3',
      auctionId: 'auction-1',
      bidderId: 'user-3',
      amount: 580,
      status: 'OUTBID',
      timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
      bidderName: 'VintageHunter',
    },
  ],
  'auction-2': [
    {
      id: 'bid-4',
      auctionId: 'auction-2',
      bidderId: 'user-4',
      amount: 350,
      status: 'ACCEPTED',
      timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      bidderName: 'ArtLover',
    },
  ],
};

export const mockCategories: Category[] = [
  { id: 'electronics', name: 'Electronics' },
  { id: 'cameras', name: 'Cameras', parentId: 'electronics' },
  { id: 'art', name: 'Art & Collectibles' },
  { id: 'jewelry', name: 'Jewelry & Watches' },
  { id: 'vehicles', name: 'Vehicles' },
  { id: 'furniture', name: 'Furniture & Home' },
];

export const mockBidIncrements: BidIncrement[] = [
  { minAmount: 0, maxAmount: 100, increment: 5 },
  { minAmount: 100, maxAmount: 1000, increment: 10 },
  { minAmount: 1000, maxAmount: 5000, increment: 50 },
  { minAmount: 5000, maxAmount: 10000, increment: 100 },
];

export const mockUser = {
  id: 'user-test-123',
  email: 'test@example.com',
  displayName: 'Test User',
  role: 'BUYER' as const,
};

export function generateMockResponse<T>(data: T, delay = 300): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delay);
  });
}

export function getMockAuctions(params?: {
  category?: string;
  status?: string;
  limit?: number;
  offset?: number;
}): Promise<AuctionListResponse> {
  let filtered = [...mockAuctions];

  if (params?.category && params.category !== 'all') {
    filtered = filtered.filter((a) => a.category === params.category);
  }

  if (params?.status && params.status !== 'all') {
    filtered = filtered.filter((a) => a.status === params.status);
  }

  const limit = params?.limit || 20;
  const offset = params?.offset || 0;
  const paginated = filtered.slice(offset, offset + limit);

  return generateMockResponse({
    auctions: paginated,
    total: filtered.length,
    hasMore: offset + limit < filtered.length,
  });
}

export function getMockAuction(id: string): Promise<Auction> {
  const auction = mockAuctions.find((a) => a.id === id);
  if (!auction) {
    return Promise.reject({ error: 'AUCTION_NOT_FOUND', message: 'Auction not found' });
  }
  return generateMockResponse(auction);
}

export function getMockBidHistory(auctionId: string): Promise<{ bids: Bid[]; total: number }> {
  const bids = mockBids[auctionId] || [];
  return generateMockResponse({ bids, total: bids.length });
}

export function placeMockBid(
  auctionId: string,
  amount: number,
  idempotencyKey: string
): Promise<PlaceBidResponse> {
  const auction = mockAuctions.find((a) => a.id === auctionId);
  if (!auction) {
    return Promise.reject({ error: 'AUCTION_NOT_FOUND', message: 'Auction not found' });
  }

  const currentHighest = auction.currentHighestBid || auction.startingPrice;
  const minBid = currentHighest + 10;

  if (amount < minBid) {
    return Promise.reject({
      error: 'BID_TOO_LOW',
      message: `Bid amount must be at least $${minBid.toFixed(2)}`,
    });
  }

  auction.currentHighestBid = amount;
  auction.bidCount++;

  const newBid: Bid = {
    id: `bid-${Date.now()}`,
    auctionId,
    bidderId: mockUser.id,
    amount,
    status: 'ACCEPTED',
    timestamp: new Date().toISOString(),
  };

  if (!mockBids[auctionId]) {
    mockBids[auctionId] = [];
  }
  mockBids[auctionId].unshift(newBid);

  return generateMockResponse({
    bidId: newBid.id,
    auctionId,
    amount,
    status: 'ACCEPTED',
    timestamp: newBid.timestamp,
  });
}

export function mockLogin(email: string, password: string): Promise<LoginResponse> {
  if (password.length < 6) {
    return Promise.reject({ error: 'INVALID_CREDENTIALS', message: 'Invalid email or password' });
  }

  return generateMockResponse({
    token: 'mock-jwt-token-' + Date.now(),
    user: {
      id: mockUser.id,
      email,
      displayName: mockUser.displayName,
      role: mockUser.role,
    },
  });
}

export function mockRegister(data: {
  email: string;
  password: string;
  displayName: string;
  role: string;
}): Promise<{ id: string; email: string; displayName: string; role: string; kycStatus: string }> {
  return generateMockResponse({
    id: 'user-' + Date.now(),
    email: data.email,
    displayName: data.displayName,
    role: data.role,
    kycStatus: 'PENDING',
  });
}
